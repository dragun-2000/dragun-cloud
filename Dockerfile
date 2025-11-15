### BUILD STAGE
FROM maven:3.9.6-eclipse-temurin-11 AS builder

WORKDIR /build

# Copy pom.xml & download dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source code
COPY . .

# Build fat jar
RUN mvn -B clean package spring-boot:repackage -DskipTests

### RUNTIME STAGE
FROM eclipse-temurin:11-jdk AS runtime

# App working directory
ENV APP_HOME=/app
WORKDIR $APP_HOME

# Create folders
RUN mkdir -p $APP_HOME/config $APP_HOME/log
RUN chmod -R 777 $APP_HOME

# Expose port
EXPOSE 8085

# Copy fat jar from builder
COPY --from=builder /build/target/*.jar app.jar

# Copy wait-for-it.sh
COPY wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

# JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Volumes for logs/config
VOLUME $APP_HOME/log
VOLUME $APP_HOME/config

# Entrypoint: wait for Redis & DB
ENTRYPOINT ["/wait-for-it.sh", "debase-redis:6379", "--", "/wait-for-it.sh", "debase-db:5432", "--", "java", "-jar", "app.jar"]
