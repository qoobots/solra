FROM python:3.11-slim

LABEL org.opencontainers.image.title="Solra LLM Router"
LABEL org.opencontainers.image.description="Intelligent LLM routing and scheduling"

WORKDIR /app

COPY pyproject.toml poetry.lock* ./
RUN pip install --no-cache-dir poetry && poetry config virtualenvs.create false && poetry install --no-dev

COPY src/ ./src/

EXPOSE 9091

ENTRYPOINT ["uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "9091"]
