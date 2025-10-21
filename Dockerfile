FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --no-daemon help

COPY src ./src
RUN ./gradlew --no-daemon -x test bootJar

FROM eclipse-temurin:17-jre-jammy AS runtime

RUN addgroup --system app && adduser --system --ingroup app app
USER app

WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]