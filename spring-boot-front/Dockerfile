FROM eclipse-temurin:21-jre-alpine
ENV SERVER_PORT 8080
RUN apk --no-cache add curl
EXPOSE ${SERVER_PORT}/tcp
COPY target/person-front-*.jar person-front.jar
CMD java ${JAVA_OPTS} -jar person-front.jar --spring.cloud.consul.host=consul --logstash.host=logstash
