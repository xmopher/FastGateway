# 安全策略

## 支持版本

当前只有1.0.0版本，后续会迭代

## 发现安全问题怎么办？

**xmopher@hotmail.com

我会在 24 小时内回复

## 安全配置

### 必须要改的
```yaml
# 别用默认密钥
gateway:
  auth:
    jwt:
      secret: ${JWT_SECRET}  # 用环境变量

# 一定要用 HTTPS
server:
  ssl:
    enabled: true

# 别暴露太多管理接口
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### 上线前检查
- [ ] 改掉默认密码
- [ ] 开启 HTTPS  
- [ ] Redis 加密码
- [ ] 设置访问限制
- [ ] 别把管理端口暴露出去

有问题就发邮件：xmopher@hotmail.com

谢谢帮忙找bug！
