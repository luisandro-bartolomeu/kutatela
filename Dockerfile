# ==========================================
# STAGE 1: Build Stage
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy maven wrapper & pom.xml first to leverage Docker layer caching for dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build production jar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ==========================================
# STAGE 2: Runtime Stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create non-root user for security
RUN addgroup -S kutatela && adduser -S kutatela -G kutatela

# Copy compiled jar from builder stage
COPY --from=builder /app/target/kutatela-mama-1.0.0-SNAPSHOT.jar app.jar

# Set ownership to non-root user
RUN chown -R kutatela:kutatela /app
USER kutatela

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
