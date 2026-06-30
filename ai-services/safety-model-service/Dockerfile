FROM python:3.11-slim

LABEL org.opencontainers.image.title="Solra Safety Model Service"
LABEL org.opencontainers.image.description="Content safety classification and filtering"

RUN addgroup --system solra && adduser --system --group solra

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

COPY pyproject.toml poetry.lock* ./ 
RUN pip install --no-cache-dir poetry && poetry config virtualenvs.create false && poetry install --no-dev
COPY src/ ./src/
COPY models/ ./models/

RUN chown -R solra:solra /app
USER solra

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:9090/health')" || exit 1

EXPOSE 9090
ENTRYPOINT ["uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "9090"]
