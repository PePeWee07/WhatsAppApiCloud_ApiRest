package com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.impl;

import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.WhatsappMediaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.BackEnd.WhatsappApiCloud.exception.ServerClientException;
import com.BackEnd.WhatsappApiCloud.model.dto.whatsapp.requestSendMessage.media.ResponseMediaMetadata;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.Tika;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WhatsappMediaServiceImpl implements WhatsappMediaService {

    private static final Logger logger = LoggerFactory.getLogger(ApiWhatsappServiceImpl.class);

    private final RestClient restMetaClient;     // para /{mediaId}
    private final RestClient restDownloadClient; // para GET de la URL efímera
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WhatsappMediaServiceImpl(
            @Value("${Phone-Number-ID}") String identifier,
            @Value("${whatsapp.token}") String token,
            @Value("${whatsapp.urlbase}") String urlBase,
            @Value("${whatsapp.version}") String version,
            ObjectMapper objectMapper) {

        // ej: https://graph.facebook.com/v23.0
        this.restMetaClient = RestClient.builder()
                .baseUrl(urlBase + version)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
        restClient = RestClient.builder()
                    .baseUrl(urlBase + version + "/" + identifier)
                    .defaultHeader("Authorization", "Bearer " + token)
                    .build();

        // sin baseUrl: usaremos URI absoluta (la URL efímera que viene en metadata.url)
        this.restDownloadClient = RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
        this.objectMapper = objectMapper;
    }

    // ---------- utilidades ----------
    private static final Map<String, String> ALLOWED_MIME_TO_EXT = Map.ofEntries(
        // Documentos (100 MB)
        Map.entry("text/plain", ".txt"),
        Map.entry("application/vnd.ms-excel", ".xls"),
        Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
        Map.entry("application/msword", ".doc"),
        Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
        Map.entry("application/vnd.ms-powerpoint", ".ppt"),
        Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
        Map.entry("application/pdf", ".pdf"),
        // Imágenes (5 MB)
        Map.entry("image/jpeg", ".jpg"),
        Map.entry("image/jpg",  ".jpg"),
        Map.entry("image/png",  ".png")
    );
    private static final long MAX_IMAGE = 5L  * 1024 * 1024;
    private static final long MAX_DOC   = 100L * 1024 * 1024;

    private static String baseMime(String mime) {
        return mime == null ? "" : mime.toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
    }
    private static boolean isAllowedImage(String m) {
        return m.equals("image/jpeg") || m.equals("image/jpg") || m.equals("image/png");
    }
    private static boolean isAllowedDoc(String m) {
        return m.equals("text/plain")
            || m.equals("application/vnd.ms-excel")
            || m.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            || m.equals("application/msword")
            || m.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            || m.equals("application/vnd.ms-powerpoint")
            || m.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")
            || m.equals("application/pdf");
    }
    private static void assertAllowedMimeAndSize(String mime, long sizeBytes) {
        String m = baseMime(mime);
        if (!ALLOWED_MIME_TO_EXT.containsKey(m)) {
            throw new IllegalStateException("Formato no soportado: " + m
                + ". Permitidos: JPG, PNG, PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, TXT.");
        }
        if (isAllowedImage(m) && sizeBytes > MAX_IMAGE) {
            throw new IllegalStateException("Imagen supera el límite de 5 MB.");
        }
        if (isAllowedDoc(m) && sizeBytes > MAX_DOC) {
            throw new IllegalStateException("Documento supera el límite de 100 MB.");
        }
    }
    private static String ensureExt(String name, String ext) {
        if (ext == null || ext.isBlank()) return name;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(ext.toLowerCase(Locale.ROOT)) ? name : name + ext;
    }
    private static String sanitize(String s) {
        return (s == null || s.isBlank()) ? "wa_file" : s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // ---------- llamadas ----------
    private ResponseMediaMetadata getMediaMetadata(String mediaId) {
        try {
            return restMetaClient.get()
                    .uri("/{mediaId}", mediaId) // devuelve { url, mime_type, file_size, ... }
                    .retrieve()
                    .body(ResponseMediaMetadata.class);
        } catch (RestClientResponseException e) {
            throw new ServerClientException("Error metadata media_id " + mediaId + ": "
                    + e.getStatusCode().value() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new ServerClientException("Error metadata media_id " + mediaId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public File downloadMediaToTemp(String mediaId, @Nullable String filenameHint) throws IOException {
        ResponseMediaMetadata meta = getMediaMetadata(mediaId);
        String mime = baseMime(meta.mimeType());
        long size   = meta.fileSize() != null ? meta.fileSize() : 0L;

        // valida antes de descargar
        assertAllowedMimeAndSize(mime, size);

        byte[] bytes = restDownloadClient.get()
                .uri(meta.url())                 // URL efímera (~5 min)
                .retrieve()
                .body(byte[].class);

        String base = sanitize((filenameHint != null && !filenameHint.isBlank()) ? filenameHint : "wa_" + mediaId);
        String ext  = ALLOWED_MIME_TO_EXT.get(mime);
        String finalName = ensureExt(base, ext);

        File tmp = File.createTempFile("wa_", "_" + finalName);
        Files.write(tmp.toPath(), bytes);
        return tmp;
    }

    
    // ============== Método para convertir CSV a Excel ==============
    private File convertCsvToExcel(File csvFile) throws IOException {
        File excelFile = new File(csvFile.getParent(), "converted_" + csvFile.getName() + ".xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            List<String> lines = Files.readAllLines(csvFile.toPath(), StandardCharsets.UTF_8);

            int rowIndex = 0;
            for (String line : lines) {
                Row row = sheet.createRow(rowIndex++);
                String[] cells = line.split(",");
                for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
                    Cell cell = row.createCell(cellIndex);
                    cell.setCellValue(cells[cellIndex]);
                }
            }

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }

        return excelFile;
    }


    // ============== Cargar archivo multimedia a servidores WhatsApp ===============
    @Override
    public String uploadMedia(File mediaFile) {
        try {
            if (mediaFile.length() == 0 || !mediaFile.exists()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El archivo está vacío."
                );
            }

            Tika tika = new Tika();
            String contentType = tika.detect(mediaFile);

            if ("text/csv".equals(contentType)) {
                mediaFile = convertCsvToExcel(mediaFile);
                contentType = "application/vnd.ms-excel";
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(mediaFile));
            body.add("type", contentType);
            body.add("messaging_product", "whatsapp");

            String response = restClient.post()
                    .uri("/media")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("id").asText();

        } catch (IOException e) {
            logger.error("Error al leer el archivo: ", e);
            throw new RuntimeException("Error al leer el archivo: ", e);
        } catch (Exception e) {
            logger.error("Error inesperado al subir el archivo: ", e);
            throw new RuntimeException("Error inesperado al subir el archivo al servidor de Whatsapp: ", e);
        }
    }


}
