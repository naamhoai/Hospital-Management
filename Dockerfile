FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B package
FROM tomcat:10.1-jdk17-temurin
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Ho_Chi_Minh"
COPY --from=build /build/target/hospital.war /usr/local/tomcat/webapps/hospital.war
