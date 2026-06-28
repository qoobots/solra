FROM solra/python-base:3.11
LABEL service="embedding-service"
WORKDIR /app
COPY pyproject.toml ./
RUN pip install poetry && poetry install --no-dev
COPY src/ src/
EXPOSE 8101
CMD ["python", "src/main.py"]
