FROM solra/python-base:3.11
LABEL service="tts-service"
WORKDIR /app
COPY pyproject.toml ./
RUN pip install poetry && poetry install --no-dev
COPY src/ src/
EXPOSE 8103
CMD ["python", "src/main.py"]
