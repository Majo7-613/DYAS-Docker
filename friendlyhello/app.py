from flask import Flask
from redis import Redis, RedisError
from redis.backoff import NoBackoff
from redis.retry import Retry
import os
import socket

# Conexión a Redis: el nombre "redis" lo resuelve la red de Docker Compose.
# Timeouts cortos y sin reintentos para que la página responda aunque Redis no exista.
redis = Redis(host="redis", db=0,
              socket_connect_timeout=2, socket_timeout=2,
              retry=Retry(NoBackoff(), 0))

app = Flask(__name__)

@app.route("/")
def hello():
    try:
        visits = redis.incr("counter")
    except RedisError:
        visits = "<i>cannot connect to Redis, counter disabled</i>"

    html = "<h3>Hola {name}!</h3>" \
           "<b>Hostname:</b> {hostname}<br/>" \
           "<b>Visits:</b> {visits}"
    return html.format(name=os.getenv("NAME", "world"), hostname=socket.gethostname(), visits=visits)


@app.route("/health")
def health():
    # Lo usa el HEALTHCHECK del Dockerfile: no depende de Redis
    return "OK"
