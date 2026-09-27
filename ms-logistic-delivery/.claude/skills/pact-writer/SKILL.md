---
name: pact-writer
description: Genera pruebas de contrato Pact entre app-repartidor (consumer) y ms-logistic-delivery (provider) siguiendo contract-testing-rules.md. Úsalo cuando pidan "escribí un pact", "agregá esta interacción al contrato", "la app ahora usa el endpoint X", "falta cubrir este método del cliente" o cuando cambie un endpoint que la app del repartidor consume.
---

# pact-writer

Genera interacciones del contrato **según las reglas escritas del proyecto**, no según criterio
propio. Las reglas viven fuera de este skill a propósito: `pact-checker` lee el mismo archivo, así
que ninguno de los dos puede desviarse por su cuenta.

Un contrato tiene dos lados y este skill escribe **los dos**: la interacción del consumer y el
`@State` que la hace verificable en el provider. Una interacción sin su estado del lado provider es
un contrato que nadie verifica.

Para pruebas solitarias el skill es `test-writer` (`testing-rules.md`); para pruebas de integración,
`integration-test-writer` (`integration-testing-rules.md`).

## Activación

Cuando la app del repartidor empiece a usar un endpoint, cambie lo que manda o lo que lee de una
respuesta, o cuando un método de `ClienteLogistica` no tenga interacción en el contrato.

## Procedimiento

1. **Leé las reglas primero.** `contract-testing-rules.md`, completo, antes de escribir una línea.
   Sin ese archivo en contexto no generes nada: pedilo o detenete.
2. **Partí del cliente, no del controlador.** Leé `ClienteLogistica`: el contrato describe lo que la
   app manda y lo que **lee** (C7). Si la app necesita un campo nuevo, primero va al record del
   cliente; después, al contrato. Nunca copies el DTO del provider entero.
3. **Separá quién controla cada dato** antes de escribir:
   - lo que controla el consumer (ids que la app ya tiene, fechas elegidas, cuerpo de la solicitud)
     va **literal** en la solicitud y, si el provider lo necesita, como parámetro del estado (C5);
   - lo que genera el provider (ids, estados, coordenadas calculadas) va con **matchers** en la
     respuesta y con `pathFromProviderState` si aparece en la solicitud (C6).
4. **Nombrá el estado en lenguaje de negocio** (C8): qué tiene que ser cierto en el servicio, no
   cómo se siembra.
5. **Escribí la interacción** en `ClienteLogisticaPactTest`: un método `@Pact` por interacción, con
   `given` → `uponReceiving` → solicitud → `willRespondWith` (C1, C2). Cuerpos con `LambdaDsl`, nunca
   un string (K3); en la respuesta, solo matchers (K4).
6. **Escribí su `@Test`**: construye `ClienteLogistica` con `mockServer.getUrl()` (C3, K2), llama al
   método real y afirma la **forma** que la app necesita, no los valores del provider (C4).
   `@DisplayName` en lenguaje de negocio.
7. **Escribí el `@State` del provider** en `ContratoAppRepartidorPactIT`, con el **mismo texto** del
   estado (V1). Siembra con los casos de uso reales, sin dobles (V2), con ids únicos para lo que
   genere por su cuenta (V4). Si la solicitud usa un valor del provider, el `@State` lo devuelve en
   un `Map`.
8. **Citá la regla de cada decisión.** En el reporte final, una línea por interacción:
   `metodoPact → C5, C6` (qué se dejó literal, qué va con matcher y por qué).
9. **Ejecutá y verificá.** `./scripts/verificar-contratos.sh`. Regenera el pacto, lo cruza con los
   `@State` y lo verifica contra la aplicación real. Si falla, arreglá antes de reportar.
10. **Invocá `pact-checker`** al terminar, antes de decir que está listo.

## Qué NO generar

- Aserciones sobre valores que decide el provider: cuántas rutas hay, qué id tienen (C4).
- Campos en el contrato que la app no lee: atan al provider sin motivo (C7).
- Ediciones a mano de `pacts/*.json`: el contrato se cambia en la prueba, nunca en el archivo (K1).
- Un `@State` que siembre con SQL o con un `@MockitoBean`: si el estado no se alcanza por la
  aplicación, el contrato promete algo que el servicio no puede cumplir (V2).

## Formato de salida

```
Contrato actualizado
  consumer  ClienteLogisticaPactTest
    <metodoPact>  → C5, C6   (qué pide la app y qué promete el provider, en una línea)
  provider  ContratoAppRepartidorPactIT
    @State("<estado>")  → V2, V4   (cómo lo alcanza con los casos de uso)
Pacto: <N> interacciones, todas con su @State
Verificación del provider: <N> interacciones, <N> fallidas
Verificación: pendiente de pact-checker
```
