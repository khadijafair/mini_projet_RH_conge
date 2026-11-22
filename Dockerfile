# --- Étape 1 : compilation ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY lombok.config .
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Étape 2 : image d'exécution légère, utilisateur non root ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/target/rh-conges-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
