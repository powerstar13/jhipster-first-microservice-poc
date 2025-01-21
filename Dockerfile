FROM bellsoft/liberica-openjre-alpine:21

ADD ./build/libs/first-microservice-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
