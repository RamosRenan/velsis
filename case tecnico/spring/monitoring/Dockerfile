FROM maven:3.9-amazoncorretto-17 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM amazoncorretto:17-alpine

LABEL org.opencontainers.image.title="monitoring" \
      org.opencontainers.image.description="Aplicacao de monitoramento Velsis" \
      org.opencontainers.image.version="1.0.0"

RUN apk add --no-cache ffmpeg

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
