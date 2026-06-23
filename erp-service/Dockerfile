FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY erp-service/pom.xml erp-service/
COPY crm-service/pom.xml crm-service/
COPY supply-chain-service/pom.xml supply-chain-service/

RUN mvn dependency:go-offline -B

COPY erp-service/src erp-service/src
RUN mvn clean package -DskipTests -pl erp-service -am

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/erp-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
