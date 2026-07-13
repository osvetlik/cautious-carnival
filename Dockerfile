FROM maven:3-eclipse-temurin-25-noble AS build
LABEL stage=builder
WORKDIR /src
COPY . /src
RUN mvn clean package

FROM eclipse-temurin:25-jre-noble AS run
WORKDIR /app
COPY --from=build /src/target/cautious-carnival*.jar /app/app.jar
RUN groupadd --system app \
 && useradd --system \
            --gid app \
            --home-dir /nonexistent \
            --shell /usr/sbin/nologin \
            app
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
