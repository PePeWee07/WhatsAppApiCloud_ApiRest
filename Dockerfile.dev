# Etapa de build (Maven + JDK 21 LTS)
FROM maven:3.9.11-eclipse-temurin-21-noble AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Etapa de runtime (JRE 21 LTS)
FROM eclipse-temurin:21-jre-noble
WORKDIR /backend

# Crear usuario no-root (en Ubuntu noble/jammy usa 'useradd')
RUN useradd --system --create-home --shell /usr/sbin/nologin spring \
 && mkdir -p /backend/var/log

COPY --chown=spring:spring --from=build /app/target/*SNAPSHOT.jar /backend/app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java","-jar","/backend/app.jar"]
