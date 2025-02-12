# Etapa de construcción
FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /app

# Copia los archivos de Maven y descarga las dependencias
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn dependency:go-offline

# Copia el código fuente del proyecto y compila la aplicación
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM openjdk:17-jdk-alpine

# Crear un usuario no root para mayor seguridad
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /backend

# Copia el JAR compilado desde la etapa de construcción
COPY --from=build /app/target/WhatsappApiCloud-0.0.1-SNAPSHOT.jar /backend/WhatsappApiCloud-app.jar

# Asigna permisos al usuario no root
RUN chown -R spring:spring /backend
USER spring:spring

# Exponer el puerto donde corre la app (Asegúrate de que sea el correcto)
EXPOSE 8080

# Comando para ejecutar la aplicación
CMD ["java", "-jar", "WhatsappApiCloud-app.jar"]
