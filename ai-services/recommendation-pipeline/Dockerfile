FROM solra/python-base:3.11
LABEL service="recommendation-pipeline"
WORKDIR /app
COPY pyproject.toml ./
RUN pip install poetry && poetry install --no-dev
COPY src/ src/
EXPOSE 8102
CMD ["python", "src/main.py"]
