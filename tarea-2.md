# Tarea 2 — Pruebas de integración aplicadas al Proyecto Final

**Alumno:** Carlos Cuellar · **Microservicio:** `ms-logistic-delivery` (Nur-tricenter)
**Stack:** Java 21 · Spring Boot 3.5 · Maven multi-módulo (arquitectura hexagonal)

---

## 1. Qué pedía la consigna y dónde está resuelto

| Pedido | Resuelto en |
| :--- | :--- |
| Pruebas de integración en el microservicio | 19 pruebas `*IT` con Testcontainers (PostGIS + RabbitMQ), en `bootstrap/src/test/java` |
| Un flujo correcto y uno incorrecto | Las 4 fronteras tienen su par; el flujo incorrecto vive sobre todo en `FlujoIncorrectoIT` (§3) |
| Postman como herramienta | `postman/ms-logistic-delivery.postman_collection.json`, ejecutable con newman desde el script de verificación |
| `integration-test-writer` con sus skills | `.claude/skills/integration-test-writer/` y `.../integration-test-checker/` |
| Entorno validado de pruebas de integración para IA | `integration-testing-rules.md` + `scripts/verificar-integracion.sh` + `AGENTS.md` |

---

## 2. Antes y después

Línea base medida sobre el código tal como quedó de la Tarea 1, antes de tocar nada:

| | Antes | Después |
| :--- | ---: | ---: |
| Pruebas `*IT` declaradas | 20 | 19 |
| Ejecutadas por `mvn verify` | 6 | **19** |
| Omitidas (`@EnabledIfSystemProperty`) | 14 | **0** |
| Resultado | 6 errores | **0 fallos, 0 errores** |

Dos cosas explican el "antes":

1. **Siete clases `*ExternaManualIT` no corrían.** Estaban condicionadas a
   `-Dmsld.external.db=true` contra un stack levantado a mano, así que `mvn verify` las saltaba en
   silencio. Cinco se migraron a Testcontainers y se renombraron; dos eran la variante manual de
   tests que ya existían con contenedores y se borraron por duplicadas.
2. **Las seis que sí corrían fallaban todas.** Testcontainers negociaba una versión antigua de la
   API de Docker y el Engine 29 la rechaza con un 400. Se fija con la propiedad
   `docker.api.version` en el pom raíz, que failsafe pasa a la JVM de las pruebas.

El total baja de 20 a 19 porque se eliminaron 5 pruebas duplicadas y se agregaron 4 nuevas.

---

## 3. Las cuatro fronteras, con su flujo correcto y su incorrecto

Una prueba de integración se define por la frontera que cruza. Éstas son las del microservicio:

| # | Frontera | Flujo correcto | Flujo incorrecto |
| :--- | :--- | :--- | :--- |
| 1 | REST → aplicación → JPA → PostGIS | Planificar y confirmar persiste la entrega y deja el evento en el Outbox | Entrega inexistente → **404**; confirmar dos veces → **422**; ruta sin paquetes → **400**, todos en `problem+json` |
| 2 | AMQP entrante | `PaquetesListos` planifica y persiste la ruta | Mensaje mal formado: se descarta sin reencolar y el consumidor sigue procesando el siguiente |
| 3 | Outbox → RabbitMQ → proyector → read model | El evento se publica, el proyector alimenta el read model y la consulta responde | Agotados los reintentos, la saga termina en `ms-catering` como no concretada; constancia inexistente → 404 |
| 4 | ACL del proveedor de mapas (virtualizado) | El endpoint de geocodificación devuelve coordenadas | Con el proveedor caído al 100 %, la planificación degrada al fallback por cercanía en vez de romperse |

El flujo incorrecto de la frontera 1 es el que mejor muestra por qué esto no es una prueba unitaria:
el **422** de confirmar dos veces sólo se puede provocar si la primera confirmación quedó realmente
escrita en PostGIS. Con un doble, la segunda llamada no tendría de dónde saber que ya pasó.

Todas las clases comparten el cableado de `support/PruebaDeIntegracion`: Spring cachea un contexto
por combinación de configuración, así que una sola anotación significa **un solo contexto y un solo
par de contenedores** para toda la suite.

---

## 4. El harness de pruebas de integración para IA

Mismo patrón que en la Tarea 1, con su propio contrato porque las reglas son distintas:

```
ms-logistic-delivery/
├── integration-testing-rules.md                        ← única fuente de las reglas sociables
├── .claude/skills/integration-test-writer/SKILL.md       genera *IT y cita la regla de cada aserción
├── .claude/skills/integration-test-checker/SKILL.md      verifica; nunca dice "listo" si falla
├── scripts/verificar-integracion.sh                     Docker + reglas + matriz + suite + newman
└── AGENTS.md                                            mismo contrato para opencode / Cursor / Codex
```

