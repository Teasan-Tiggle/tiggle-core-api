FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Seoul
COPY build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-Duser.timezone=Asia/Seoul","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]
