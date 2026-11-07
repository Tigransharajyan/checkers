# Stage one: build the executable JAR with the JDK and Maven.
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy build configuration first so dependencies stay cached until pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Resolve dependencies before the source-code layer.
RUN ./mvnw dependency:go-offline -q

# Build the application. Tests run separately before publication and are skipped in this layer.
COPY src/ src/
RUN ./mvnw clean package -DskipTests -q

# Select the executable JAR, excluding a plain archive without bundled dependencies.
RUN cp $(find target -maxdepth 1 -name "*.jar" ! -name "*-plain.jar" | head -n 1) app.jar

# Stage two: run with the JRE; the final image does not need Maven or a compiler.
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Run the application as a dedicated non-root user.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

# Render supplies PORT; application.yml uses it instead of the default local port.
ENTRYPOINT ["java", "-jar", "app.jar"]
