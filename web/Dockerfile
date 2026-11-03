# Docker image of the web module. Build context is the repository root (core and persistence
# are needed too):  docker build -f web/Dockerfile -t mpf-labs-web .

# ====== build stage ======
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
# POM files first: the dependency layer is rebuilt only when a pom.xml changes.
COPY pom.xml .
COPY core/pom.xml core/pom.xml
COPY persistence/pom.xml persistence/pom.xml
COPY web/pom.xml web/pom.xml
RUN mvn -B -q dependency:go-offline

COPY core core
COPY persistence persistence
COPY web web
# Tests already ran in GitHub Actions against PostgreSQL; here only the JAR is built.
RUN mvn -pl web -am -B package -DskipTests

# ====== runtime stage ======
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/web/target/web-*.jar app.jar
USER app
EXPOSE 8080
# The heap follows the memory limit of the container (512 MB on the Render free plan).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
