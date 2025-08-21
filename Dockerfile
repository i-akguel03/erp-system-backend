# ---- Build Stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Maven Wrapper + pom.xml zuerst kopieren (Caching für Dependencies)
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Restlichen Code kopieren
COPY src ./src

# App bauen (Tests überspringen spart Zeit im Container)
RUN ./mvnw clean package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jdk
WORKDIR /app

# JAR aus Build Stage kopieren
COPY --from=build /app/target/*.jar app.jar

# Render Port (wird dynamisch gesetzt)
ENV PORT=8080

# Java starten mit dynamischem Port
ENTRYPOINT ["sh","-c","java -Dhttps.protocols=TLSv1.2 -jar /app/app.jar --server.port=$PORT"]
