FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B


FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd -r fraudsentinel && useradd -r -m -d /home/fraudsentinel -g fraudsentinel fraudsentinel

COPY --from=build /app/target/*.jar app.jar
RUN chown fraudsentinel:fraudsentinel app.jar && mkdir -p /home/fraudsentinel/uploads && chown -R fraudsentinel:fraudsentinel /home/fraudsentinel

USER fraudsentinel

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
