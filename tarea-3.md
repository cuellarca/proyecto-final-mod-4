# Tarea 3 — Contract testing con Pact

**Alumno:** Carlos Cuellar
**Microservicio:** `ms-logistic-delivery` (Nur-tricenter)
**Herramienta:** Pact JVM 4.6.21 (especificación V3), con Java 21 y Spring Boot 3.5

## Qué hice

Apliqué contract testing entre la **app del repartidor** (consumidor) y mi microservicio
`ms-logistic-delivery` (provider). Elegí la app del repartidor porque, dentro del caso de estudio, es
la que usa la API de logística: consulta su ruta del día y confirma cada entrega.

El consumidor está en el módulo `consumidor-app-repartidor`. Es un cliente HTTP (`ClienteLogistica`)
con sus pruebas Pact. No depende del código del microservicio: solo conoce la API a través del
contrato.

## Las dos solicitudes del contrato

| # | Solicitud | Estado que prepara el provider | Respuesta esperada |
|---|---|---|---|
| 1 | `GET /api/v1/repartidores/{id}/rutas?fecha=...` | el repartidor tiene una ruta planificada para la fecha | `200` con la lista de rutas y sus paradas |
| 2 | `POST /api/v1/entregas/{id}/confirmacion` | hay una entrega pendiente de confirmar | `204` sin cuerpo |

Detalles de la implementación:

- En la respuesta usé matchers (`uuid`, `date`, `minArrayLike`, etc.) en lugar de valores fijos,
  porque los ids y los estados los decide el provider.
- El id de la entrega lo genera el servicio, así que en la solicitud 2 usé `pathFromProviderState`:
  el `@State` del provider devuelve el id real y Pact lo pone en la URL.
- El contrato solo incluye los campos que la app realmente usa.

## Cómo funciona

1. `ClienteLogisticaPactTest` (consumer) corre contra el mock server de Pact y genera el contrato en
   `ms-logistic-delivery/pacts/app-repartidor-ms-logistic-delivery.json`.
2. `ContratoAppRepartidorPactIT` (provider) lee ese archivo y reproduce cada solicitud contra la
   aplicación real, con PostGIS y RabbitMQ levantados con Testcontainers. Los `@State` preparan los
   datos usando los casos de uso del servicio.

Los dos pasos corren en un solo `mvn verify`. En un entorno real el contrato se compartiría con un
Pact Broker; acá lo dejé en la carpeta `pacts/` del mismo repositorio.

Resultado de la verificación en el provider:

```
Verifying a pact between app-repartidor and ms-logistic-delivery
  Given el repartidor tiene una ruta planificada para la fecha
    returns a response which
      has status code 200 (OK)
      has a matching body (OK)
Verifying a pact between app-repartidor and ms-logistic-delivery
  Given hay una entrega pendiente de confirmar
    returns a response which
      has status code 204 (OK)
      has a matching body (OK)
```

## pact-writer, skills y entorno validado para IA

Seguí el mismo esquema de las tareas 1 y 2:

| Archivo | Para qué sirve |
|---|---|
| `contract-testing-rules.md` | Reglas del contrato: cómo escribir las interacciones, los estados y qué está prohibido |
| `.claude/skills/pact-writer/` | Skill que escribe las interacciones del consumer y su `@State` en el provider |
| `.claude/skills/pact-checker/` | Skill que revisa el contrato contra las reglas y da PASS o FAIL |
| `scripts/verificar-contratos.sh` | Script que revisa las reglas, regenera el pacto y lo verifica contra el provider |
| `AGENTS.md` | Indica a cualquier agente de IA qué reglas y qué script usar |

Para comprobar que el script realmente detecta errores, lo hice fallar a propósito:

- Con un archivo de violaciones deliberadas (`EjemploViolacionesPactTest.java.ejemplo` y
  `EjemploViolacionesPactIT.java.ejemplo`): detectó las 14.
- Renombrando un `@State` en el provider: marcó el estado sin verificar.
- Editando el pacto a mano: lo detectó y lo regeneró.
- Haciendo que el servicio devolviera el estado de la ruta en minúsculas: la verificación falló con
  `$[0].estado Expected 'planificada' to match 'PLANIFICADA|EN_CURSO|FINALIZADA'`. Es un cambio que
  no rompe ninguna otra prueba del servicio, pero sí rompe a la app.

## Cómo ejecutarlo

Desde `ms-logistic-delivery` (necesita Docker):

```bash
./scripts/verificar-contratos.sh            # verificación completa
./scripts/verificar-contratos.sh --rapido   # solo reglas y pacto, sin Maven
```

Salida:

```
== 3. Consumer: pruebas Pact y regeneracion del pacto ==
  Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
  K1: el pacto en disco es identico al que genera el consumer OK

== 4. El pacto: interacciones, estados y cobertura del cliente ==
  app-repartidor-ms-logistic-delivery.json  (app-repartidor -> ms-logistic-delivery)
    - una consulta de las rutas del dia del repartidor     @State SI
    - la confirmacion de una entrega con su constancia     @State SI
  ClienteLogistica.rutasDelDia()                       cubierto SI
  ClienteLogistica.confirmarEntrega()                  cubierto SI

== 5. Provider: verificacion contra la aplicacion real (failsafe) ==
  Interacciones verificadas: 2  ·  fallos: 0  ·  errores: 0  ·  omitidas: 0

PASS — reglas, pacto regenerado y verificacion del provider en verde.
```

Las pruebas de las tareas anteriores siguen pasando: `verificar-pruebas.sh` y
`verificar-integracion.sh` dan PASS.

## Otros cambios

- En el `Dockerfile` agregué el pom del módulo consumidor, porque sin él la imagen no compilaba.
  La imagen sigue incluyendo solo el servicio.
- Desactivé la telemetría de Pact (`pact_do_not_track`) para que las pruebas no hagan llamadas
  externas.

## Qué quedó pendiente

- No usé Pact Broker ni CI.
- El contrato solo tiene casos de éxito. Los casos de error ya los cubren las pruebas de integración
  de la tarea 2.
- Los otros servicios del caso (notificaciones, catering) se comunican por mensajes, así que sus
  contratos serían con Pact Messages.
