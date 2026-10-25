# Reflexión final

## 1. ¿Qué diferencia hay entre una imagen y un contenedor? ¿Y entre un volumen y la capa de escritura del contenedor?

Una **imagen** es una pila de capas de **solo lectura**, identificada por su *digest*: es la "clase". Un **contenedor** es una "instancia": un proceso aislado del sistema anfitrión (con *namespaces* y *cgroups*) que monta esas capas y les agrega encima **una capa de escritura propia**. En el Paso 6 se vio claramente: una sola imagen, `friendlyhello-web`, dio lugar a 5 contenedores, cada uno con su *hostname* y su proceso, y ninguno modificó la imagen.

La **capa de escritura** nace y muere con el contenedor. Lo que se escribe ahí desaparece con `docker rm` o `docker compose down`, y además es lenta para mucha escritura, porque usa *copy-on-write* sobre el sistema de archivos de capas. Un **volumen** vive fuera del ciclo de vida del contenedor. En el Paso 5, después de `docker compose down`, que borró el contenedor de Redis, el contador siguió en 6 en un contenedor nuevo porque los datos estaban en `./data`. En el proyecto pasa lo mismo con PostgreSQL: el volumen `datos-db` conserva las tareas entre reinicios y solo `docker compose down -v` las borra.

**Regla práctica:** el código y las dependencias van en la imagen, los datos van en volúmenes y la capa de escritura se trata como desechable. Por eso los contenedores se pueden destruir y recrear sin miedo (`--rm`, `--scale`).

## 2. ¿Por qué el orden de las instrucciones del Dockerfile afecta el tiempo de construcción?

Cada instrucción genera una capa cuya clave de caché depende de la instrucción, de la capa anterior y, en `COPY`, del *checksum* de los archivos. Si una capa cambia, **todas las siguientes se invalidan**, aunque su instrucción no haya cambiado. Por eso lo que cambia poco (dependencias) va arriba y lo que cambia mucho (código) va abajo.

Lo medimos: tras el mismo cambio en `app.py`, el orden correcto tardó **4,2 s** (con `pip install` en `CACHED`) y el orden incorrecto **32,3 s**, de los cuales 24,1 s fueron reinstalar las dependencias: unas **7,5 veces más**. En el proyecto Java el efecto es mayor, porque descargar las dependencias de Spring Boot es lo más lento. Por eso el Dockerfile copia primero el `pom.xml`, ejecuta `dependency:go-offline` y solo después copia `src/`, y además usa `--mount=type=cache` para conservar `~/.m2` entre *builds*. En CI, donde cada *push* reconstruye la imagen, esto define si el pipeline tarda un minuto o diez.

## 3. ¿Por qué 5 réplicas en una sola máquina no dan alta disponibilidad?

Porque la alta disponibilidad exige **eliminar los puntos únicos de falla**, y en la demostración todos siguen ahí:

- **El equipo físico y el *daemon* de Docker:** si se reinicia el portátil o se cae Docker Desktop, caen las 5 réplicas a la vez.
- **Traefik:** hay una sola instancia. Si falla, nadie recibe tráfico, aunque las réplicas estén sanas.
- **Redis:** hay un solo nodo. Si cae, las 5 réplicas siguen respondiendo, pero sin contador. El estado compartido es otro punto único.
- **Recursos compartidos:** las 5 réplicas compiten por la misma CPU, memoria y disco. Un pico de carga o una fuga de memoria en una afecta a las demás.

Lo que sí dan las réplicas es **escalabilidad horizontal** y tolerancia a fallas de **un proceso**: si un *worker* se cuelga y su *healthcheck* falla, Traefik deja de enviarle tráfico. Para alta disponibilidad real hacen falta réplicas en **varios nodos**, ojalá en distintas zonas, con un orquestador (Kubernetes) que las reprograme cuando un nodo cae, un balanceador redundante y una base de datos replicada. Que el diseño sea **sin estado** (todo el estado en Redis o PostgreSQL) es lo que hace posible ese salto, y es la decisión de arquitectura que hay que tomar desde ya en el proyecto.

## 4. ¿Qué riesgos de seguridad tiene una imagen que corre como `root` o que incluye herramientas de compilación?

