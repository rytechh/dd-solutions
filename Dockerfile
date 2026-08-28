# ---------- Estágio 1: build ----------
# O JAR é compilado dentro da imagem: o avaliador não precisa de Java nem Maven na máquina.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependências primeiro para aproveitar o cache de camadas do Docker.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Estágio 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Aplicação não roda como root.
RUN addgroup -S greenshift && adduser -S greenshift -G greenshift

COPY --from=build /build/target/*.jar app.jar
RUN chown greenshift:greenshift app.jar
USER greenshift

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
