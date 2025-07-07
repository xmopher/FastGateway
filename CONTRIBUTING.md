# 贡献指南

欢迎为 Mo Gateway 项目贡献代码！

## 🛠️ 开发环境

**必需软件：**
- JDK 21+
- Maven 3.8+
- Docker & Redis

**快速启动：**
```bash
# 克隆项目
git clone <repository-url>
cd general-gateway

# 启动 Redis
docker-compose up -d redis

# 构建并运行
mvn clean compile
mvn spring-boot:run

# 验证
curl http://localhost:8080/actuator/health
```

## 🔄 开发流程

1. **创建分支**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **开发代码**
   - 遵循代码规范
   - 添加必要测试
   - 更新相关文档

3. **提交代码**
   ```bash
   git commit -m "feat: your feature description"
   git push origin feature/your-feature-name
   ```

4. **创建 Pull Request**

## 📝 代码规范

**命名规范：**
```java
// 类名：PascalCase
public class AuthenticationService {}

// 方法名：camelCase  
public boolean authenticateUser() {}

// 常量：UPPER_SNAKE_CASE
public static final String DEFAULT_PROVIDER = "jwt";
```

**注释要求：**
- 公共类和方法必须有 JavaDoc
- 复杂逻辑添加行内注释

## 🧪 测试要求

- 新增功能必须包含单元测试
- 运行测试：`mvn test`

## 📋 提交规范

使用以下格式：

类型：
feat: 新功能
fix: 修复
docs: 文档
refactor: 重构
test: 测试


**示例：**
```bash
git commit -m "feat: add JWT authentication support"
git commit -m "fix: resolve rate limiting memory leak"
git commit -m "docs: update API documentation"
```

## 🔌 插件开发

**认证插件示例：**
```java
public class MyAuthProvider implements AuthenticationProvider {
    @Override
    public String getProviderName() {
        return "my-auth";
    }
    
    @Override
    public AuthenticationResult authenticate(AuthenticationRequest request, 
                                           AuthenticationContext context) {
        // 实现认证逻辑
        return AuthenticationResult.success(userId, roles);
    }
}
```

**注册插件：**
在 `META-INF/services/com.mo.gateway.spi.auth.AuthenticationProvider` 中添加实现类全路径。

## 🐛 问题反馈

**Bug 报告：**
- 描述问题现象
- 提供重现步骤  
- 附上错误日志
- 说明环境信息

**功能建议：**
- 描述需求场景
- 说明期望功能
- 提供实现思路


感谢你的贡献！🎉
