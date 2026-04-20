#!/usr/bin/env bash

java -jar spring-boot-front/target/person-front-0.1-SNAPSHOT.jar --server.port=8080 >> front.log &
java -jar spring-boot-service/target/person-service-0.1-SNAPSHOT.jar --server.port=8081 >> service1.log &
java -jar spring-boot-service/target/person-service-0.1-SNAPSHOT.jar --server.port=8082 >> service2.log &
java -jar spring-boot-service/target/person-service-0.1-SNAPSHOT.jar --server.port=8083 >> service3.log &

echo "Running front node at port 8080"
echo "Running service nodes at ports [8081,8082,8083]"
echo "Service nodes logs [front.log, service1.log, service2.log, service3.log]"
