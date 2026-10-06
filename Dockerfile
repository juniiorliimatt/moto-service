# syntax=docker/dockerfile:1

# Versão de Java fixada e única em todo o monorepo (workbox-api, budget-service, notes, forza,
# backup e esta imagem) — ver java.toolchain.languageVersion em build.gradle. Mude em todos
# juntos se atualizar.
ARG JAVA_VERSION=25

FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine AS build
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
# Cache mount evita rebaixar a distribuição do Gradle e re-resolver as dependências a cada build.
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon dependencies || true
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
