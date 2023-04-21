FROM gradle:8.1.0-jdk17 as BUILD

RUN mkdir -p /home/gradle/src
WORKDIR /home/gradle/src
COPY . /home/gradle/src
RUN gradle --no-daemon shadowJar

FROM openjdk:17
RUN mkdir /app
COPY --from=BUILD /home/gradle/src/build/libs/gismeteoParser-1.0-all.jar /app/run.jar
WORKDIR /app

ENTRYPOINT ["java", "-jar", "run.jar"]
