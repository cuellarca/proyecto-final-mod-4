# ms-logistic-delivery

Microservicio del **Bounded Context de Logística de Entrega** del caso **Nur-tricenter** (Proyecto
Final del Diplomado de Microservicios). Construido con **DDD + arquitectura hexagonal + CQRS +
Outbox transaccional**, sobre Java 21 y Spring Boot 3.5.

> Proyecto **académico**: los colaboradores externos (proveedor de mapas, productor de
> `PaquetesListosParaEntrega`, `ms-notificaciones`, `ms-catering-suscripcion`) son **stubs**; el
> object storage está simulado (se guarda la **URL** de la evidencia, no el binario); no hay auth
> real.

Todos los comandos de este documento se ejecutan **desde esta carpeta**
(`proyecto-final-mod-4/ms-logistic-delivery`).

| Documento | Qué contiene |
| :--- | :--- |
| `README.md` (este) | Levantar, probar, configurar y publicar el servicio |
| [`testing-rules.md`](testing-rules.md) | Contrato único de pruebas: qué se prueba, qué se dobla, qué está prohibido |
| [`AGENTS.md`](AGENTS.md) | Cómo debe trabajar un agente de IA sobre este repositorio |
| [`postman/README.md`](postman/README.md) | Colección Postman del flujo HU-1…HU-6 y de los casos de error |

---

## Arquitectura

Multi-módulo Maven, un módulo por capa. La dirección de dependencias
`domain ← application ← infrastructure ← bootstrap` la **fuerza el build**.

```
domain/          jar puro (sin framework): agregados, VOs, servicios de dominio, eventos,
                 puertos de repositorio e hidratación.
application/     casos de uso (comandos) + queries CQRS + puertos de salida (Outbox, read model).
infrastructure/  adaptadores: REST, JPA/PostGIS (Hibernate Spatial), ACL de mapas (Resilience4j),
                 AMQP (consumer + publicador Outbox + stubs), proyector del read model.
bootstrap/       ejecutable (@SpringBootApplication), application.yml, migraciones Flyway, tests IT.
```

- **Persistencia**: PostgreSQL + **PostGIS** (`geometry(Point,4326)`) vía Hibernate Spatial + JTS.
  Entidades JPA separadas + mapper a mano, para que el dominio quede puro. El esquema lo gobierna
  Flyway (`bootstrap/src/main/resources/db/migration`, V1…V4); Hibernate no toca el DDL.
- **Eventos**: **Outbox transaccional manual** — cada evento se escribe en la tabla `outbox` en la
  **misma transacción** que el agregado; un publicador `@Scheduled` lo drena hacia RabbitMQ
  (entrega al-menos-una-vez; los consumidores deduplican por `eventId`).
- **CQRS**: el read model `historial_entrega_paciente` lo puebla un **proyector** que consume los
  eventos; las consultas leen solo del read model. La lectura es **eventualmente consistente**: se
  actualiza cuando el publicador drena el outbox (`OUTBOX_POLL_DELAY_MS`, 2 s por defecto).
- **Resiliencia (HU-3)**: el ACL al proveedor de mapas usa **Resilience4j** (timeout + retry +
  circuit breaker) y **degrada** la optimización al fallback puro `OptimizadorPorCercania`
  (Haversine).

### Mensajería

Un único exchange *topic* `nurtricenter.eventos`; la routing key distingue el tipo de evento.

| Cola | Recibe | Rol |
| :--- | :--- | :--- |
| `logistica.paquetes-listos-para-entrega` | `produccion.paquetes-listos-para-entrega` | Entrada: lo que publica Producción (HU-1) |
| `ms-notificaciones.avisos` | entrega confirmada / fallida | Stub del aviso al paciente |
| `ms-catering.reprogramaciones` | entrega no concretada | Stub de la reprogramación de la suscripción |
| `logistica.proyeccion-historial` | todos los eventos de la entrega | Proyector del read model (HU-6) |

---

## Cómo levantar

### Opción A — todo en Docker (recomendada)

Único requisito: Docker. La imagen compila el proyecto dentro del contenedor, no hace falta Maven
ni JDK en la máquina.

```bash
# 1) Credenciales locales (fichero no versionado). Editar los valores antes de seguir.
cp .env.example .env

# 2) Construye la imagen y levanta app + Postgres/PostGIS + RabbitMQ
docker compose up -d --build

# 3) Seguir el arranque hasta que la app quede lista
docker compose logs -f app
```

Sin `.env`, `docker compose up` aborta indicando qué variable falta.

Compose espera a que Postgres y RabbitMQ estén *healthy* antes de arrancar la app, y Flyway aplica
las migraciones al iniciar. El servicio queda listo cuando responde el health y `docker compose ps`
muestra `app` como `healthy`:

