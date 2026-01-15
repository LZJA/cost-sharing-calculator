# 成本分摊计算器后端服务

基于 Java Spring Boot + MySQL 的成本分摊计算器后端服务。

## 技术栈

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- MySQL 8.0+
- Lombok
- Maven

## 项目结构

```
backend/
├── src/main/java/com/costsharing/calculator/
│   ├── entity/          # 实体类
│   │   ├── Card.java           # 卡片配置实体
│   │   ├── LiziBill.java       # 李子账单实体
│   │   └── GeziBill.java       # 鸽子账单实体
│   ├── repository/      # 数据访问层
│   │   ├── CardRepository.java
│   │   ├── LiziBillRepository.java
│   │   └── GeziBillRepository.java
│   ├── service/         # 业务逻辑层
│   │   ├── CardService.java
│   │   ├── LiziBillService.java
│   │   └── GeziBillService.java
│   ├── controller/      # 接口层
│   │   ├── CardController.java
│   │   ├── LiziBillController.java
│   │   └── GeziBillController.java
│   ├── dto/             # 数据传输对象
│   │   ├── ApiResponse.java
│   │   ├── CardRequest.java
│   │   ├── LiziBillRequest.java
│   │   └── GeziBillRequest.java
│   ├── config/          # 配置类
│   │   └── WebConfig.java
│   └── CostSharingApplication.java  # 主类
├── src/main/resources/
│   ├── application.yml  # 应用配置
│   └── schema.sql       # 数据库初始化脚本
└── pom.xml              # Maven 配置
```

## 快速开始

### 1. 环境要求

- JDK 17 或更高版本
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库配置

创建数据库并执行初始化脚本：

```bash
mysql -u root -p < src/main/resources/schema.sql
```

或手动在 MySQL 中执行：

```sql
CREATE DATABASE IF NOT EXISTS cost_sharing_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
```

然后执行 `schema.sql` 中的建表语句。

### 3. 修改配置

编辑 `src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cost_sharing_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password_here # 修改为你的MySQL密码
```

### 4. 编译项目

```bash
cd backend
mvn clean install
```

### 5. 运行项目

```bash
mvn spring-boot:run
```

或者运行打包后的 jar：

```bash
java -jar target/cost-sharing-backend-1.0.0.jar
```

服务启动后访问：`http://localhost:8080/api`

## API 接口文档

### 基础响应格式

所有接口返回统一的响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 卡片管理接口

#### 1. 获取所有卡片

```
GET /api/cards
```

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "type": "lizi",
      "name": "李子的分账计算器",
      "description": "分账让生活更简单",
      "avatar": "🍐",
      "background": "",
      "enableBackground": true,
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    }
  ]
}
```

#### 2. 根据类型获取卡片

```
GET /api/cards/{type}
```

**参数：**

- `type`: 卡片类型（lizi 或 gezi）

#### 3. 保存或更新卡片

```
POST /api/cards
Content-Type: application/json
```

**请求体：**

```json
{
  "type": "lizi",
  "name": "李子的分账计算器",
  "description": "分账让生活更简单",
  "avatar": "🍐",
  "background": "/path/to/image.jpg",
  "enableBackground": true
}
```

### 李子账单接口

#### 1. 计算并保存账单

```
POST /api/lizi-bills
Content-Type: application/json
```

**请求体：**

```json
{
  "month": 1,
  "year": 2024,
  "totalDays": 31,
  "waterBill": 100.5,
  "electricBill": 200.75,
  "gasBill": 80.25,
  "ownerDays": 15
}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "month": 1,
    "year": 2024,
    "totalDays": 31,
    "waterBill": 100.5,
    "electricBill": 200.75,
    "gasBill": 80.25,
    "ownerDays": 15,
    "totalAmount": 381.5,
    "ownerAmount": 92.42,
    "remainingAmount": 289.08,
    "sisterAmount": 144.54,
    "datouAmount": 144.54,
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

#### 2. 分页查询账单列表

```
GET /api/lizi-bills?page=0&size=10&year=2024&month=1
```

**查询参数：**

- `page`: 页码（从 0 开始，默认 0）
- `size`: 每页大小（默认 10）
- `year`: 年份（可选，用于筛选）
- `month`: 月份（可选，1-12，用于筛选）

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "month": 1,
        "year": 2024,
        "totalDays": 31,
        "waterBill": 100.5,
        "electricBill": 200.75,
        "gasBill": 80.25,
        "ownerDays": 15,
        "totalAmount": 381.5,
        "ownerAmount": 92.42,
        "remainingAmount": 289.08,
        "sisterAmount": 144.54,
        "datouAmount": 144.54,
        "createdAt": "2024-01-01T10:00:00"
      }
    ],
    "totalElements": 100,
    "totalPages": 10,
    "size": 10,
    "number": 0,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

**使用示例：**

- 获取所有账单第一页：`GET /api/lizi-bills?page=0&size=10`
- 获取 2024 年的账单：`GET /api/lizi-bills?year=2024&page=0&size=10`
- 获取 2024 年 1 月的账单：`GET /api/lizi-bills?year=2024&month=1&page=0&size=10`

