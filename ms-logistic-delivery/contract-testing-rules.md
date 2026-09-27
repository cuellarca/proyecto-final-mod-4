# contract-testing-rules.md — reglas de las pruebas de contrato (Pact) de `ms-logistic-delivery`

Contrato de las pruebas **de contrato** entre `app-repartidor` (consumidor) y `ms-logistic-delivery`
(provider). Complementa a [`testing-rules.md`](testing-rules.md) (solitarias) y a
[`integration-testing-rules.md`](integration-testing-rules.md) (sociables). Lo leen tres
consumidores y **ninguno puede tener su propia copia**:

| Consumidor | Qué hace con este archivo |
|---|---|
| La persona que programa | Lo aplica al escribir interacciones del contrato |
| El skill `pact-writer` | Genera interacciones que lo cumplen y cita la regla de cada decisión |
| El skill `pact-checker` + `scripts/verificar-contratos.sh` | Verifica lo generado contra estas mismas reglas |

## STACK

- Java 21 · JUnit 5 · **Pact JVM 4.6.x**, especificación Pact **V3**.
- Consumer: `au.com.dius.pact.consumer:junit5` (`PactConsumerTestExt`, `@Pact`, `@PactTestFor`) y
  el DSL `LambdaDsl` para los cuerpos.
- Provider: `au.com.dius.pact.provider:junit5spring` (`@Provider`, `@PactFolder`, `@State`,
  `MockMvcTestTarget`) sobre el contexto de `@PruebaDeIntegracion` (PostGIS y RabbitMQ con
  Testcontainers).
- El pacto se escribe en `pacts/` (fuera de `target/`, versionado). El provider lo lee de ahí. En
  un despliegue real ese archivo viajaría por un Pact Broker; aquí la carpeta cumple ese papel.

## PIEZAS

| Pieza | Dónde |
|---|---|
| Cliente HTTP real del consumidor | `consumidor-app-repartidor/src/main/java/.../ClienteLogistica.java` |
| Pruebas del consumer (generan el pacto) | `consumidor-app-repartidor/src/test/java/.../ClienteLogisticaPactTest.java` |
| Pacto generado | `pacts/app-repartidor-ms-logistic-delivery.json` |
| Verificación del provider | `bootstrap/src/test/java/.../contrato/ContratoAppRepartidorPactIT.java` |

## COMPORTAMIENTO — lado consumer

- **C1** — **Una interacción por método `@Pact`, y un `@Test` por interacción.** Dos escenarios del
  mismo endpoint (p. ej. ok y error) son dos interacciones, cada una con su estado.
- **C2** — Orden fijo: `given` → `uponReceiving` → solicitud (`method`, `path`, `query`, `headers`,
  `body`) → `willRespondWith` (`status`, `headers`, `body`). La solicitud declara los headers que
  el cliente real manda (p. ej. `Content-Type` en un POST con cuerpo).
- **C3** — La prueba ejercita el **cliente real** (`ClienteLogistica`) apuntado a
  `mockServer.getUrl()`. Un `RestTemplate` suelto dentro de la prueba solo verifica que Pact
  funciona, no que el consumidor sabe hablar con el provider.
- **C4** — Se afirma la **forma** que el consumidor necesita, no los valores que decide el provider:
  que la lista no esté vacía, que el id no esté en blanco, que el orden sea positivo. Nunca cuántos
  elementos hay exactamente ni qué id tienen.
- **C5** — **Lo que controla el consumidor** (el `repartidorId`, la `fecha`, el cuerpo de la
  solicitud) va **literal** en la solicitud y, si el provider lo necesita para preparar el estado,
  viaja como **parámetro del estado**: `given("...", Map.of("repartidorId", ...))`. Nunca metido en
  el texto del estado.
- **C6** — **Lo que genera el provider** (ids, fechas de creación) se describe con **matchers** en la
  respuesta (`uuid`, `date`, `stringMatcher`, `integerType`, `numberType`, `minArrayLike`) y, si
  aparece en la solicitud, con `pathFromProviderState` / `valueFromProviderState`: el `@State`
  del provider devuelve el valor real.
