FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar /app/app.jar

ENV OUTPUT_DIR=/output

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "\
  if [ \"$MODE\" = 'pipeline' ]; then \
    java -cp app.jar edu.asu.ser516.metrics.MetricPipelineMain \"$INPUT_PATH\" \"$OUTPUT_DIR\"; \
  else \
    java -jar app.jar; \
  fi"]