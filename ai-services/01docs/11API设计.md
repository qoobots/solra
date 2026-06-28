# AI服务层（AI-Services）API设计

> ai-services/ REST API设计——OpenAI兼容接口、安全检测、嵌入

---

## 一、API设计原则

| 原则 | 说明 |
|------|------|
| OpenAI兼容 | Chat API兼容OpenAI格式 |
| 统一错误 | 所有端点统一响应格式 |
| 异步优先 | 长任务返回 task_id + 轮询 |
| 流式支持 | SSE格式推送Token |

---

## 二、核心API

### 2.1 对话推理 (OpenAI兼容)

```
POST /v1/chat/completions
{
  "model": "qwen2.5-7b",
  "messages": [{"role":"user","content":"你好"}],
  "stream": true,
  "temperature": 0.7
}
```

### 2.2 安全检测

```
POST /v1/safety/text
{
  "text": "用户输入内容",
  "categories": ["porn","violence","politics"]
}
→ { "safe": true, "scores": {"porn":0.01,...} }
```

### 2.3 文本嵌入

```
POST /v1/embeddings
{
  "texts": ["文本1","文本2"],
  "model": "text2vec-large-chinese"
}
→ { "embeddings": [[0.1,...],[0.2,...]] }
```

### 2.4 语音合成

```
POST /v1/tts
{
  "text": "你好欢迎来到索拉",
  "emotion": "happy",
  "format": "wav"
}
→ { "audio_url": "https://cdn/xxx.wav" }
```
