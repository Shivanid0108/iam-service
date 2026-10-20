# ---- Stage 1: build the app (needs the full JDK and Maven) ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -DskipTests package

# ---- Stage 2: run the app (needs only the smaller JRE) ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]