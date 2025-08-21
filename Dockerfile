# ---- Build Stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Maven Wrapper + pom.xml zuerst kopieren (Caching für Dependencies)
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml .

# Dependencies herunterladen (so müssen sie nicht bei jedem kleinen Codechange neu gezogen werden)
RUN ./mvnw dependency:go-offline -B

# Jetzt den Rest des Codes kopieren
COPY src ./src

# App bauen (Tests überspringen spart Zeit im Container)
RUN ./mvnw clean package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# JAR aus dem Build Stage ins Runtime Image kopieren
COPY --from=build /app/target/*.jar app.jar

# Container starten
ENV PORT 8080
ENTRYPOINT ["java","-jar","app.jar"]
