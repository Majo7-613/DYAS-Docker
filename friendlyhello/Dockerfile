# Imagen base oficial de Python, con versión fija
FROM python:3.14-slim

# Directorio de trabajo dentro de la imagen
WORKDIR /app

# Primero solo las dependencias: esta capa se reutiliza mientras requirements.txt no cambie
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Después el código de la aplicación
COPY . .

# Usuario sin privilegios para ejecutar la aplicación
RUN useradd --system --no-create-home app
USER app

# Variable de entorno y puerto de la aplicación
ENV NAME=World
EXPOSE 8000

# Docker marca el contenedor como "unhealthy" si la aplicación deja de responder
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --start-interval=2s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health', timeout=2)"

# Servidor de producción (gunicorn) en lugar del servidor de desarrollo de Flask
CMD ["gunicorn", "--bind", "0.0.0.0:8000", "--workers", "2", "app:app"]
