# Sprint Despliegue (ADR 016) — build multi-stage: JDK completo solo en el
# stage de build, runtime en JRE headless liviano. Java 21 (pom.xml).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Capa de dependencias separada del código: cachea `mvn dependency:go-offline`
# mientras no cambie el pom.xml, así un cambio de código no vuelve a bajar
# todo Maven Central en cada build de Coolify.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Corre sin root — no hay razón para que el proceso tenga privilegios de
# superusuario dentro del contenedor.
RUN useradd --system --create-home appuser
USER appuser

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

# JVM headless (sin X11) — no hace falta para un backend REST. Curl-based
# healthcheck alternativo: Coolify puede pegarle directo a /actuator/health.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Djava.awt.headless=true", "-jar", "app.jar"]
