FROM gradle:8.10-jdk22 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

FROM openjdk:24-jdk-slim
RUN apt-get update
RUN apt-get install -y ffmpeg
EXPOSE 69
WORKDIR /run
COPY --from=build /home/gradle/src/build/libs/*.jar /run/server.jar
CMD java -jar /run/server.jar


#FROM gradle:7-jdk11 AS build
#COPY --chown=gradle:gradle . /home/gradle/src
#WORKDIR /home/gradle/src
#RUN gradle buildFatJar --no-daemon
#
#FROM ubuntu:22.04
#ENV DEBIAN_FRONTEND=noninteractive
#RUN apt-get update
#RUN apt-get install -y default-jre
## default-jdk ffmpeg curl unzip && \ apt-get clean
#RUN mkdir /app
#COPY --from=build /home/gradle/src/build/libs/*.jar /app/ktro-docker-server.jar
#CMD [ "java", "-jre", "/app/ktro-docker-server.jar" ]



# FROM bellsoft/liberica-openjdk-alpine:21
# RUN apk add --no-cache ffmpeg
# EXPOSE 69:69
# RUN mkdir /app
# COPY --from=build /home/gradle/src/build/libs/*.jar /app/ktor-docker-server.jar
# ENTRYPOINT ["java","-jar","/app/ktor-docker-server.jar"]
