# Dockerfile for Java Rational Agent
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY java-agent/ /app/
RUN mkdir -p bin && \
    find src/main/java -name "*.java" > sources.txt && \
    javac -encoding UTF-8 -d bin @sources.txt

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/bin /app/bin
COPY frontend/ /app/frontend/

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

CMD ["java", "-cp", "bin", "com.disaster.agent.DisasterAgentApp", "8080"]
