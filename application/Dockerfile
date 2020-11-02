FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY /build/resources/main/datamodel/ /tmp/datamodel
COPY /build/resources/main/policies/ /tmp/policies
COPY /build/libs/compositedatastore-1.0.0.RELEASE.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Dspring.profiles.active=docker","-jar","/app.jar"]