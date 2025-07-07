package com.BackEnd.WhatsappApiCloud.service.glpi;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.jsoup.nodes.TextNode;

public class HtmlCleaner {

    public static String cleanHtmlForWhatsApp(String rawHtml) {
        // 1) Decode entidades y parsear
        Document doc = Jsoup.parse(Parser.unescapeEntities(rawHtml, false));
        doc.outputSettings().prettyPrint(false);

        // 2) Preprocesar enlaces: 
        //    - si texto == href, queda solo href
        //    - si no, texto (href)
        doc.select("a").forEach(a -> {
            String text = a.text().trim();
            String href = a.attr("href").trim();
            String replacement = text.equals(href) 
                ? href 
                : text + " (" + href + ")";
            a.replaceWith(new TextNode(replacement));
        });

        // 3) Construir el resultado
        StringBuilder sb = new StringBuilder();
        for (Node node : doc.body().childNodes()) {
            processNode(sb, node, 0);
        }

        return sb.toString().trim();
    }

    private static void processNode(StringBuilder sb, Node node, int depth) {
        if (node instanceof TextNode) {
            String txt = ((TextNode) node).text().trim();
            if (!txt.isEmpty()) sb.append(txt);
        } 
        else if (node instanceof Element) {
            Element el = (Element) node;
            switch (el.tagName()) {
                case "p":
                    sb.append(el.text()).append("\n\n");
                    break;

                case "ol":
                    int idx = 1;
                    for (Element li : el.select("> li")) {
                        // 1) Título del paso
                        Element p = li.selectFirst("> p");
                        String title = (p != null) 
                            ? p.text().trim()
                            : li.ownText().trim();
                        sb.append(idx++).append(". ").append(title).append("\n");

                        // 2) Procesar cualquier lista anidada (<ul> o <ol>) con un nivel más
                        for (Element nested : li.select("> ul, > ol")) {
                            processSubList(sb, nested, 1);
                        }

                        sb.append("\n");
                    }
                    break;

                case "ul":
                    for (Element li : el.select("> li")) {
                        sb.append("* ").append(li.text()).append("\n");
                    }
                    sb.append("\n");
                    break;

                default:
                    // Recurre a los hijos para cualquier otro tag
                    for (Node c : el.childNodes()) {
                        processNode(sb, c, depth + 1);
                    }
            }
        }
    }

    /**
     * Procesa listas anidadas con un nivel de indentación extra.
     * @param sb       StringBuilder donde se acumula
     * @param listEl   <ul> o <ol> anidado
     * @param level    nivel de indentación (1 = tres espacios + viñeta)
     */
    private static void processSubList(StringBuilder sb, Element listEl, int level) {
        String indent = "   ".repeat(level);
        if ("ul".equals(listEl.tagName())) {
            for (Element li : listEl.select("> li")) {
                sb.append(indent).append("* ").append(li.text().trim()).append("\n");
                // admite lista más profunda aún:
                for (Element deeper : li.select("> ul, > ol")) {
                    processSubList(sb, deeper, level + 1);
                }
            }
        } else { // "ol"
            int idx = 1;
            for (Element li : listEl.select("> li")) {
                sb.append(indent).append(idx++).append(". ").append(li.text().trim()).append("\n");
                for (Element deeper : li.select("> ul, > ol")) {
                    processSubList(sb, deeper, level + 1);
                }
            }
        }
    }
}
