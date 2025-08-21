FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:21-jdk
WORKDIR /app

# JAR kopieren
COPY --from=build /app/target/*.jar app.jar

# Render PORT
ENV PORT=8080
ENTRYPOINT ["sh","-c","java -jar /app/app.jar --server.port=$PORT"]
