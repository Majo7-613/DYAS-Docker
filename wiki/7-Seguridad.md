# Seguridad de las imágenes

## Escaneo de vulnerabilidades con Trivy

```bash
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy:0.76.0 image --timeout 20m --severity HIGH,CRITICAL proyecto:dev
```

> En Windows, el primer escaneo de `proyecto:dev` falló con `context deadline exceeded`: el límite por defecto de Trivy es 5 minutos y el análisis de las capas tardó más. Se resolvió con `--timeout 20m`.

### Resultados (base de datos de Trivy del 24 de octubre de 2026)

| Imagen | Sistema base | Hallazgos en el SO | Hallazgos en la aplicación |
|--------|--------------|-------------------:|---------------------------:|
| `proyecto:dev` | Ubuntu 24.04 (Temurin 25.0.4.1) | **0** | **0** en `app.jar` (Spring Boot 4.1.2) |
| `friendlyhello` | Debian 13.7 (`python:3.14-slim`) | 44 HIGH, 0 CRITICAL | 4 HIGH (en pip, no en la aplicación) |
| `friendlyhello` sin pip | Debian 13.7 | 44 HIGH, 0 CRITICAL | **0** |

```text
proyecto:dev (ubuntu 24.04) │ ubuntu │ 0
app/app.jar                 │  jar   │ 0
```

### Análisis

**`proyecto:dev` sale limpia.** La guía reporta que un Spring Boot 4.1.1 recién generado tenía 8 hallazgos (Tomcat 11.0.24 y Jackson 3.1.5). Con **Spring Boot 4.1.2**, que trae Tomcat 11.0.27 y Jackson 3.1.7, esos hallazgos ya están corregidos. El sistema base también sale limpio porque se actualizó a la etiqueta **`25.0.4.1_1`** en lugar de la `25.0.1_8`, que tenía casi un año. Este resultado es una foto del día: mañana puede publicarse un CVE nuevo. Por eso el escaneo es un paso del pipeline y no una revisión única.

**`friendlyhello`: los 44 hallazgos de Debian no tienen corrección publicada.** 43 están en estado `affected` y 1 en `fix_deferred` (`util-linux`, `ncurses`, `systemd`, `perl-base`, `acl`…). Actualizar la imagen hoy no los elimina. Las opciones serían:

- Aceptar el riesgo y documentarlo. La mayoría son paquetes de administración del sistema (`mount`, `login`, `nsenter`) que la aplicación no ejecuta y que, sin `root` (`USER app`), son difíciles de explotar.
- Cambiar a una base más mínima (*distroless* o Alpine), que no incluye esos paquetes.
- Revisar periódicamente con Dependabot o Renovate para tomar la corrección cuando salga.

**Los 4 hallazgos de Python vienen de pip, no de la aplicación.** Están en las librerías que pip trae empaquetadas en `pip/_vendor`:

| Paquete | Versión | Corrección | Origen |
|---------|---------|------------|--------|
| `msgpack` | 1.1.2 | 1.2.1 | `pip/_vendor/msgpack` |
| `urllib3` (2 CVE) | 2.7.0 | 2.8.0 | `pip/_vendor/urllib3` |
| `setuptools` | 70.3.0 | 78.1.1 | `pip/_vendor/pkg_resources` |

Las dependencias reales (Flask, Werkzeug, Jinja2, redis, gunicorn…) tienen **0 hallazgos**. pip solo se necesita **al construir**, no al ejecutar. Se probó desinstalarlo en la misma capa:

```dockerfile
RUN pip install --no-cache-dir -r requirements.txt && pip uninstall -y pip
```

Con eso los hallazgos de Python bajan de **4 a 0** y la aplicación sigue respondiendo (`/health` → `OK`). Es la misma idea del *multi-stage*: **lo que solo sirve para construir no debe llegar a producción**. El Dockerfile entregado se dejó como lo pide la guía, para que coincida con las evidencias de los Pasos 3 y 4. La mitigación queda medida en [`evidencias/09-trivy-friendlyhello-sinpip.txt`](../blob/main/evidencias/09-trivy-friendlyhello-sinpip.txt).

## Lista de materiales (SBOM)

```bash
MSYS_NO_PATHCONV=1 docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v "$(pwd -W):/out" \
  aquasec/trivy:0.76.0 image --format cyclonedx --output /out/sbom-proyecto.cdx.json proyecto:dev
```

[`evidencias/sbom-proyecto.cdx.json`](../blob/main/evidencias/sbom-proyecto.cdx.json) (formato CycloneDX) inventaría **165 componentes**: el sistema operativo y 164 librerías. Por ejemplo:

| Componente | Versión |
|------------|---------|
| `org.apache.tomcat.embed:tomcat-embed-core` | 11.0.27 |
| `tools.jackson.core:jackson-databind` | 3.1.7 |
| `org.hibernate:hibernate-core` | 7.4.13.Final |
| `org.postgresql:postgresql` | 42.7.14 |

**Para qué sirve:** si mañana se publica una vulnerabilidad en, por ejemplo, `tomcat-embed-core 11.0.27`, basta con buscar en el SBOM para saber en minutos si la imagen desplegada está afectada, sin reconstruir ni volver a escanear.

## Integración en el pipeline

El job `imagenes` de [`.github/workflows/ci.yml`](../blob/main/.github/workflows/ci.yml) ejecuta Trivy sobre `proyecto:ci` en cada *push*. Hoy usa `--exit-code 0`, que solo reporta. El siguiente paso de endurecimiento es usar `--exit-code 1` con `--severity CRITICAL`, para que **una vulnerabilidad crítica bloquee la publicación**, y agregar la firma de la imagen con cosign, como propone el taller de DevSecOps.

## Prácticas de seguridad aplicadas

| Práctica | `friendlyhello` | `proyecto` |
|----------|:---------------:|:----------:|
| Usuario sin privilegios (`uid=999(app)`) | ✅ | ✅ |
| Sin herramientas de compilación en la imagen final | parcial (pip; mitigación medida) | ✅ *multi-stage*: sin `mvn` ni `javac` |
| Imagen base con versión fija | ✅ `python:3.14-slim` | ✅ `25.0.4.1_1-jre-noble` |
| Sin secretos en la imagen | ✅ | ✅ `.env` en `.dockerignore` y `.gitignore` |
| Base de datos sin puertos publicados | ✅ Redis | ✅ PostgreSQL |
| Escaneo de vulnerabilidades | ✅ | ✅ también en CI |
| SBOM | – | ✅ CycloneDX |
