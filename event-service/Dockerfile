FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle/ gradle/
RUN sed -i 's/\r$//' gradlew && chmod +x ./gradlew

COPY build.gradle* settings.gradle* gradle.properties* ./

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies || true

COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon clean bootJar

RUN mkdir -p /out && cp build/libs/*.jar /out/app.jar


FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /out/app.jar ./app.jar

EXPOSE 8082
ENV JAVA_OPTS=""
#ENV JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
