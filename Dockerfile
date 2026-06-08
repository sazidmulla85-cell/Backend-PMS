FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /opt/backend-pms

RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /var/lib/hotel-pms/uploads \
    && chown -R spring:spring /opt/backend-pms /var/lib/hotel-pms

COPY --from=build /app/target/Backend-PMS-0.0.1-SNAPSHOT.jar app.jar

USER spring

ENV PMS_PRODUCTION_MODE=true
ENV PMS_UPLOAD_DIR=/var/lib/hotel-pms/uploads
ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
