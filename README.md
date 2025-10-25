# Draw2Draw 后端项目

## 项目简介

Draw2Draw 是一个基于 Spring Boot 的前后端分离绘图应用后端服务，提供用户管理、图片处理、团队协作空间等功能。

## 技术栈

### 核心框架
- **Spring Boot** 2.6.13 - 核心框架
- **Java** 1.8 - 开发语言
- **Maven** - 构建工具

### 数据存储
- **MySQL** 8.0+ - 关系型数据库
- **MyBatis Plus** 3.5.14 - ORM框架
- **Redis** - 缓存和会话存储
- **HikariCP** - 数据库连接池

### 工具库
- **Lombok** - 减少模板代码
- **Hutool** 5.8.38 - 工具类库
- **Jackson** - JSON处理
- **Knife4j** 4.4.0 - API文档生成

### 认证与权限
- **Sa-Token** 1.39.0 - 认证授权框架
- **Spring Security** - 安全框架

### 异步处理
- **Quartz** - 定时任务调度
- **Disruptor** - 高性能异步队列

### 文件存储
- **腾讯云 COS** - 对象存储服务

### 实时通信
- **WebSocket** - 实时协同编辑
- **Spring WebSocket** - WebSocket支持

## 项目结构

```
draw2draw-backend/
├── src/main/java/com/xm/draw2drawbackend/
│   ├── controller/          # 控制器层
│   ├── service/            # 业务逻辑层
│   ├── mapper/             # 数据访问层
│   ├── model/              # 实体和模型
│   ├── manager/            # 业务管理器
│   │   ├── auth/          # 权限管理
│   │   └── websocket/     # WebSocket处理
│   ├── config/            # 配置类
│   ├── common/            # 公共类
│   └── utils/             # 工具类
├── src/main/resources/
│   ├── application.yml     # 主配置文件
│   ├── application-dev.yml # 开发环境配置
│   └── application-prod.yml # 生产环境配置
└── pom.xml                # Maven配置文件
```

## 环境要求

- **Java** 1.8+
- **Maven** 3.6+
- **MySQL** 8.0+
- **Redis** 6.0+

## 快速开始

### 1. 数据库配置

创建MySQL数据库：
```sql
CREATE DATABASE d2d CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 修改配置文件

编辑 `src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/d2d
    username: your_username
    password: your_password

  redis:
    host: localhost
    port: 6379
    database: 1

server:
  port: 8090
  servlet:
    context-path: /api
```

### 3. 启动应用

```bash
# 进入项目目录
cd draw2draw-backend

# 编译项目
mvn clean install

# 启动应用
mvn spring-boot:run

# 或者使用开发环境配置
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. 访问应用

- **API接口**: http://localhost:8090/api
- **Swagger文档**: http://localhost:8090/api/doc.html
- **健康检查**: http://localhost:8090/api/health

## 核心功能

### 用户管理
- 用户注册、登录、权限管理
- 基于Sa-Token的认证授权
- 支持多种角色权限控制

### 图片管理
- 图片上传、下载、预览
- 图片格式转换和压缩
- 批量处理和分类管理

### 团队空间
- 多用户协作空间
- 权限控制和成员管理
- 空间容量和配额管理

### 实时协同编辑
- 基于WebSocket的实时通信
- 多用户同时编辑支持
- 编辑冲突检测和解决

### AI功能集成
- 阿里云AI服务集成
- 智能图片处理和分析
- 自动标签生成

## API文档

项目使用Knife4j生成API文档，启动后可访问：
- **开发环境**: http://localhost:8090/api/doc.html
- **生产环境**: 根据具体配置访问

主要API模块：
- 用户管理 (`/user`)
- 图片管理 (`/picture`)
- 空间管理 (`/space`)
- 文件上传 (`/upload`)

## 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/d2d
    username: root
    password: 123456
```

### Redis配置
```yaml
spring:
  redis:
    database: 1
    host: localhost
    port: 6379
    timeout: 5000
```

### 文件上传配置
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 100MB
```

### COS对象存储配置
```yaml
cos:
  client:
    host: https://your-bucket.cos.ap-guangzhou.myqcloud.com
    secretId: your-secret-id
    secretKey: your-secret-key
    region: ap-guangzhou
    bucket: your-bucket-name
```

## 开发指南

### 代码规范
- 遵循Spring Boot最佳实践
- 使用Lombok减少模板代码
- 统一异常处理和响应格式
- API接口需添加Swagger注解

### 数据库规范
- 表名使用下划线命名法
- 字段名使用下划线命名法
- 必须包含 `id`, `create_time`, `update_time`, `is_deleted` 字段
- 使用MyBatis Plus进行数据库操作

### 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 权限控制
- 基于Sa-Token进行权限控制
- 支持角色权限和资源权限
- 自定义权限注解 `@AuthCheck`

## 部署说明

### 生产环境配置
1. 修改数据库连接配置
2. 配置Redis连接
3. 设置COS存储参数
4. 配置日志级别
5. 设置JVM参数

## 监控和日志

### 日志配置
- 开发环境：控制台输出，SQL日志
- 生产环境：文件输出，错误日志

### 健康检查
- Spring Boot Actuator健康检查
- 数据库连接状态监控
- Redis连接状态监控

## 常见问题

### Q: WebSocket连接失败？
A: 检查以下配置：
1. 后端服务是否正常启动
2. 防火墙是否允许WebSocket连接
3. 用户是否已登录且有编辑权限
4. 图片是否属于团队空间

### Q: 文件上传失败？
A: 检查：
1. COS配置是否正确
2. 文件大小是否超过限制
3. 网络连接是否正常

### Q: 数据库连接异常？
A: 检查：
1. 数据库服务是否启动
2. 连接配置是否正确
3. 用户权限是否足够

## 联系方式

如有问题或建议，请联系开发团队。