FROM eclipse-temurin:21-jdk-alpine AS builder

RUN apk add --no-cache curl

WORKDIR /app

COPY gradle ./gradle
COPY gradlew ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

RUN curl -L "https://github.com/gradle/gradle/raw/refs/tags/v8.12.1/gradle/wrapper/gradle-wrapper.jar" \
    -o gradle/wrapper/gradle-wrapper.jar

COPY build.gradle ./
RUN echo "rootProject.name = 'app'" > settings.gradle

RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew shadowJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--server"]
