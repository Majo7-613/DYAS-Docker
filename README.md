# Taller de Docker: de una imagen a un sistema con CI

**María José Almanza Caviedes**, mariaalmca@unisabana.edu.co
Diseño y Arquitectura de Software, Ingeniería Informática, Universidad de La Sabana (2026)

📘 **Documentación completa con evidencias y capturas:** [Wiki del repositorio](https://github.com/Majo7-613/DYAS-Docker/wiki)
🐳 **Imagen publicada:** [`majo062013/friendlyhello`](https://hub.docker.com/r/majo062013/friendlyhello) (etiquetas `0.1.0` y `latest`)

---

## Qué hay en este repositorio

En este taller contenericé dos aplicaciones aplicando las prácticas que exige una imagen de producción:

1. **`friendlyhello`**: una aplicación Flask con un contador en Redis. Primero la empaqueté en una imagen, después la orquesté con Docker Compose y por último la escalé a **5 réplicas** detrás del balanceador **Traefik**.
2. **`proyecto`**: una API REST de tareas en **Spring Boot 4.1.2 y Java 25** con **PostgreSQL 18**. Tiene Dockerfile *multi-stage*, configuración por variables de entorno y un pipeline de CI que prueba, construye, escanea y publica las imágenes.

```text
hello-world/                      Primer Dockerfile (busybox)
friendlyhello/
 ├─ app.py, requirements.txt      Aplicación Flask + Redis, dependencias con versión fija
 ├─ Dockerfile, .dockerignore     Imagen con usuario sin privilegios y HEALTHCHECK
 ├─ Dockerfile.malorden           Anti-ejemplo para medir el efecto del orden de las capas
 ├─ docker-compose.yaml           web + Redis con healthcheck y depends_on con condición
 ├─ docker-compose.traefik.yaml   5 réplicas + Redis + Traefik
 ├─ docker-compose.hub.yaml       La aplicación usando la imagen publicada en Docker Hub
 └─ docker-compose.companero.yaml La aplicación usando la imagen de un compañero
proyecto/                         API Spring Boot + PostgreSQL (Dockerfile multi-stage)
evidencias/                       Salidas de consola de cada prueba, reportes de Trivy y SBOM
wiki/                             Fuente de las páginas del Wiki
.github/workflows/ci.yml          Pruebas → build → escaneo con Trivy → publicación en GHCR
integrantes.txt
```

## Resultados principales

| Qué se probó | Resultado |
|--------------|-----------|
| Orden de las capas del Dockerfile | Tras cambiar el código, **4,2 s** con el orden correcto frente a **32,3 s** con el incorrecto: `pip install` se repite y cuesta unas 7,5 veces más |
| Contenedores | `healthy` y ejecutándose como `uid=999(app)`, nunca como `root` |
| Compose | El contador sobrevive a `docker compose down` gracias al volumen |
| Balanceo | 20 peticiones repartidas **4-4-4-4-4** entre las 5 réplicas |
| Imagen Java *multi-stage* | Sin `mvn` ni `javac` en la imagen final |
| Escaneo con Trivy | **0** vulnerabilidades HIGH/CRITICAL en la imagen del proyecto |
| CI | Las imágenes solo se construyen si pasan las pruebas (`needs: pruebas`) |

### Un problema real que encontré

Al pasar el proyecto de Java 21 a **Java 25**, la aplicación seguía respondiendo, pero Docker marcaba el contenedor como **`unhealthy`**. La causa: las imágenes de **Temurin 25 ya no traen `curl`**, y el `HEALTHCHECK` lo necesita. Lo resolví instalando `curl` en la imagen final y actualizando la base a `25.0.4.1_1`. La lección es que, al cambiar la imagen base, hay que verificar que el contenedor llegue a `healthy`, no solo que la aplicación responda. El detalle está en el [Wiki → Proyecto](https://github.com/Majo7-613/DYAS-Docker/wiki/6-Proyecto).

## Cómo ejecutarlo

Requisitos: Docker Desktop (o Docker Engine) con Docker Compose v2.

**Aplicación con Redis**

```bash
cd friendlyhello
docker compose up -d --build          # http://localhost:4000
```

**Con 5 réplicas y balanceador**

```bash
cd friendlyhello
docker compose -f docker-compose.traefik.yaml up -d --build --scale web=5
# Aplicación: http://localhost:4000   ·   Dashboard de Traefik: http://localhost:8080
```

> Si el puerto 4000 está ocupado: `WEB_PORT=4001 docker compose ...`

**Proyecto Java con PostgreSQL**

```bash
cd proyecto
cp .env.example .env                  # credenciales locales, fuera del repositorio
docker compose up -d --build          # http://localhost:8080/tareas
```

> Si el puerto 8080 está ocupado: `APP_PORT=8081 docker compose ...`

Para detener cualquiera de ellos: `docker compose down` (con el mismo `-f` si se usó uno).

## Documentación

| Página del Wiki | Contenido |
|-----------------|-----------|
| [Imágenes](https://github.com/Majo7-613/DYAS-Docker/wiki/1-Imagenes) | Dockerfile, buenas prácticas y `docker build --check` |
| [Caché de capas](https://github.com/Majo7-613/DYAS-Docker/wiki/2-Cache-de-capas) | Experimento y comparación de tiempos |
| [Compose](https://github.com/Majo7-613/DYAS-Docker/wiki/3-Compose) | Servicios, *healthchecks* y persistencia |
| [Balanceo de carga](https://github.com/Majo7-613/DYAS-Docker/wiki/4-Balanceo-de-carga) | Réplicas, Traefik y sus límites |
| [Registro](https://github.com/Majo7-613/DYAS-Docker/wiki/5-Registro) | Docker Hub, GHCR y ejercicios |
| [Proyecto](https://github.com/Majo7-613/DYAS-Docker/wiki/6-Proyecto) | Spring Boot *multi-stage*, Compose y CI |
| [Seguridad](https://github.com/Majo7-613/DYAS-Docker/wiki/7-Seguridad) | Trivy, mitigaciones medidas y SBOM |
| [Reflexión final](https://github.com/Majo7-613/DYAS-Docker/wiki/8-Reflexion) | Análisis y aprendizajes |

## Créditos

Este trabajo desarrolla el [Taller de Docker](https://github.com/CesarAVegaF312/DYAS-Docker) del profesor **César Augusto Vega Fernández** (Universidad de La Sabana), cuyas Partes 1 a 3 se basan en el [Taller de Docker](https://aulasoftwarelibre.github.io/taller-de-docker/) de **Aula de Software Libre** (Universidad de Córdoba). El enunciado original se puede consultar en el historial de este repositorio o en el repositorio del profesor.

Se distribuye, como el material original, bajo la licencia [Creative Commons Atribución-NoComercial-CompartirIgual 4.0 Internacional (CC BY-NC-SA 4.0)](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.es).
