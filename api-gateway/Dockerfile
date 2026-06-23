FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY erp-service/pom.xml erp-service/
COPY crm-service/pom.xml crm-service/
COPY supply-chain-service/pom.xml supply-chain-service/

RUN mvn dependency:go-offline -B

COPY api-gateway/src api-gateway/src
RUN mvn clean package -DskipTests -pl api-gateway -am

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
