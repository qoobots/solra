# 03_Docker与K8s容器部署说明

> **文档编号**：06-03 | **版本号**：v1.0 | **编制人**：运维组 | **状态**：已发布 | **更新日期**：2026-06-30

---

## 1. Docker镜像规范

| 规范 | 说明 |
|------|------|
| 基础镜像 | eclipse-temurin:17-jre (Java) / python:3.11-slim / nginx:alpine |
| 多架构 | amd64 + arm64 |
| 非root用户 | USER 1000:1000 |
| 健康检查 | HEALTHCHECK指令 |
| 标签 | git commit SHA |

### 示例 Dockerfile (Java)

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/*.jar app.jar
USER 1000:1000
EXPOSE 8080
HEALTHCHECK --interval=30s CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 2. K8s资源配置

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
      - name: auth-service
        resources:
          requests: { cpu: "500m", memory: "512Mi" }
          limits: { cpu: "2000m", memory: "2Gi" }
        livenessProbe:
          httpGet: { path: /actuator/health, port: 8080 }
          initialDelaySeconds: 30
        readinessProbe:
          httpGet: { path: /actuator/health/readiness, port: 8080 }
          initialDelaySeconds: 10
```

## 3. HPA配置

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target: { type: Utilization, averageUtilization: 70 }
```
