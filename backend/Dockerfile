# 阶段7：后端镜像（构建阶段用 Maven，运行阶段只保留 JRE）
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先只拷贝 pom.xml 以便充分利用依赖层缓存
COPY pom.xml ./
RUN mvn -B -ntp -q dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -q -DskipTests package

FROM eclipse-temurin:21-jre

# 健康检查需要 curl；镜像构建时一次性安装并清理缓存
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/target/library-backend-*.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Shanghai" \
    TZ=Asia/Shanghai

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=45s --retries=6 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
