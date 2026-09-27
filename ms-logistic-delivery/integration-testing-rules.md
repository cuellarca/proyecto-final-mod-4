# integration-testing-rules.md — reglas de las pruebas de integración de `ms-logistic-delivery`

Contrato de las pruebas **sociables** del microservicio. Complementa a
[`testing-rules.md`](testing-rules.md), que gobierna las solitarias y deja las `IT` fuera de su
alcance. Lo leen tres consumidores y **ninguno puede tener su propia copia**:

| Consumidor | Qué hace con este archivo |
|---|---|
| La persona que programa | Lo aplica al escribir pruebas de integración |
| El skill `integration-test-writer` | Genera pruebas que lo cumplen y cita la regla de cada aserción |
| El skill `integration-test-checker` + `scripts/verificar-integracion.sh` | Verifica lo generado contra estas mismas reglas |

## STACK

- Java 21 · JUnit 5 · failsafe (`mvn verify`) · AssertJ · MockMvc sobre el contexto completo ·
  Awaitility para lo asincrónico.
- Testcontainers: `postgis/postgis:16-3.4` y `rabbitmq:3-management`, cableados a Spring con
  `@ServiceConnection` (`support/PostgisContainerConfig`, `support/RabbitContainerConfig`).
- `support/PruebaDeIntegracion` es el cableado común: Spring cachea un contexto por combinación de
  configuración, así que compartir la anotación significa **un solo contexto y un solo par de
  contenedores** para toda la suite.
- La versión de la API de Docker que docker-java negocia se fija en el pom raíz
  (`docker.api.version`): sin eso, los Engine recientes rechazan la conexión con un 400.
- La colección Postman (`postman/`) es la misma suite vista desde afuera del proceso; la corre
  newman.

## FRONTERAS

Cada prueba declara qué frontera cruza. Son cuatro:

| # | Frontera | Dónde se prueba |
|---|---|---|
| 1 | REST → aplicación → JPA → PostGIS | `FlujoPersistenciaOutboxIT`, `BordeRestAmqpIT`, `PersistenciaRoundTripIT`, `FlujoIncorrectoIT` |
| 2 | AMQP entrante (`PaquetesListosListener`) | `BordeRestAmqpIT`, `FlujoEntregaExitosaIT`, `FlujoEntregaNoConcretadaIT`, `FlujoIncorrectoIT` |
| 3 | Outbox → RabbitMQ → proyector → read model | `OutboxPublicacionIT`, `SagaEntregaIT`, `LecturaReadModelIT` |
| 4 | ACL del proveedor de mapas (virtualizado) | `BordeRestAmqpIT`, `DegradacionMapasIT` |

## FLUJOS

Además de por frontera, las pruebas se agrupan por el flujo de negocio que recorren, con
`@Tag("flujo-a")` / `@Tag("flujo-b")`. Cada flujo tiene una prueba de punta a punta.

| Flujo | Tag | Recorrido | Punta a punta |
|---|---|---|---|
| A · Entrega exitosa | `flujo-a` | `PaquetesListos` → ruta planificada → iniciar, avanzar y completar la parada → confirmar con constancia → `EntregaConfirmada` a ms-notificaciones → historial y constancia | `FlujoEntregaExitosaIT` |
| B · Entrega no concretada | `flujo-b` | `PaquetesListos` → ruta planificada → iniciar, avanzar y fallar la parada → fallo y reintento hasta agotar los intentos → `EntregaNoConcretada` a ms-catering → historial | `FlujoEntregaNoConcretadaIT` |

- Correr un solo flujo: `mvn verify -Dgroups=flujo-a` (o `flujo-b`).
- Una prueba que toca los dos flujos lleva los dos tags. `FlujoIncorrectoIT` (validaciones y
  errores del borde REST) y `DegradacionMapasIT` (caída del proveedor de mapas) no pertenecen a
  ningún flujo: se agrupan solo por frontera.

## COMPORTAMIENTO

- **I1** — Un `IT` prueba la **colaboración** entre módulos, no una unidad. Si lo que se afirma se
  puede afirmar sin cruzar una frontera, es una prueba solitaria y va bajo `testing-rules.md`.
- **I2** — Cada frontera tiene al menos un **flujo correcto** y un **flujo incorrecto**. El flujo
  incorrecto es parte del contrato, no un extra.
- **I3** — La aserción es sobre el efecto que **cruzó** la frontera: la fila en PostGIS, el mensaje
  en la cola, el `status` + `application/problem+json` de la respuesta. Una aserción que un mock
  podría satisfacer no prueba integración.
- **I4** — Cada prueba siembra sus propios datos con identificadores únicos. Ninguna depende del
  orden de ejecución ni del estado que dejó otra.
- **I5** — Lo asincrónico (Outbox, proyector, listeners) se espera con Awaitility y un timeout
  explícito. Esperar por tiempo fijo produce pruebas frágiles.

## LÍMITES

- **Se levanta de verdad:** Postgres/PostGIS y RabbitMQ. Son las dependencias que el servicio tiene
  en producción y las que hacen aparecer los defectos de integración.
- **Se virtualiza:** el proveedor externo de mapas, mediante `logistic.maps.stub-failure-rate`. Es
  el sistema de terceros que no controlamos: se simula para poder ejercer su caída.
- **No se mockea nada dentro del proceso.** Un `@MockitoBean`/`@MockBean` en un `IT` significa que la
  prueba dejó de ser de integración.

## RESTRICCIONES (mecánicas, las verifica el script)

- **Q1** — Prohibido `Thread.sleep(...)`: la espera va con Awaitility (I5).
- **Q2** — Prohibido `@Disabled` sin un comentario que explique la causa y su fecha.
- **Q3** — Prohibido `@EnabledIfSystemProperty` y demás `@EnabledIf*`: un `IT` que no corre en
  `mvn verify` no es evidencia de nada.
- **Q4** — Prohibido hardcodear el host, el puerto o las credenciales del stack (`localhost:5435`,
  `5672`, …): el cableado entra por `@ServiceConnection`.
- **Q5** — Prohibida una prueba sin ninguna aserción.
- **Q6** — Prohibido `@DirtiesContext`: si una prueba necesita el contexto limpio, el problema real
  es el dato compartido (I4), y reiniciar el contexto reinicia también los contenedores.

## NOMBRES

- Clase: `<Escenario>IT` — el sufijo es lo que hace que failsafe la corra.
- Método: `escenarioResultado` en camelCase, en español, sin `test` como prefijo.
- `@DisplayName` **obligatorio**, empezando por el código de la historia y el tipo de flujo:
  `"HU-4 · flujo correcto: ..."` o `"HU-4 · flujo incorrecto: ..."`. Es lo que hace verificable I2.

## COBERTURA

- Los `IT` **no** cuentan para los umbrales de JaCoCo: eso lo gobierna `testing-rules.md`. Medir
  cobertura de línea sobre pruebas de integración premia levantar el contexto, no probar la
  colaboración.
- La cobertura que sí se exige es la **matriz frontera × (correcto, incorrecto)** de la sección
  FRONTERAS. El script la verifica leyendo los `@DisplayName`.
