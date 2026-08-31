# Tarea 1

## 1. Qué pedía la consigna y dónde está resuelto

| Pedido | Resuelto en |
| :--- | :--- |
| Casos de prueba para el microservicio | 75 pruebas nuevas en los 3 módulos con lógica (`domain`, `application`, `infrastructure`) |
| Mostrar mocks | Mockito en aplicación, borde REST, AMQP, Outbox y proyector (detalle en §3) |
| Medir el code coverage | JaCoCo en los 4 módulos + umbrales que hacen fallar el build (`mvn test -Pcobertura`) |
| Continuar con el lenguaje elegido | Java, el mismo del microservicio desde el Módulo 2 |
| `test-writer` con sus skills | `ms-logistic-delivery/.claude/skills/test-writer/` y `.../test-checker/` |
| Entorno validado de pruebas para IA | `testing-rules.md` + `scripts/verificar-pruebas.sh` + `AGENTS.md`, todo dentro del microservicio |

---

## 2. Antes y después

Línea base medida sobre el código tal como quedó del Módulo 3, antes de tocar nada:

| Módulo | Pruebas antes | Pruebas después | Cobertura de línea antes | Después | Rama después |
| :--- | ---: | ---: | ---: | ---: | ---: |
| `domain` | 32 | 38 | 80.6 % | **85.3 %** | 74.3 % |
| `application` | 10 | 18 | 66.6 % | **95.9 %** | 100 % |
| `infrastructure` | 4 | 65 | 5.6 % | **59.5 %** | 47.5 % |
| **Total** | **46** | **121** | | | |

`bootstrap` solo contiene la clase `main` y las clases base de las pruebas de integración: no tiene
código medible propio.

Los umbrales que el build hace cumplir son 80 % de línea y 70 % de rama en `domain` y `application`.
`infrastructure` queda sin umbral a propósito: buena parte de esa capa (repositorios JPA, topología
AMQP) solo se ejerce de verdad contra Postgres y RabbitMQ, y eso son pruebas `*IT` con
Testcontainers, no unitarias.

> La cobertura registra **ejecución**, no evidencia. Por eso el porcentaje va acompañado de qué se
> afirma en cada prueba, y por eso se excluyen del cómputo las clases sin comportamiento propio
> (DTOs, entidades JPA, `@Configuration`, `main`): probarlas subiría el número sin comprar nada.

---

## 3. Qué se probó y con qué dobles

El seam es siempre el mismo que en producción: la inyección por constructor que ya tenía el
proyecto. No se inventó ningún punto de sustitución que solo exista en las pruebas.

| Sujeto bajo prueba | Doble usado | Qué comportamiento se afirma |
| :--- | :--- | :--- |
| `ConsultarHistorialDeEntregasPorPaciente` | mock del puerto `HistorialReadModel` | El día `hasta` se incluye completo; un extremo ausente abre el rango (HU-6) |
| `ConsultarRutaDelRepartidor` | mock de `RutaRepository`; agregado **real** | Cada parada se proyecta con su orden y coordenadas (HU-1) |
| `EntregaController` | mocks de los casos de uso; `RestMapperImpl` **real** | 204/400/404 y traducción de path + cuerpo al comando (HU-4/HU-5) |
| `GeocodingController` | mock de `GeocodificarDireccion` | Proveedor caído ⇒ **503**, no 500 (HU-3) |
| `PublicadorDeEventosOutbox` | mock de `OutboxJpaRepository`; `ObjectMapper` de producción | Una fila por evento, con tipo, `aggregateId` y payload JSON |
| `PublicadorOutboxScheduler` | mocks de `RabbitTemplate` y repositorio; `Clock.fixed` | Si el broker falla, la fila **no** queda marcada: se reintenta (al-menos-una-vez) |
| `ProyectorHistorial` | mocks de repositorio e idempotencia; `Clock.fixed` | Upsert por tipo de evento y **dedupe**: el mismo evento dos veces proyecta una (HU-6) |
| `PaquetesListosListener` | mock de `PlanificarRutaDelDia` | El mensaje de Producción se traduce íntegro al comando (HU-1) |
| `GeocodificadorAcl` | stub del proveedor externo con tasa de fallo | Con el proveedor caído falla explícito; no inventa coordenadas |
| `GeoSupport` | ninguno | X = longitud, Y = latitud, SRID 4326: invertirlos no rompe el compilador, solo mueve las entregas de continente |

Lo que **nunca** se mockea: entidades, agregados y value objects del dominio, los servicios de
dominio puros y los mappers. Se construyen de verdad.

---

## 4. El harness de pruebas para IA

El problema que resuelve: *"escribime pruebas"* no alcanza, y repetir las restricciones en cada
prompt no escala. Las reglas se escriben una vez, en un archivo del proyecto, y las leen tanto quien
genera como quien verifica.

