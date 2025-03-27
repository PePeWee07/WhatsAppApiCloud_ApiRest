# WhatsApp API Cloud Chatbot

## Descripción

Este proyecto implementa la API de WhatsApp Cloud y OpenAI para crear un chat inteligente que responde preguntas sobre soporte TIC.

## Características

- **Integración con WhatsApp Cloud API**: Permite la comunicación a través de WhatsApp.
- **Uso de OpenAI**: Proporciona respuestas inteligentes y contextuales.
- **Soporte TIC**: Responde preguntas relacionadas con soporte técnico en tecnologías de la información y comunicación.
# WhatsApp API Cloud Chatbot

## Características

- **Integración con WhatsApp Cloud API**: Permite la comunicación a través de WhatsApp.
- **Uso de OpenAI**: Proporciona respuestas inteligentes y contextuales.
- **Soporte TIC**: Responde preguntas relacionadas con soporte técnico en tecnologías de la información y comunicación.
**Uso de Tools/Functions**: Permite la ejecución de herramientas o funciones específicas para extender las capacidades del chatbot, como realizar cálculos, consultar bases de datos o ejecutar procesos personalizados.

## Repositorios Necesarios
#### **Webhook**
Repositorio: [Webhook](https://github.com/PePeWee07/TicAI-Support.git)
#### **AI Server**
Repositorio: [TicAI-Support](https://github.com/PePeWee07/TicAI-Support.git)
#### **ERP Simulator**
Repositorio: [ERP-Simulator](https://github.com/PePeWee07/ERP_simulator.git)


## Instalación y Ejecución

1. **Clonar el repositorio:**
   ```powershell
   git clone https://github.com/PePeWee07/WhatsAppApiCloud_ApiRest.git
   ```

2. **Configurar las variables de entorno:**
   - Crear un archivo `.env` con las configuraciones necesarias.


      ### Las Variables de entorno:
      ```properties
      # WhatsApp API
      WHATSAPP_URLBASE=https://graph.facebook.com/
      WHATSAPP_VERSION=v20.0
      PHONE_NUMBER_ID=0000000000000
      WHATSAPP_TOKEN=[meta-whatsap-token]

      # ERP SIMULTAOR
      ERP_BASE_URL=http://host.docker.internal:3000
      ERP_URI="/data?cedula="

      # AI Server
      AI_BASE_URL=http://ia-soporte:5000
      AI_URI=/ask
      OPENAI_API_KEY=[api-key-openai]

      # Configuracion
      RESTRICTED_ROLES="Invitado,Visitante"
      LIMIT_QUESTIONS_PER_DAY=50
      HOURS_TO_WAIT_AFTER_LIMIT=24
      STRIKE_LIMIT=3

      # API Key
      API_KEY=[api-key-backend]
      API_KEY_HEADER=[header-api-key-backend]

      # Database Credentials
      DB_URL=jdbc:postgresql://<you-container-name>:<port>/<name_bd>
      DB_DATABASE=[]
      DB_USERNAME=[]
      DB_PASSWORD=[]
      DB_DDL_AUTO=[]

      # Server PORT
      SERVER_PORT=8082

      WELCOME_MESSAGE_FILE=/app/doc/welcome_message.txt

      ```

3. **Construir y ejecutar con Docker Compose:**
   ```powershell
   docker-compose up -d
   ```

## Concideraciones:
   - Usar Mapper para datos del usuario
   - Actualmente se concidera que existe multiples registro de usuarios con diferentes roles, se debera cambiar si un usuario trae una lista de roles
   - El archvio de `welcome_message.txt` deberia estar fuera del contenedor ya que la idea es editarlo y actualice al instante, tendremos editar el volumen en docker-compose.yml - [NewpathFile]:/app/doc/welcome_message.txt