- **C7** — El contrato pide **solo lo que el consumidor lee**. Los records del cliente declaran
  únicamente los campos que la app usa; el pacto no fija campos que nadie consume. Así el provider
  puede agregar campos sin romper a nadie.
- **C8** — Los estados se nombran en **lenguaje de negocio** ("el repartidor tiene una ruta
  planificada para la fecha"), no técnico ("seed1"): es lo que el equipo del provider lee para
  saber qué preparar.

## COMPORTAMIENTO — lado provider

- **V1** — Cada estado del pacto tiene **exactamente un** `@State` con el mismo texto en el provider.
- **V2** — El `@State` siembra los datos a través de los **casos de uso reales**
  (`PlanificarRutaDelDia`, …), no con SQL a mano ni con dobles: si el estado no se puede alcanzar
  por la aplicación, el contrato describe algo que el servicio no puede cumplir.
- **V3** — La verificación corre sobre el contexto compartido de `@PruebaDeIntegracion` con
  `MockMvcTestTarget`: borde REST, casos de uso, JPA y PostGIS reales, sin pagar un contexto nuevo.
  Como toda prueba `*IT`, cumple también `integration-testing-rules.md` (LÍMITES y Q1–Q6).
- **V4** — Los datos que el estado genera por su cuenta usan identificadores únicos (regla I4 de
  integración): el contrato no depende del orden ni de lo que haya sembrado otra prueba.

## RESTRICCIONES (mecánicas, las verifica el script)

- **K1** — El JSON de `pacts/` **no se edita a mano**. El versionado tiene que ser idéntico al que
  regenera el consumer. Si el contrato cambia, cambia la prueba, nunca el archivo.
- **K2** — Prohibida la URL hardcodeada en el consumer (`http://localhost…`, `"localhost"`). Cada
  `@Test` construye el cliente con `mockServer.getUrl()` (C3).
- **K3** — Prohibido `.body("...")` con un string literal: los cuerpos se escriben con `LambdaDsl`
  (`newJsonBody`, `newJsonArrayMinLike`).
- **K4** — Prohibidos los valores fijos en el cuerpo de la **respuesta** (`stringValue`,
  `numberValue`, `booleanValue` después de `willRespondWith`): la respuesta va con matchers (C6).
- **K5** — Cada método `@Pact` declara un estado con `.given(` y **una sola** `uponReceiving` (C1).
- **K6** — Prohibida una prueba sin aserción y prohibido `@Disabled` sin un comentario que explique
  la causa.
- **K7** — El provider lee el pacto de `@PactFolder("../pacts")` y no declara dobles dentro del
  proceso (`@MockitoBean`/`@MockBean`).
- **K8** — El pacto tiene **al menos dos interacciones**, y cada `providerState` del JSON tiene su
  `@State` en el provider (V1).

## NOMBRES

- Consumer: `<Cliente>PactTest`, en el módulo `consumidor-app-repartidor`. Métodos `@Pact` con el
  nombre de la operación (`rutasDelDia`, `confirmarEntrega`). `@Test` en camelCase y en español, sin
  el prefijo `test`, con `@DisplayName` **obligatorio** en lenguaje de negocio: qué garantiza el
  contrato a la app ("la app confirma una entrega enviando la constancia").
- Provider: `Contrato<Consumidor>PactIT`, en `bootstrap/src/test/java/.../contrato/`. Su
  `@DisplayName` sigue el formato de integración: `"HU-n · flujo correcto: ..."`.

## COBERTURA DEL CONTRATO

- Cada método público de `ClienteLogistica` se ejercita en al menos una interacción: un endpoint que
  la app usa y que no está en el contrato puede romperse sin que nadie se entere.
- El contrato **no** cuenta para los umbrales de JaCoCo. `ClienteLogistica` es código del consumidor
  simulado, no del microservicio.
