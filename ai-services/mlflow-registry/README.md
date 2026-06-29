# MLflow Model Registry

> Solra AI 模型注册、版本管理与评测平台

## 访问

| 服务 | 地址 |
|------|------|
| MLflow UI | http://localhost:8104 |
| MinIO Console | http://localhost:9001 |

## 启动

```bash
cd ai-services/mlflow-registry
docker compose up -d
```

## 注册模型

```python
import mlflow

mlflow.set_tracking_uri("http://localhost:8104")

with mlflow.start_run(run_name="cosyvoice-2-v1.0.0"):
    mlflow.log_param("model_name", "cosyvoice-2")
    mlflow.log_param("framework", "pytorch")
    mlflow.log_param("quantization", "fp16")
    mlflow.log_metric("mos", 4.3)
    mlflow.log_metric("rtf", 0.15)
    mlflow.log_artifact("model_weights/")

    mlflow.register_model(
        model_uri=f"runs:/{mlflow.active_run().info.run_id}/model",
        name="cosyvoice-2"
    )
```

## 模型阶段管理

```bash
# 将 v1 提升到 Production
mlflow models transition-stage --name cosyvoice-2 --version 1 --stage Production

# 归档旧版本
mlflow models transition-stage --name cosyvoice-2 --version 1 --stage Archived
```
