FROM python:3.11-slim-bookworm

# System dependency for pdf2image (poppler)
RUN apt-get update && apt-get install -y \
    poppler-utils \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/ ./app/

EXPOSE 8090

# $PORT injected by Render.com at runtime; falls back to 8090 for local Docker
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8090}"]
