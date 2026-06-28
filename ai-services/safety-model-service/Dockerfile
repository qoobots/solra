FROM python:3.11-slim

LABEL org.opencontainers.image.title="Solra Safety Model Service"
LABEL org.opencontainers.image.description="Content safety classification and filtering"

WORKDIR /app
COPY pyproject.toml poetry.lock* ./
RUN pip install --no-cache-dir poetry && poetry config virtualenvs.create false && poetry install --no-dev
COPY src/ ./src/
COPY models/ ./models/

EXPOSE 9094
ENTRYPOINT ["uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "9094"]
