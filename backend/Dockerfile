FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp verify

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system contamil && useradd --system --gid contamil contamil
COPY --from=build --chown=contamil:contamil /app/target/contamil-0.1.0-SNAPSHOT.jar /app/contamil.jar
USER contamil
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/contamil.jar"]
