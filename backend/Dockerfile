# Imagen de la API para la nube (Render u otro hosting con Docker).
# La configuración llega por variables de entorno PRECIOGAS_* (ver README); en la imagen no hay credenciales.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package

FROM tomcat:10.1-jre17-temurin
COPY --from=build /app/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war
ENV PRECIOGAS_ZONA=America/Mexico_City
EXPOSE 8080
# Render indica el puerto en $PORT; sin ella, 8080.
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/\" conf/server.xml && exec catalina.sh run"]
