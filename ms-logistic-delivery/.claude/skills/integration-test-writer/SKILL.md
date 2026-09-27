---
name: integration-test-writer
description: Genera pruebas de integración (*IT) para ms-logistic-delivery siguiendo integration-testing-rules.md. Úsalo cuando pidan "escribí una prueba de integración", "probá esto contra la base de verdad", "falta cubrir la frontera X" o cuando se agregue un adaptador (REST, AMQP, JPA, ACL) que cruce una frontera del microservicio.
---

# integration-test-writer

Genera pruebas sociables para `ms-logistic-delivery` **según las reglas escritas del proyecto**, no
según criterio propio. Las reglas viven fuera de este skill a propósito: `integration-test-checker`
lee el mismo archivo, así que ninguno de los dos puede desviarse por su cuenta.

Para pruebas solitarias (unitarias) el skill es `test-writer`, que lee `testing-rules.md`. Si lo que
se pide no cruza una frontera, no es trabajo de este skill.

## Activación

Cuando pidan probar la colaboración entre módulos: el borde REST contra PostGIS, el consumidor AMQP,
el Outbox contra RabbitMQ, el proyector contra el read model o el ACL del proveedor de mapas.

## Procedimiento

1. **Leé las reglas primero.** `integration-testing-rules.md`, completo, antes de escribir una
   línea. Sin ese archivo en contexto no generes nada: pedilo o detenete.
2. **Identificá la frontera** en la tabla FRONTERAS. Si el escenario no cruza ninguna, es una prueba
   solitaria: pasala a `test-writer` (regla I1).
3. **Enumerá el par de flujos** antes de codear: el correcto y el incorrecto de esa frontera (I2).
   Un flujo incorrecto es una petición rechazada, un mensaje que no se puede convertir, una
   invariante violada sobre estado ya persistido o una dependencia caída.
4. **Elegí qué se levanta y qué se virtualiza** por la sección LÍMITES. Postgres y RabbitMQ se
   levantan de verdad; el proveedor de mapas se virtualiza con `logistic.maps.stub-failure-rate`.
   Nada de dobles dentro del proceso.
5. **Usá el cableado común**: anotá la clase con `@PruebaDeIntegracion`. Solo declarás
   `@SpringBootTest` propio si necesitás propiedades distintas — y entonces sabé que estás pagando
   un contexto y un par de contenedores más.
6. **Escribí la prueba** en `bootstrap/src/test/java/...`, respetando NOMBRES: clase `<Escenario>IT`
   y `@DisplayName` que empiece con `"HU-n · flujo correcto:"` o `"HU-n · flujo incorrecto:"`.
   Si recorre un flujo de la tabla FLUJOS, etiquetala con su `@Tag` (`flujo-a`, `flujo-b`).
7. **Afirmá sobre lo que cruzó la frontera** (I3): la fila en PostGIS, el mensaje en la cola, el
   `status` + `problem+json`. Lo asincrónico se espera con Awaitility y timeout explícito (I5).
8. **Citá la regla de cada aserción.** En el reporte final, una línea por prueba nueva:
   `ClaseIT#metodo → I<n>` (y `→ Q<n>` si evitaste una restricción no obvia). Sin cita, la prueba no
   se entrega.
9. **Ejecutá y verificá.** `./scripts/verificar-integracion.sh`. Si la suite queda roja o el script
   reporta violaciones, arreglá antes de reportar.
10. **Invocá `integration-test-checker`** al terminar, antes de decir que está listo.

## Qué NO generar

- Pruebas que un mock podría satisfacer: eso es una prueba solitaria (I1, I3).
- Pruebas condicionadas con `@EnabledIf*`: si no corre en `mvn verify`, no es evidencia (Q3).
- Pruebas que dependen del orden o del dato que dejó otra (I4).
- Un flujo correcto sin su flujo incorrecto: la frontera queda a medias (I2).

## Formato de salida

```
Pruebas de integración generadas
  <ruta del archivo>
    <metodo>  → I2, I3   (qué frontera cruza y qué efecto afirma, en una línea)
    ...
Frontera: <n> — <correcto | incorrecto | ambos>
Ejecución: <N> pruebas de integración, <N> fallidas
Verificación: pendiente de integration-test-checker
```