**Correr como `root`:**

- Si un atacante explota una vulnerabilidad de la aplicación (por ejemplo, una ejecución remota de código en una librería), obtiene **root dentro del contenedor**: puede instalar herramientas, modificar binarios y leer todo el sistema de archivos.
- El contenedor comparte el kernel del anfitrión. Con una vulnerabilidad de escape del contenedor, una configuración débil (`--privileged`, `docker.sock` montado, volúmenes del *host*) o el UID 0 sin *user namespaces*, ese root puede convertirse en **root del servidor**.
- Por eso las dos imágenes del taller usan `USER app`. Se verificó con `uid=999(app)` en `friendlyhello` y `uid=999(app)` en el proyecto Java.

**Incluir herramientas de compilación (JDK, Maven, gcc, pip, git):**

- **Más superficie de ataque:** cada paquete extra puede tener CVE. Más paquetes significa más hallazgos en Trivy y más parches que mantener.
- **Más capacidades para el atacante:** con un compilador o un gestor de paquetes dentro, un intruso puede descargar y compilar sus propias herramientas (*living off the land*).
- **Fuga de información:** el código fuente, el `~/.m2` o archivos de configuración del *build* terminan en producción.
- **Imágenes más grandes:** más lentas de descargar y desplegar.

El Dockerfile *multi-stage* del proyecto resuelve esto: la etapa `build` tiene Maven y el JDK, y la imagen final solo tiene el JRE y el `.jar` (ver [Proyecto](6-Proyecto)).

## Conexión con el proyecto

Lo aprendido se traduce directamente en el checklist del Proyecto 3:

| Requisito | Dónde quedó resuelto |
|-----------|----------------------|
| Dockerfile *multi-stage*, versión fija, usuario sin privilegios | `proyecto/Dockerfile` |
| `.dockerignore` | `proyecto/.dockerignore` (excluye `target/`, `.env`, `.git`) |
| `docker compose up` deja el sistema funcionando | `proyecto/docker-compose.yml` (app + PostgreSQL con `service_healthy`) |
| Configuración por variables de entorno, sin secretos | `SPRING_DATASOURCE_*` + `.env.example`, con `.env` en `.gitignore` |
| *Healthcheck* usado por Compose y por el despliegue | `/actuator/health/readiness` |
| Imagen construida en CI | `.github/workflows/ci.yml`: el job `imagenes` tiene `needs: pruebas` |

**Aprendizajes principales:**

1. **Reproducibilidad** significa versiones fijas en todo, desde la imagen base hasta Trivy. Una etiqueta como `latest` o `21-jre` hace que el mismo Dockerfile produzca imágenes distintas según el día.
2. **El *healthcheck* es parte del contrato** entre la aplicación y la infraestructura: Compose lo usa para el orden de arranque y Traefik para decidir a qué réplica enviar tráfico. Debe verificar **la propia aplicación** y no sus dependencias. Por eso se usa `readiness` y no `/actuator/health`.
3. **La seguridad de una imagen es una foto del día, no una propiedad fija:** la guía reporta 8 hallazgos en Spring Boot 4.1.1, y nuestra imagen con 4.1.2 y Temurin 25.0.4.1 sale con 0. En cambio, `python:3.14-slim` tiene 44 hallazgos de Debian sin corrección publicada, y 4 más vienen de pip, una herramienta que solo se necesita al construir (ver [Seguridad](7-Seguridad)). Hay que escanear en cada *build* y tener un SBOM para reaccionar cuando aparezca un CVE nuevo.
4. **El diseño sin estado** es lo que permite escalar. Es una decisión de arquitectura, no de infraestructura.
5. **Cambiar la imagen base puede romper cosas invisibles:** al pasar de Java 21 a Java 25 la aplicación seguía respondiendo, pero el contenedor quedó `unhealthy` porque Temurin 25 ya no trae `curl` (ver [Proyecto](6-Proyecto#problema-encontrado-unhealthy-al-pasar-a-java-25)). Sin el *healthcheck*, un despliegue habría enviado tráfico a una réplica que la infraestructura no podía vigilar.
