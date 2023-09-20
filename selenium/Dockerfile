FROM maven:3-jdk-11 as builder
WORKDIR /build
COPY pom.xml /build
RUN mvn verify --fail-never
COPY src/ /build/src
RUN mvn package

FROM amazoncorretto:11-alpine
COPY --from=builder /build/target/selenium-scenario.jar /usr/local/
RUN mkdir /usr/local/images
#ENTRYPOINT ["java", "-jar", "/usr/local/selenium-scenario.jar"]

