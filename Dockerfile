FROM openjdk:11-jre-slim

WORKDIR app
EXPOSE 8080
RUN apt update && \
    apt upgrade -y && \
    apt install curl -y
COPY target/cx-integrations-repos-manager-*.jar cx-integrations-repos-manager.jar
HEALTHCHECK CMD curl http://localhost:8080/actuator/health

ENTRYPOINT ["java", "-Xms512m", "-Xmx2048m", "-jar", "cx-integrations-repos-manager.jar"]