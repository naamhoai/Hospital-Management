FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B package
FROM tomcat:10.1-jdk17-temurin
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Ho_Chi_Minh"
# Tomcat trong container cũng nghe cổng 3000, thống nhất với cách chạy local.
RUN sed -i -E '/protocol="HTTP\/1\.1"/s/port="[0-9]+"/port="3000"/' /usr/local/tomcat/conf/server.xml
EXPOSE 3000
COPY --from=build /build/target/hospital.war /usr/local/tomcat/webapps/hospital.war
