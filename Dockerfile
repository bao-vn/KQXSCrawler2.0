#FROM openjdk:8-jdk-alpine
#ARG JAR_FILE=target/kqxs-crawler-1.0.jar
#COPY ${JAR_FILE} kqxs-crawler-1.0.jar
#ENTRYPOINT ["java","-jar","/kqxs-crawler-1.0.jar
FROM openjdk:8 AS TEMP_BUILD_IMAGE
ENV APP_HOME=/go/src/github.com/bao-vn/KQXSCrawler2.0/
WORKDIR $APP_HOME
COPY build.gradle settings.gradle gradlew $APP_HOME
COPY gradle $APP_HOME/gradle
COPY src $APP_HOME/src
RUN ./gradlew build || return 0 
COPY . .
RUN ./gradlew build

FROM openjdk:8
ENV ARTIFACT_NAME=kqxs-crawler-1.0.jar
ENV APP_HOME=/go/src/github.com/bao-vn/KQXSCrawler2.0/
WORKDIR $APP_HOME
#COPY --from=TEMP_BUILD_IMAGE $APP_HOME/build/libs/$ARTIFACT_NAME .
COPY --from=TEMP_BUILD_IMAGE $APP_HOME/build/libs/kqxs-crawler-*.jar /kqxs-crawler.jar
EXPOSE 8080
CMD ["java","-jar","/kqxs-crawler.jar"]