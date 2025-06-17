FROM eclipse-temurin:21-jre-alpine

# Add non-root user for security
RUN addgroup -S gateway && adduser -S gateway -G gateway

# Install curl for health checks and timezone data
RUN apk add --no-cache curl tzdata

WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs && chown -R gateway:gateway /app

# Copy the pre-built JAR (built locally)
COPY target/gateway-service-*.jar app.jar

# Change ownership to non-root user
RUN chown -R gateway:gateway /app
USER gateway

# Expose ports
EXPOSE 8080 8443

# JVM optimizations for containers and JDK 21
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]