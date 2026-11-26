# Imagem única do Torque: o Spring Boot serve a API (/api) e o build do Angular.
# Build: docker build -t torque .
# Os testes não rodam aqui (precisam de PostgreSQL); rodam com `mvn -B verify` antes do merge.

# 1) Frontend
FROM node:20-alpine AS web
WORKDIR /web
COPY torque-web/package.json torque-web/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY torque-web/ ./
RUN npm run build

# 2) API com o frontend em static/
FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /api
COPY torque-api/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY torque-api/src ./src
COPY --from=web /web/dist/torque-web/browser/ ./src/main/resources/static/
RUN mvn -B -q package -DskipTests

# 3) Execução
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S torque && adduser -S torque -G torque
COPY --from=api /api/target/*.jar app.jar
USER torque
# Ajustado para planos com 512 MB de RAM (ex.: Render gratuito).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
