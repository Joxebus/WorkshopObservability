FROM eclipse-temurin:21-jre-alpine
ENV SERVER_PORT 8081
RUN apk --no-cache add curl
EXPOSE ${SERVER_PORT}/tcp
COPY target/person-service-*.jar person-service.jar
CMD java ${JAVA_OPTS} -jar person-service.jar --spring.profiles.active=docker