#### 3. 删除账单

```
DELETE /api/lizi-bills/{id}
```

### 鸽子账单接口

#### 1. 计算并保存账单

```
POST /api/gezi-bills
Content-Type: application/json
```

**请求体：**

```json
{
  "waterBill": 100.0,
  "electricBill": 200.0,
  "gasBill": 80.0,
  "splitRule": "普通分账",
  "year": 2024,
  "month": 1
}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "waterBill": 100.0,
    "electricBill": 200.0,
    "gasBill": 80.0,
    "splitRule": "普通分账",
    "year": 2024,
    "month": 1,
    "totalAmount": 380.0,
    "liziAmount": 78.33,
    "geziAmount": 78.33,
    "chunfengAmount": 111.67,
    "chengziAmount": 111.67,
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

#### 2. 分页查询账单列表

```
GET /api/gezi-bills?page=0&size=10&year=2024&month=1
```

**查询参数：**

- `page`: 页码（从 0 开始，默认 0）
- `size`: 每页大小（默认 10）
- `year`: 年份（可选，用于筛选）
- `month`: 月份（可选，1-12，用于筛选）

**使用示例：**

- 获取所有账单第一页：`GET /api/gezi-bills?page=0&size=10`
- 获取 2024 年的账单：`GET /api/gezi-bills?year=2024&page=0&size=10`
- 获取 2024 年 1 月的账单：`GET /api/gezi-bills?year=2024&month=1&page=0&size=10`

#### 3. 删除账单

```
DELETE /api/gezi-bills/{id}
```

## 数据库表结构

### cards - 卡片配置表

| 字段              | 类型         | 说明                  |
| ----------------- | ------------ | --------------------- |
| id                | BIGINT       | 主键 ID               |
| type              | VARCHAR(10)  | 卡片类型（lizi/gezi） |
| name              | VARCHAR(100) | 卡片名称              |
| description       | VARCHAR(200) | 描述语                |
| avatar            | VARCHAR(10)  | 头像（emoji）         |
| background        | VARCHAR(500) | 背景图片路径          |
| enable_background | BOOLEAN      | 是否启用背景图        |
| created_at        | TIMESTAMP    | 创建时间              |
| updated_at        | TIMESTAMP    | 更新时间              |

### lizi_bills - 李子账单表

| 字段             | 类型          | 说明             |
| ---------------- | ------------- | ---------------- |
| id               | BIGINT        | 主键 ID          |
| month            | INT           | 月份 (1-12)      |
| year             | INT           | 年份             |
| total_days       | INT           | 月份总天数       |
| water_bill       | DECIMAL(10,2) | 水费             |
| electric_bill    | DECIMAL(10,2) | 电费             |
| gas_bill         | DECIMAL(10,2) | 燃气费           |
| owner_days       | INT           | 房主居住天数     |
| total_amount     | DECIMAL(10,2) | 总费用           |
| owner_amount     | DECIMAL(10,2) | 房主应承担费用   |
| remaining_amount | DECIMAL(10,2) | 剩余费用         |
| sister_amount    | DECIMAL(10,2) | 谢林珠应承担费用 |
| datou_amount     | DECIMAL(10,2) | 张锦豪应承担费用 |
| created_at       | TIMESTAMP     | 创建时间         |

### gezi_bills - 鸽子账单表

| 字段            | 类型          | 说明           |
| --------------- | ------------- | -------------- |
| id              | BIGINT        | 主键 ID        |
| water_bill      | DECIMAL(10,2) | 水费           |
| electric_bill   | DECIMAL(10,2) | 电费           |
| gas_bill        | DECIMAL(10,2) | 燃气费         |
| split_rule      | VARCHAR(20)   | 分账规则       |
| year            | INT           | 年份           |
| month           | INT           | 月份 (1-12)    |
| total_amount    | DECIMAL(10,2) | 总费用         |
| lizi_amount     | DECIMAL(10,2) | 李子应承担费用 |
| gezi_amount     | DECIMAL(10,2) | 鸽子应承担费用 |
| chunfeng_amount | DECIMAL(10,2) | 春风应承担费用 |
| chengzi_amount  | DECIMAL(10,2) | 橙子应承担费用 |
| created_at      | TIMESTAMP     | 创建时间       |

## 常见问题

### 1. 启动时数据库连接失败

检查 `application.yml` 中的数据库配置是否正确，确保 MySQL 服务已启动。

### 2. 端口被占用

修改 `application.yml` 中的 `server.port` 配置。

### 3. 跨域问题

已在 `WebConfig` 中配置了 CORS，允许所有域名访问。生产环境建议配置具体的允许域名。

## 开发建议

1. 使用 IDE（IntelliJ IDEA 推荐）导入 Maven 项目
2. 启用 Lombok 插件
3. 使用 Postman 或类似工具测试 API
4. 查看日志文件排查问题

## 许可证

MIT License
