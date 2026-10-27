# Publicación en un registro (Parte 3)

## Docker Hub

**Imagen publicada:** https://hub.docker.com/r/majo062013/friendlyhello

```bash
docker login
docker build -t majo062013/friendlyhello:0.1.0 -t majo062013/friendlyhello:latest friendlyhello
docker push majo062013/friendlyhello:0.1.0
docker push majo062013/friendlyhello:latest
```

Las dos etiquetas quedaron publicadas con el mismo *digest*:

```text
Docker Hub: latest  sha256:4a6d8d5ea5d4…
Docker Hub: 0.1.0   sha256:4a6d8d5ea5d4…
```

Salida: [`evidencias/10-dockerhub-tags.txt`](../blob/main/evidencias/10-dockerhub-tags.txt) (consulta a la API de Docker Hub).

![Repositorio en Docker Hub con las etiquetas 0.1.0 y latest](images/10-dockerhub.png)

En la página de Docker Hub el *digest* que aparece en cada etiqueta (`75f003bf8216`) es el del manifiesto de la plataforma `linux/amd64`; el `4a6d8d5ea5d4` de la terminal es el del índice que lo agrupa. Las dos etiquetas comparten ambos.

### Por qué dos etiquetas

`0.1.0` y `latest` apuntan al **mismo *digest*** en este momento. La diferencia está en el tiempo:

- `0.1.0` es **inmutable** por convención: quien la use hoy o dentro de un año obtiene la misma imagen. Es la que se pone en un despliegue o en un archivo de Compose.
- `latest` se mueve con cada versión. Con la `0.2.0`, `latest` apuntará a la nueva y `0.1.0` seguirá disponible para hacer *rollback*.

Por eso el Ejercicio 1 usa `:0.1.0` y no `:latest`.

## GitHub Container Registry (GHCR)

El pipeline [`.github/workflows/ci.yml`](../blob/main/.github/workflows/ci.yml) publica las imágenes `friendlyhello` y `proyecto` en GHCR en cada *push* a `main`. Usa el `GITHUB_TOKEN` de la ejecución con permiso `packages: write`, sin tokens personales, y las etiqueta con el SHA del commit y con `latest`:

```text
ghcr.io/majo7-613/friendlyhello:<sha>
ghcr.io/majo7-613/proyecto:<sha>
```

El paso *Publicar en GHCR* terminó con éxito en cada ejecución del pipeline sobre `main` (pestaña [Actions](../actions)). GHCR crea los paquetes como **privados**: aparecen en la pestaña *Packages* del perfil y, para que otra persona los descargue sin credenciales, hay que cambiarlos a públicos.

## Ejercicio 1: usar la imagen publicada

[`friendlyhello/docker-compose.hub.yaml`](../blob/main/friendlyhello/docker-compose.hub.yaml) reemplaza `build: .` por la imagen del registro:

```yaml
services:
  web:
    image: ${DOCKERHUB_USER:?Defina DOCKERHUB_USER con su usuario de Docker Hub}/friendlyhello:0.1.0
```

```bash
docker rmi majo062013/friendlyhello:0.1.0 majo062013/friendlyhello:latest   # forzar la descarga desde Docker Hub
DOCKERHUB_USER=majo062013 WEB_PORT=4001 docker compose -f docker-compose.hub.yaml up -d
```

```text
== docker images majo062013/friendlyhello (antes: no existe localmente)
REPOSITORY   TAG       IMAGE ID   CREATED   SIZE

 web Pulling
 c3d376f707e4 Already exists
 ...
 web Pulled
 Container friendlyhello-redis-1  Healthy
 Container friendlyhello-web-1  Started

NAME                    IMAGE                            STATUS                    PORTS
friendlyhello-redis-1   redis:8.8                        Up 19 seconds (healthy)   6379/tcp
friendlyhello-web-1     majo062013/friendlyhello:0.1.0   Up 12 seconds (healthy)   0.0.0.0:4001->8000/tcp

Hola World!  Hostname: 271bde3ad9d1  Visits: 28
Hola World!  Hostname: 271bde3ad9d1  Visits: 29
```

Compose descargó la imagen desde Docker Hub (`web Pulled`). Las capas aparecen como `Already exists` porque son las mismas de `python:3.14-slim`, que ya estaba en el equipo: Docker descarga **solo las capas que no tiene**, la misma idea de la caché del Paso 4. El contador siguió en 28 porque el volumen `./data` es el mismo de los pasos anteriores.


Salida completa: [`evidencias/11-ejercicio1.txt`](../blob/main/evidencias/11-ejercicio1.txt).

## Ejercicio 2: usar la imagen de un compañero

[`friendlyhello/docker-compose.companero.yaml`](../blob/main/friendlyhello/docker-compose.companero.yaml):

Se usó la imagen publicada por una compañera del curso, [`amyjean/friendlyhello`](https://hub.docker.com/r/amyjean/friendlyhello), que también tiene las etiquetas `0.1.0` y `latest`.

```bash
COMPANERO_IMAGE=amyjean/friendlyhello:0.1.0 WEB_PORT=4001 docker compose -f docker-compose.companero.yaml up -d
```

Antes de ejecutar una imagen de otra persona se revisó su configuración con `docker inspect` y `docker history`. Es la aplicación del taller: puerto 8000, usuario `app`, gunicorn y el mismo `HEALTHCHECK`.

```text
== docker images amyjean/friendlyhello (antes: no existe localmente)
REPOSITORY   TAG       IMAGE ID   CREATED   SIZE

 web Pulling
 ...
 web Pulled
 Container friendlyhello-redis-1  Healthy
 Container friendlyhello-web-1  Started

NAME                    IMAGE                         STATUS                    PORTS
friendlyhello-redis-1   redis:8.8                     Up 10 seconds (healthy)   6379/tcp
friendlyhello-web-1     amyjean/friendlyhello:0.1.0   Up 3 seconds (healthy)    0.0.0.0:4001->8000/tcp

$ docker exec friendlyhello-web-1 id
uid=999(app) gid=999(app) groups=999(app)

Hello World!  Hostname: a584c7c7abc8  Visits: 71
Hello World!  Hostname: a584c7c7abc8  Visits: 72
```

![Ejercicio 2: la aplicación con la imagen de la compañera](images/12-ejercicio2.png)

Dos detalles muestran que se está ejecutando **su** imagen y no la propia:

- Dice **"Hello"**, mientras que la imagen propia dice "Hola" por el cambio del Paso 4. El código viene dentro de su imagen.
- Esta vez Docker sí **descargó** capas (`Pull complete`), porque su imagen se construyó sobre otra versión de `python:3.14-slim`: la suya trae Python 3.14.9 y la propia 3.14.10. Es la etiqueta de versión menor de la que se habla en [Imágenes](1-Imagenes) moviéndose con los parches.

El contador sigue en 71 porque, aunque la imagen es otra, el Redis y el volumen `./data` son los propios. La aplicación de la compañera usa los datos locales sin saber nada de ellos: **la imagen trae el código, pero los datos los pone el entorno donde se ejecuta**.

Este es el valor de un registro: se ejecutó la aplicación de otra persona **sin tener su código ni construir nada**, solo con el nombre de la imagen. Es lo mismo que hace un servidor de despliegue con la imagen que publica el pipeline.

Salida completa: [`evidencias/12-ejercicio2.txt`](../blob/main/evidencias/12-ejercicio2.txt).
