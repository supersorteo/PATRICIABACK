# Usar una imagen base de Eclipse Temurin Java 21
FROM eclipse-temurin:21-jdk

COPY target/exellsior-0.0.1-SNAPSHOT.jar java-app.jar
#COPY target/app.jar java-app.jar


# Exponer el puerto 8080
EXPOSE 8080

# Configurar el comando de entrada para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "java-app.jar"]