```bash
curl http://localhost:8080/actuator/health     # {"status":"UP"}
docker compose ps                              # app debe figurar como healthy
```

| Servicio | URL / puerto |
| :--- | :--- |
| API REST | http://localhost:8080/api/v1 |
| Health | http://localhost:8080/actuator/health |
| Consola RabbitMQ | http://localhost:15672 — credenciales del `.env` |
| Postgres (desde el host) | `localhost:5435` — BD `logistic`, credenciales del `.env` |

### Opción B — dependencias en Docker, app desde Maven

Para desarrollar con recarga rápida y depurador:

```bash
docker compose up -d postgres rabbitmq        # solo las dependencias
mvn clean install -DskipITs                   # compila los 4 módulos
mvn -f bootstrap/pom.xml spring-boot:run      # arranca la app en el host
```

Los valores por defecto de `application.yml` ya apuntan a `localhost` (Postgres en el puerto 5435
publicado por compose) y las credenciales salen del mismo `.env` (`spring.config.import`), así que
tampoco aquí hay que configurar nada a mano.

### Apagar

```bash
docker compose down        # conserva los datos
docker compose down -v     # borra también el volumen de Postgres (arranque limpio)
```

### Si algo falla

```bash
docker compose ps                        # ¿quién está unhealthy?
docker compose logs app | tail -50       # error de arranque de la app
docker compose logs postgres | tail -30  # error de la BD o de Flyway
docker compose up -d --build app         # recompilar solo la app tras un cambio
docker compose down -v && docker compose up -d --build   # arranque desde cero
```

Dos causas habituales:

