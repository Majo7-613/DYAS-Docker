# Taller de Docker

**Curso:** Diseño y Arquitectura de Software, Universidad de La Sabana (2026)

## Integrantes

| Nombre | Correo institucional |
|--------|----------------------|
| María José Almanza Caviedes | mariaalmca@unisabana.edu.co |

## Propósito

Construir imágenes propias con buenas prácticas, componer una aplicación de varios servicios, escalarla detrás de un balanceador de carga, publicar las imágenes en un registro y contenerizar una aplicación Java / Spring Boot con su base de datos.

## Contenido del Wiki

1. [Imágenes](1-Imagenes): Dockerfile de `friendlyhello` y `docker build --check`.
2. [Caché de capas](2-Cache-de-capas): evidencia de `CACHED` y comparación de tiempos (4,2 s frente a 32,3 s).
3. [Compose](3-Compose): web y Redis con *healthcheck* y `depends_on` con condición.
4. [Balanceo de carga](4-Balanceo-de-carga): 5 réplicas detrás de Traefik.
5. [Registro](5-Registro): imagen publicada en Docker Hub (`majo062013/friendlyhello`), y los dos ejercicios de la Parte 3 resueltos.
6. [Proyecto](6-Proyecto): Spring Boot *multi-stage* con Java 25 y PostgreSQL.
7. [Seguridad](7-Seguridad): escaneo con Trivy y SBOM CycloneDX.
8. [Reflexión final](8-Reflexion).

## Estructura del repositorio

```text
hello-world/                      Paso 1
friendlyhello/                    Pasos 2 a 6 y Parte 3
 ├─ app.py, requirements.txt, .dockerignore, Dockerfile
 ├─ Dockerfile.malorden           anti-ejemplo del Paso 4 (COPY . . antes de pip install)
 ├─ docker-compose.yaml           web + redis
 ├─ docker-compose.traefik.yaml   5 réplicas + redis + traefik
 ├─ docker-compose.hub.yaml       Ejercicio 1: imagen publicada propia
 └─ docker-compose.companero.yaml Ejercicio 2: imagen de un compañero
proyecto/                         Parte 4: API REST de tareas (Spring Boot 4.1.2 + Java 25 + PostgreSQL 18)
evidencias/                       salidas de consola de cada paso
.github/workflows/ci.yml          pruebas -> build -> Trivy -> publicación en GHCR
```

## Entorno de pruebas

- Windows 11 con Docker Desktop (Engine 28.5.1, Compose v2.40.0).
- En este equipo el puerto 4000 lo ocupaba otro proyecto, así que las pruebas se hicieron en el **4001**. Los archivos de Compose aceptan `WEB_PORT` (por defecto 4000), como recomienda la tabla de *Problemas frecuentes* de la guía:

```bash
WEB_PORT=4001 docker compose up -d --build
```