```
ms-logistic-delivery/
├── testing-rules.md                     ← única fuente de las reglas
├── .claude/skills/test-writer/SKILL.md    genera y cita la regla de cada aserción
├── .claude/skills/test-checker/SKILL.md   verifica; nunca dice "listo" si falla
├── scripts/verificar-pruebas.sh           chequeo mecánico + suite + umbrales
└── AGENTS.md                              mismo contrato para opencode / Cursor / Codex
```

`testing-rules.md` fija COMPORTAMIENTO (R1–R6), LÍMITES (qué se mockea y qué no), la separación
solitarias/sociables, RESTRICCIONES mecánicas (P1–P7), NOMBRES, datos de prueba y COBERTURA.

`scripts/verificar-pruebas.sh` es la parte no-negociable: chequea las restricciones sobre las
pruebas solitarias, corre la suite y verifica los umbrales. Devuelve 0 solo si las tres pasan.

**El harness se validó de verdad**, no se dio por bueno. Al estrenarlo encontró dos cosas:

1. Cuatro violaciones **reales** en pruebas de dominio escritas en el Módulo 2 (`Instant.now()`
   dentro de la prueba, restricción P4). Se corrigieron con un instante fijo.
2. Un bug en el propio chequeo: awk POSIX no soporta `\b` ni backreferencias, así que P5 y NOMBRES
   nunca llegaban a evaluarse. Se corrigió y se comprobó contra un archivo con violaciones
   deliberadas, que el script detectó una por una:

```
EjemploViolacionesTest.java:15  NOMBRES  el metodo no debe empezar con "test"
EjemploViolacionesTest.java:16  P4  tiempo no determinista: usar Clock.fixed(...)
EjemploViolacionesTest.java:15  P5  prueba sin ninguna asercion ni verificacion
EjemploViolacionesTest.java:22  P1  asercion tautologica: no afirma nada
EjemploViolacionesTest.java:23  P1  asercion tautologica: los dos lados son lo mismo
EjemploViolacionesTest.java:29  P3  Thread.sleep en prueba solitaria
EjemploViolacionesTest.java:30  P2  catch vacio: usar assertThatThrownBy/assertThrows
EjemploViolacionesTest.java:34  P6  @Disabled sin comentario que explique la causa
```

Un chequeo que nunca falla no verifica nada: había que verlo fallar.

---

## 5. Cómo reproducirlo

```bash
cd proyecto-final-mod-4/ms-logistic-delivery

./scripts/verificar-pruebas.sh            # reglas + suite + cobertura
./scripts/verificar-pruebas.sh --rapido   # solo el chequeo mecánico (segundos)

mvn test                                  # suite + reporte JaCoCo
open domain/target/site/jacoco/index.html # reporte HTML por módulo
```

Salida actual:

```
== 1. Chequeo mecanico contra testing-rules.md ==
Archivos de prueba solitarios analizados: 33
Sin violaciones de las RESTRICCIONES ni de NOMBRES.

== 2. Suite de pruebas solitarias + 3. umbrales de cobertura ==
  Pruebas: 121  ·  fallos: 0  ·  errores: 0  ·  omitidas: 0

== Cobertura por modulo (JaCoCo, linea / rama) ==
  domain           linea  85.3%   rama  74.3%
  application      linea  95.9%   rama 100.0%
  infrastructure   linea  59.5%   rama  47.5%
  bootstrap        (sin clases medibles: todas excluidas)

PASS — reglas, suite y umbrales de cobertura en verde.
```

Nada de esto necesita Docker. Las pruebas sociables (`*IT`, con Testcontainers sobre PostGIS y
RabbitMQ) siguen corriendo aparte con `mvn verify`.

---

## 6. Alcance y qué queda pendiente

El trabajo se aplicó íntegramente sobre `ms-logistic-delivery`, el microservicio del que soy
responsable en el Proyecto Final. Sus cuatro módulos están cubiertos; el detalle de qué falta:

- `infrastructure` está en 59.5 %: falta cubrir los adaptadores JPA de repositorio
  (`EntregaRepositoryJpa`, `RutaRepositoryJpa`) y los mappers JPA, que se prueban mejor como
  pruebas sociables `*IT` contra PostGIS real que como unitarias con dobles.
- Los listeners AMQP de salida (`CateringStubListener`, `NotificacionesStubListener`) son stubs de
  demostración de otros bounded contexts: no tienen lógica propia que afirmar.
- La consigna acepta explícitamente un avance parcial; el criterio para parar fue la regla R2 de
  `testing-rules.md`: no se escribió ninguna prueba de la que no se pudiera decir qué tendría que
  romperse para que fallara.