- **Cambiaste `DB_PASSWORD` en `.env` y la app no conecta.** El volumen conserva la contraseña con
  la que se creó la base: hace falta `docker compose down -v`, o cambiarla dentro de Postgres (ver
  [Credenciales](#credenciales)).
- **Puerto ocupado.** El 8080 (app), 5435 (Postgres) y 5672/15672 (RabbitMQ) tienen que estar
  libres: `lsof -i :8080`.

---

## Probar la API

Las tres formas comparten el mismo recorrido (HU-1 → HU-6) y requieren el stack arriba.

```bash
./demo/demo.sh                                                    # demo end-to-end en curl
npx newman run postman/ms-logistic-delivery.postman_collection.json   # la misma jornada, con aserciones
```

- **`demo/demo.sh`** — recorre las seis HU vía REST e imprime cada respuesta.
- **[`postman/`](postman/README.md)** — colección con los 14 pasos del flujo, 6 casos de error
  `application/problem+json` y tests por petición. Se importa y funciona contra `localhost:8080`
  sin configurar nada.
- **`demo/requests.http`** — las mismas peticiones sueltas para el *REST Client* de VS Code o el
  cliente HTTP de IntelliJ.

### Endpoints

| Método | Ruta | HU |
| :--- | :--- | :--- |
| POST | `/api/v1/geocoding` | HU-2 · resolver una dirección a coordenadas |
| POST | `/api/v1/rutas` | HU-1/2/3 · planificar la ruta del día |
| GET | `/api/v1/repartidores/{repartidorId}/rutas?fecha=` | HU-1 · rutas del repartidor |
| POST | `/api/v1/rutas/{rutaId}/inicio` | HU-3 · iniciar la ruta |
| POST | `/api/v1/rutas/{rutaId}/paradas/{paradaId}/avance` | HU-3 · avanzar a la parada |
| POST | `/api/v1/rutas/{rutaId}/paradas/{paradaId}/completar` | HU-3 · completar la parada |
| POST | `/api/v1/rutas/{rutaId}/paradas/{paradaId}/fallo` | HU-3 · fallar la parada |
| POST | `/api/v1/entregas/{entregaId}/confirmacion` | HU-4 · confirmar con constancia |
| POST | `/api/v1/entregas/{entregaId}/fallo` | HU-5 · registrar el fallo |
| POST | `/api/v1/entregas/{entregaId}/reintento` | HU-5 · reintentar la entrega |
| GET | `/api/v1/pacientes/{pacienteId}/entregas?desde=&hasta=` | HU-6 · historial (read model) |
| GET | `/api/v1/entregas/{entregaId}/constancia` | HU-6 · constancia de la entrega |

Los errores salen como `application/problem+json` (RFC 7807).

### Ver la degradación de HU-3

Basta con levantar la app con el proveedor de mapas «caído»:

```bash
MAPS_STUB_FAILURE_RATE=1.0 mvn -f bootstrap/pom.xml spring-boot:run
```

`POST /api/v1/rutas` sigue respondiendo 201: en el log aparece *«Proveedor de mapas no disponible
(...). Degradando a optimizacion por cercania (Haversine, HU-3)»* y la ruta se planifica con el
fallback de dominio.

---

## Pruebas y cobertura

El contrato de pruebas del microservicio está escrito en **[`testing-rules.md`](testing-rules.md)**:
qué se prueba, qué se reemplaza por un doble, qué está prohibido y qué umbral de cobertura se exige.
Lo aplican por igual quien programa y los skills de IA del repositorio (ver
[`AGENTS.md`](AGENTS.md)).

### Verificación completa — lo que hay que correr antes de entregar

```bash
./scripts/verificar-pruebas.sh
```

Hace tres cosas y devuelve 0 solo si las tres pasan:

1. chequeo mecánico de las restricciones de `testing-rules.md` (P1–P7 y nombres),
2. la suite de pruebas solitarias,
3. los umbrales de cobertura de JaCoCo.

```bash
./scripts/verificar-pruebas.sh --rapido   # solo el paso 1: segundos, sin Maven
```

**No necesita Docker.**

### Suite y cobertura

```bash
mvn test                     # pruebas solitarias + reporte JaCoCo
mvn test -Pcobertura         # además falla el build si un módulo baja de su umbral
mvn clean test               # desde cero, si sospechás de resultados cacheados

open domain/target/site/jacoco/index.html          # reporte HTML por módulo
open application/target/site/jacoco/index.html
open infrastructure/target/site/jacoco/index.html
```

Umbrales que hace cumplir el perfil `cobertura`: `domain` y `application` ≥ 80 % de línea y ≥ 70 %
de rama. `infrastructure` no tiene umbral en esta tarea (buena parte solo se ejerce con
Postgres/Rabbit, es decir con pruebas `IT`).

Quedan fuera del cómputo las clases sin comportamiento propio (DTOs, entidades JPA,
`@Configuration` y la clase `main`): probarlas solo subiría el porcentaje sin comprar evidencia.

### Correr un subconjunto

```bash
mvn -pl domain test                # un solo módulo
mvn -pl application -am test       # un módulo y lo que necesita compilar

# Una clase o un método: acotar al módulo donde vive esa clase
mvn -pl domain test -Dtest=EntregaTest
mvn -pl domain test -Dtest='EntregaTest#noPuedeConfirmarseDosVeces'
mvn -pl application test -Dtest='Consultar*Test'
```

Ojo con `-Dtest` sobre el reactor completo: falla en los módulos donde el patrón no coincide
(*"No tests matching pattern ... were executed!"*). O se acota el módulo con `-pl`, como arriba, o
se desactiva ese error — la propiedad lleva el prefijo `surefire.`:

```bash
mvn test -Dtest=EntregaTest -Dsurefire.failIfNoSpecifiedTests=false
```

### Pruebas de integración (sociables)

Requieren Docker: Testcontainers levanta PostGIS y RabbitMQ solos. Su contrato está en
**[`integration-testing-rules.md`](integration-testing-rules.md)**: qué frontera prueba cada `IT`,
qué se levanta de verdad y qué se virtualiza, y la exigencia de que cada frontera tenga su **flujo
correcto y su flujo incorrecto**.

```bash
./scripts/verificar-integracion.sh              # Docker + reglas + matriz frontera×flujo + suite
./scripts/verificar-integracion.sh --rapido     # sin Maven: precondiciones y chequeo mecánico
./scripts/verificar-integracion.sh --con-postman # además, la colección Postman con newman

mvn verify                   # solitarias + sociables (*IT)
mvn install -DskipITs        # compilar e instalar saltándose las sociables
```

Todas las clases `IT` comparten el cableado de `support/PruebaDeIntegracion`, de modo que la suite
entera corre sobre un único contexto de Spring y un único par de contenedores.

| Tipo | Sufijo | Plugin | Necesita |
| :--- | :--- | :--- | :--- |
| Solitaria (unitaria) | `*Test` | surefire — `mvn test` | nada |
| Sociable (integración) | `*IT` | failsafe — `mvn verify` | Docker |

### Generar pruebas con IA

Los skills leen las mismas reglas que el script de verificación, así que no pueden desalinearse:

```
/test-writer                genera pruebas solitarias según testing-rules.md
/test-checker               verifica contra esas mismas reglas; PASS/FAIL
/integration-test-writer    genera pruebas *IT según integration-testing-rules.md
/integration-test-checker   verifica los *IT contra ese contrato; PASS/FAIL
```

Definidos en `.claude/skills/`. Un agente que no soporte skills obtiene lo mismo leyendo
[`AGENTS.md`](AGENTS.md).

---

## Credenciales

Ninguna contraseña vive en el repositorio ni dentro de la imagen. El único sitio donde existen es
`.env`, un fichero local ignorado por Git y por el contexto de build de Docker:

| Fichero | Rol |
|---|---|
| `.env.example` | Plantilla versionada, con marcadores en lugar de valores |
| `.env` | Valores reales de tu máquina. **No se versiona ni se publica** |

Ese mismo `.env` lo consumen los dos modos de ejecución:

- **En Docker**: Compose lo lee e inyecta las variables en cada contenedor (`${DB_USER:?...}`
  aborta el arranque si falta alguna).
- **Fuera de Docker** (IDE / `spring-boot:run`): Spring Boot lo importa como *properties* mediante
  `spring.config.import` en `application.yml`. Las variables de entorno reales tienen prioridad
  sobre el fichero, así que en contenedor manda siempre lo que inyecta Compose.

**Rotar la contraseña de Postgres.** El volumen `logistic-pgdata` conserva la contraseña con la que
se creó la base, así que cambiar `.env` no basta:

```bash
# Opción A — arranque limpio (borra los datos de la demo)
docker compose down -v && docker compose up -d

# Opción B — conservando los datos
docker compose exec postgres psql -U logistic -d logistic \
  -c "ALTER USER logistic WITH PASSWORD 'la-nueva';"
# ...y luego actualizar DB_PASSWORD en .env + docker compose up -d
```

RabbitMQ no usa volumen, así que le basta con `docker compose up -d` tras editar el `.env`.

---

## Configuración

La imagen se parametriza por variables de entorno (ver `docker-compose.yml`); entre paréntesis, el
valor por defecto:

| Variable | Descripción |
|---|---|
| `DB_URL` (`jdbc:postgresql://localhost:5435/logistic`) | JDBC de Postgres/PostGIS |
| `DB_USER` / `DB_PASSWORD` (**obligatorias**, sin valor por defecto) | Credenciales de la BD |
| `RABBITMQ_HOST` (`localhost`) / `RABBITMQ_PORT` (`5672`) | Broker AMQP |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` (**obligatorias**, sin valor por defecto) | Credenciales del broker |
| `OUTBOX_POLL_DELAY_MS` (`2000`) / `OUTBOX_BATCH_SIZE` (`50`) | Publicador del outbox |
| `MAPS_BASE_LAT` (`-17.7692`) / `MAPS_BASE_LON` (`-63.1824`) | Punto base del geocodificador stub (centro de Santa Cruz de la Sierra) |
| `MAPS_STUB_LATENCY_MS` (`200`) / `MAPS_STUB_FAILURE_RATE` (`0.0`) | Stub del proveedor de mapas (para demostrar la degradación de HU-3) |
| `REINTENTOS_MAXIMO` (`2`) | Reintentos permitidos antes de dar la entrega por no concretada (HU-5) |
| `JAVA_OPTS` | Opciones de la JVM |

---

## Publicar la imagen en Docker Hub

La imagen no contiene credenciales: el `Dockerfile` no declara ningún `ENV`/`ARG` con secretos, el
`.dockerignore` excluye `.env` del contexto de build y `application.yml` no trae contraseñas por
defecto. Quien la descargue debe aportar las suyas.

```bash
# Login con un Access Token de Docker Hub (Account Settings > Personal access tokens),
# nunca con la contraseña de la cuenta y nunca en la propia línea de comandos.
echo "$DOCKERHUB_TOKEN" | docker login -u <usuario> --password-stdin

docker build -t <usuario>/ms-logistic-delivery:1.0.0 .
docker push <usuario>/ms-logistic-delivery:1.0.0
```

Comprobación rápida de que no se publica nada sensible:

```bash
docker history --no-trunc <usuario>/ms-logistic-delivery:1.0.0 | grep -i -E "pass|secret"
docker run --rm <usuario>/ms-logistic-delivery:1.0.0 printenv | grep -i -E "pass|secret"
```

Consumo de la imagen publicada, con las credenciales del entorno de quien la ejecuta:

```bash
docker run -d -p 8080:8080 --env-file .env \
  -e DB_URL=jdbc:postgresql://mi-host:5432/logistic \
  -e RABBITMQ_HOST=mi-broker \
  <usuario>/ms-logistic-delivery:1.0.0
```

> En un despliegue real, el `.env` se sustituye por el gestor de secretos de la plataforma
> (Docker/Swarm secrets, Kubernetes Secrets, AWS Secrets Manager…). El contrato de la imagen no
> cambia: sigue leyendo las mismas variables de entorno.
