# ==========================================
# STAGE 1: Build Stage
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy maven wrapper & pom.xml first to leverage Docker layer caching
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

# Create data directory for persistent H2 database
RUN mkdir -p /app/data && chmod 777 /app/data

# Copy compiled jar from builder stage
COPY --from=builder /app/target/kutatela-mama-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
