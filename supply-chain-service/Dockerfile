FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY erp-service/pom.xml erp-service/
COPY crm-service/pom.xml crm-service/
COPY supply-chain-service/pom.xml supply-chain-service/

RUN mvn dependency:go-offline -B

COPY supply-chain-service/src supply-chain-service/src
RUN mvn clean package -DskipTests -pl supply-chain-service -am

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/supply-chain-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
