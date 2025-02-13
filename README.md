# WhatsApp API Cloud Chatbot

## Descripción

Este proyecto implementa la API de WhatsApp Cloud y OpenAI para crear un chat inteligente que responde preguntas sobre soporte TIC.

## Características

- **Integración con WhatsApp Cloud API**: Permite la comunicación a través de WhatsApp.
- **Uso de OpenAI**: Proporciona respuestas inteligentes y contextuales.
- **Soporte TIC**: Responde preguntas relacionadas con soporte técnico en tecnologías de la información y comunicación.
# WhatsApp API Cloud Chatbot

## Descripción

Este proyecto implementa la API de WhatsApp Cloud y OpenAI para crear un chatbot inteligente que responde preguntas sobre soporte TIC.

## Características

- **Integración con WhatsApp Cloud API**: Permite la comunicación a través de WhatsApp.
- **Uso de OpenAI**: Proporciona respuestas inteligentes y contextuales.
- **Soporte TIC**: Responde preguntas relacionadas con soporte técnico en tecnologías de la información y comunicación en `application.properties`.

## Requisitos

Para ejecutar este proyecto, asegúrate de contar con los siguientes entornos y configuraciones:

### Variables de entorno necesarias en `application.properties`

#### **Meta (WhatsApp Cloud API)**
```properties
whatsapp.urlbase=https://graph.facebook.com/
whatsapp.version=v20.0
Phone-Number-ID=0000000000
whatsapp.token=your-token-here
```

#### **ERP Simulator**
Repositorio: [ERP-Simulator](#)
```properties
baseurl.jsonserver=http://host.docker.internal:3000
uri.jsonserver=/data?cedula=
```

#### **AI Server**
Repositorio: [TicAI-Support](https://github.com/PePeWee07/TicAI-Support.git)
```properties
baseurl.aiserver=http://host.docker.internal:5000
uri.aiserver=/ask
service.api.key.openai=my-static-api-key
```

#### **Configuración del Chatbot**
```properties
restricted.roles=Estudiante,Invitado,Visitante
limit.questions.per.day=50
hours.to.wait.after.limit=24
strike.limit=3
```

#### **API Key**
```properties
api.key=my-secure-api-key
api.key.header=X-API-KEY
```

#### **Base de Datos (PostgreSQL)**
```properties
spring.datasource.url=jdbc:postgresql://db:5432/DATABASENAME
spring.datasource.username=USERNAME
spring.datasource.password=PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
```

#### **Zona Horaria**
```properties
spring.jackson.time-zone=America/Guayaquil
```

#### **Configuración del Servidor**
```properties
server.port=8082
```
### Variables de entorno necesarias en `.env`
```properties
DB_DATABASE=DATABASENAME
DB_USERNAME=USERNAME
DB_PASSWORD=PASSWORD
```

## Instalación y Ejecución

1. **Clonar el repositorio:**
   ```sh
   git clone https://github.com/tu-usuario/tu-repositorio.git
   cd tu-repositorio
   ```

2. **Configurar las variables de entorno:**
   - Crear un archivo `.env` con las configuraciones necesarias.

3. **Construir y ejecutar con Docker Compose:**
   ```sh
   docker-compose up -d
   ```


