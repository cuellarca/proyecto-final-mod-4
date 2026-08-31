# testing-rules.md — reglas de prueba de `ms-logistic-delivery`

Contrato único de pruebas del microservicio. Lo leen tres consumidores y **ninguno puede
tener su propia copia**:

| Consumidor | Qué hace con este archivo |
|---|---|
| La persona que programa | Lo aplica al escribir pruebas a mano |
| El skill `test-writer` | Genera pruebas que lo cumplen y cita la regla de cada aserción |
| El skill `test-checker` + `scripts/verificar-pruebas.sh` | Verifica lo generado contra estas mismas reglas |

> Generar y verificar leen el **mismo** archivo: no pueden desalinearse solos.

## STACK

- Java 21 · JUnit 5 (Jupiter) · Mockito · AssertJ · MockMvc (`standaloneSetup`).
- Cobertura con JaCoCo (`mvn test` deja el reporte en `<módulo>/target/site/jacoco/index.html`).
- Un módulo = una frontera arquitectónica: `domain`, `application`, `infrastructure`, `bootstrap`.

## COMPORTAMIENTO

- **R1** — Una prueba por comportamiento observable, no por método. Si un método tiene tres
  reglas de negocio, son tres pruebas.
- **R2** — Cada prueba responde las tres preguntas de la clase: qué comportamiento afirma, qué
  tendría que romperse para que falle, y quién es dueño de la falla. Si la respuesta a la
  segunda es "nada", la prueba es decoración y se borra.
- **R3** — Estructura Arrange–Act–Assert. La aserción va **al final**; nunca se actúa después
  de asertar.
- **R4** — Como máximo una aserción *o* una verificación de mock por prueba. Si la prueba
  asserta estado, no verifica mocks; si verifica un mock, no asserta estado. Aserciones
  encadenadas de AssertJ sobre un mismo objeto (`.hasSize(3).contains(...)`) y las expectativas
  encadenadas de MockMvc sobre una misma respuesta HTTP cuentan como una.
- **R5** — Valores esperados como literales (`"CONFIRMADA"`, `-17.78`), no como variables
  derivadas de la misma lógica que se está probando.
- **R6** — Nada de pruebas negativas ("verificar que X *no* pasó") salvo que el no-efecto sea
  la regla de negocio en sí — idempotencia y dedupe sí califican, y se documentan con
  `@DisplayName`.

## LÍMITES (seams)

El seam de prueba y el de producción son el **mismo**: la inyección de dependencias por
constructor que ya usa el proyecto. Está prohibido inventar un seam que solo exista en pruebas.

**Se reemplazan por test double (mock/stub/fake):**

- Puertos de salida: `EntregaRepository`, `RutaRepository`, `PublicadorDeEventos`,
  `HistorialReadModel`, `Geocodificador`, `OptimizadorDeRutas`.
- Repositorios Spring Data: `OutboxJpaRepository`, `HistorialJpaRepository`.
- Infraestructura remota: `RabbitTemplate`, el proveedor externo de mapas, HTTP saliente.
- El tiempo: siempre `Clock.fixed(...)`. Prohibido `Instant.now()` sin reloj inyectado dentro
  de una prueba.
- Los casos de uso, **solo** cuando el SUT es un controlador REST o un listener AMQP.

**Nunca se mockean:**

- Entidades, agregados y Value Objects del dominio (`Entrega`, `RutaDeEntrega`, `Parada`,
  `Geolocalizacion`, `Destino`, `Intentos`, ids). Se construyen de verdad con sus factories.
- Servicios de dominio puros y sin estado (`OptimizadorPorCercania`, `PoliticaDeReintento`).
- Mappers (`RestMapperImpl`, `EntregaJpaMapper`, `RutaJpaMapper`): se usa la implementación real.

## SOLITARIAS vs. SOCIABLES

- **Solitaria** — no cruza fronteras de proceso y la única clase concreta no-dominio es el SUT.
  Vive en `src/test/java` y la corre `mvn test` (surefire). Es la norma.
- **Sociable / integración** — necesita Postgres, RabbitMQ o el contexto Spring completo. Va en
  una clase con sufijo `IT`, la corre failsafe (`mvn verify`), y **no** cuenta para el umbral de
  cobertura de esta tarea.
- Una prueba solitaria **no** puede abrir base de datos, red, archivos ni dormir el hilo.

## RESTRICCIONES (mecánicas, las verifica el script)

- **P1** — Prohibido `assertTrue(true)`, `assertEquals(1, 1)` y cualquier aserción tautológica:
  no es una aserción.
- **P2** — Prohibido `catch` vacío en una prueba. Para excepciones se usa
  `assertThatThrownBy(...)` (AssertJ) o `assertThrows(...)` (JUnit).
- **P3** — Prohibido `Thread.sleep(...)` en pruebas solitarias.
- **P4** — Prohibido `Instant.now()` / `LocalDate.now()` en el cuerpo de una prueba: el tiempo
  entra por `Clock.fixed`.
- **P5** — Prohibida una prueba sin ninguna aserción ni verificación de mock.
- **P6** — Prohibido `@Disabled` sin un comentario que explique la causa y su fecha.
- **P7** — Prohibido `verifyNoMoreInteractions` como red de arrastre: sobreespecifica la
  implementación (usar matchers flexibles antes que verificaciones exhaustivas).

## NOMBRES

- Clase de prueba: `<ClaseBajoPrueba>Test` (solitaria) o `<Escenario>IT` (sociable).
- Método: `escenarioResultado` en camelCase, en español, sin `test` como prefijo
  (`confirmaConConstanciaGuardaYPublicaElEvento`, `falloExigeMotivoYQuedaFallida`).
- `@DisplayName` obligatorio cuando la prueba cubre una historia de usuario: empieza con el
  código (`"HU-4: ..."`). Es la trazabilidad de requisito a evidencia.

## DATOS DE PRUEBA

- Preparación **en línea** dentro de cada prueba. `@BeforeEach` solo para lo que es
  infraestructura del caso de prueba (registros de Resilience4j, `MockMvc`), nunca para armar
  el escenario de negocio.
- La duplicación al construir agregados se resuelve con métodos privados de fábrica dentro de
  la propia clase de prueba (`entregaPendiente()`, `tresParadas()`), no con estado compartido
  entre métodos.

## COBERTURA

- JaCoCo mide **ejecución**, no evidencia: un porcentaje alto con aserciones débiles no vale
  nada. El número es el último artefacto de la cadena, no el objetivo.
- Umbrales que el build hace cumplir (`mvn test -Pcobertura`):
  | Módulo | Línea | Rama |
  |---|---|---|
  | `domain` | ≥ 80 % | ≥ 70 % |
  | `application` | ≥ 80 % | ≥ 70 % |
  | `infrastructure` | sin umbral en esta tarea: buena parte solo se ejerce con Postgres/Rabbit (pruebas `IT`) | — |
- Excluidas del cómputo por no tener comportamiento propio: `record` de DTO/vista, entidades JPA
  (solo getters/setters), clases `@Configuration` y la clase `main` de Spring Boot.
