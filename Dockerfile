# ---- Build Stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Maven Wrapper + pom.xml zuerst kopieren (Caching für Dependencies)
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Dependencies herunterladen
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Restlichen Code kopieren
COPY src ./src

# App bauen (Tests überspringen)
RUN ./mvnw clean package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# CA Certificates installieren für TLS / SSL
RUN apk add --no-cache ca-certificates

# JAR aus Build Stage kopieren
COPY --from=build /app/target/*.jar app.jar

# Render PORT verwenden
ENV PORT=8080

# Start Befehl mit Render PORT und TLS-Unterstützung
ENTRYPOINT ["sh","-c","java -jar /app/app.jar --server.port=$PORT"]
