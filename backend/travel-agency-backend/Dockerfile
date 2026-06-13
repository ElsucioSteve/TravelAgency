# Imagen base: Java 17 con JRE liviano
FROM eclipse-temurin:17-jre-alpine

# Carpeta de trabajo dentro del container
WORKDIR /app

# Copiar el .jar ya generado a la imagen
COPY target/travel-agency-backend.jar app.jar

# Puerto que expone (informativo)
EXPOSE 8090

# Comando que corre cuando arranca el container
ENTRYPOINT ["java", "-jar", "app.jar"]