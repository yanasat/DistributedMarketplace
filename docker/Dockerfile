FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/distributed-marketplace-1.0.0.jar SellerProcess.jar
COPY src/main/resources/ src/main/resources/

ENTRYPOINT ["sh", "-c", "java -jar SellerProcess.jar seller1.yaml"]