`integration-testing-rules.md` fija las FRONTERAS, el COMPORTAMIENTO (I1–I5), los LÍMITES (qué se
levanta de verdad y qué se virtualiza), las RESTRICCIONES mecánicas (Q1–Q6) y los NOMBRES.

La regla que más trabajo hace es **I2**: cada frontera necesita su flujo correcto *y* su flujo
incorrecto. El script la verifica de una forma poco habitual: **lee la tabla de fronteras del propio
archivo de reglas**, busca las clases que nombra y comprueba en sus `@DisplayName` que estén los dos
flujos. La documentación es la fuente, no una copia dentro del script.

**El harness se validó de verdad**, no se dio por bueno:

1. Al estrenarlo encontró un test real sin etiquetar (`constanciaInexistenteDevuelve404`), que era
   además un flujo incorrecto sin declarar.
2. Se lo hizo fallar a propósito contra un archivo con una violación de cada regla, y las detectó
   una por una:

```
EjemploViolacionesIT.java:9   Q6       @DirtiesContext reinicia los contenedores: aislar por dato (I4)
EjemploViolacionesIT.java:12  LIMITES  doble dentro del proceso: deja de ser prueba de integracion
EjemploViolacionesIT.java:16  NOMBRES  el metodo no debe empezar con "test": testDeberiaFallarElChequeo
EjemploViolacionesIT.java:16  NOMBRES  prueba sin @DisplayName "HU-n - flujo correcto/incorrecto"
EjemploViolacionesIT.java:17  Q1       Thread.sleep: la espera va con Awaitility
EjemploViolacionesIT.java:23  Q4       host/puerto del stack hardcodeado: usar @ServiceConnection
EjemploViolacionesIT.java:22  Q5       prueba sin ninguna asercion: pruebaSinNingunaAsercion
EjemploViolacionesIT.java:27  Q2       @Disabled sin comentario que explique la causa
EjemploViolacionesIT.java:28  Q3       prueba condicionada: no corre en mvn verify, no es evidencia
```

3. Se agregó una frontera ficticia a la tabla de reglas para comprobar que la matriz también falla
   cuando una frontera se queda sin su par.

El archivo de violaciones quedó como `EjemploViolacionesIT.java.ejemplo` para que no entre al build.
Un chequeo que nunca falló no verifica nada: había que verlo fallar.

---

## 5. Cómo reproducirlo

```bash
cd ms-logistic-delivery

./scripts/verificar-integracion.sh              # Docker + reglas + matriz + suite (necesita Docker)
./scripts/verificar-integracion.sh --rapido     # solo reglas y matriz, sin Maven: segundos

# La colección Postman como suite de regresión, contra el stack levantado:
docker compose up -d --build
./scripts/verificar-integracion.sh --con-postman
```

Salida actual:

```
== 1. Precondiciones ==
  Docker OK (Engine 29.7.2)

== 2. Chequeo mecanico contra integration-testing-rules.md ==
Archivos de prueba sociables analizados: 11
Sin violaciones de las RESTRICCIONES ni de NOMBRES.

== Matriz frontera x flujo (seccion FRONTERAS de integration-testing-rules.md) ==
  1. REST → aplicación → JPA → PostGIS           correcto SI  incorrecto SI
  2. AMQP entrante (PaquetesListosListener)      correcto SI  incorrecto SI
  3. Outbox → RabbitMQ → proyector → read model  correcto SI  incorrecto SI
  4. ACL del proveedor de mapas (virtualizado)   correcto SI  incorrecto SI

== 3. Suite de pruebas sociables (failsafe) ==
  Pruebas de integracion: 19  ·  fallos: 0  ·  errores: 0  ·  omitidas: 0

== 4. Coleccion Postman (newman) ==
  requests       ejecutadas 21   fallidas 0
  test-scripts   ejecutadas 21   fallidas 0
  assertions     ejecutadas 41   fallidas 0

PASS — precondiciones, reglas, matriz y suite de integracion en verde.
```

El harness unitario de la Tarea 1 sigue intacto y en verde: `./scripts/verificar-pruebas.sh`.

---

## 6. Alcance y qué queda pendiente

El trabajo se aplicó sobre `ms-logistic-delivery`, el microservicio del que soy responsable en el
Proyecto Final.

- **Sin CI.** El material de la Clase 2 insiste en que las suites de integración vivan en el
  pipeline. Queda fuera de esta entrega a propósito; el script ya está listo para que un workflow lo
  invoque tal cual.
- **El consumidor AMQP no deduplica.** Un mismo `PaquetesListos` entregado dos veces planificaría dos
  rutas. Se detectó al buscar el flujo incorrecto de la frontera 2 y se probó en cambio el mensaje
  mal formado, que sí está resuelto. Es una deuda del diseño, no de las pruebas.
- **`DegradacionMapasIT` paga su propio contexto** porque necesita el proveedor de mapas caído desde
  el arranque. Es el único caso, y está documentado en el contrato.
