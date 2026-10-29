FROM node:24-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package*.json ./
COPY frontend/.npmrc ./
RUN npm ci --no-audit --no-fund
COPY frontend ./
RUN npm run build
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
COPY --from=frontend /build/target/frontend ./target/frontend
RUN mvn -B -Dfrontend.skip=true package
FROM tomcat:10.1-jdk17-temurin
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Ho_Chi_Minh"
COPY --from=build /build/target/hospital.war /usr/local/tomcat/webapps/hospital.war
