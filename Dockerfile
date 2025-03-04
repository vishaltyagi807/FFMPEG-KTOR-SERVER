FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon


FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache ffmpeg
WORKDIR /app
COPY --from=build /app/build/libs/*.jar ./application.jar
EXPOSE 69
ENTRYPOINT ["java", "-jar", "/app/application.jar"]