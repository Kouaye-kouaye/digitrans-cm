FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY erp-service/pom.xml erp-service/
COPY crm-service/pom.xml crm-service/
COPY supply-chain-service/pom.xml supply-chain-service/

RUN mvn dependency:go-offline -B

COPY crm-service/src crm-service/src
RUN mvn clean package -DskipTests -pl crm-service -am

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/crm-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
