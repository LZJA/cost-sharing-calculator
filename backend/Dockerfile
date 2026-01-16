# 第一阶段：构建应用
FROM amazoncorretto:17 AS builder

# 设置工作目录
WORKDIR /app

# 安装必要的工具
RUN yum install -y wget tar gzip

# 下载并安装 Maven 3.9.5
RUN wget https://archive.apache.org/dist/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz && \
    tar -xzf apache-maven-3.9.5-bin.tar.gz -C /opt && \
    rm apache-maven-3.9.5-bin.tar.gz && \
    ln -s /opt/apache-maven-3.9.5/bin/mvn /usr/bin/mvn

# 复制 Maven 配置文件
COPY pom.xml .

# 下载依赖（利用 Docker 缓存）
RUN mvn dependency:go-offline -B || true

# 复制源代码
COPY src ./src

# 编译项目
RUN mvn clean package -DskipTests

# 第二阶段：运行应用
FROM amazoncorretto:17

# 设置工作目录
WORKDIR /app

# 从构建阶段复制编译好的 jar 文件
COPY --from=builder /app/target/cost-sharing-backend-1.0.0.jar ./app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
CMD ["java", "-jar", "app.jar"]
