---
name: pact-checker
description: Verifica que las pruebas de contrato Pact de ms-logistic-delivery (consumer app-repartidor y verificación del provider) cumplan contract-testing-rules.md y que el provider cumpla el pacto antes de dar por terminado un trabajo. Úsalo siempre después de generar o modificar un pact, y cuando pidan "revisá el contrato", "esto rompe a la app?", "¿el pacto está verificado?" o antes de entregar/commitear un cambio en un endpoint que la app consume.
---

# pact-checker

Verifica el contrato contra **el mismo** `contract-testing-rules.md` que usa `pact-writer`. No
juzga con criterio propio: juzga con el archivo.

## Activación

Automática al terminar de generar o modificar interacciones del contrato, **antes** de reportar
"listo". También a pedido explícito, y siempre que cambie un endpoint que la app del repartidor
consume: es la forma de saber si el cambio la rompe.

## Regla dura

**Nunca digas "listo" si el chequeo falla.** Si hay una sola violación, un estado sin su `@State`,
un pacto que no coincide con el que genera el consumer o una interacción que el provider no cumple,
el veredicto es FAIL y el reporte dice qué regla, dónde y cómo se arregla. No se negocia el
veredicto con el usuario ni se lo suaviza.

Si el provider no cumple el pacto, **no se arregla el pacto para que pase**: o el provider volvió a
cumplir lo prometido, o el cambio del contrato lo decide el consumer, en su prueba (K1).

## Procedimiento

1. Leé `contract-testing-rules.md`.
2. **Chequeo mecánico:** corré `./scripts/verificar-contratos.sh`. Cubre K1–K8 y NOMBRES, regenera
   el pacto, cruza cada estado con su `@State`, comprueba que cada método del cliente esté en el
   contrato y verifica el provider contra la aplicación real.
3. **Juicio sobre lo que la máquina no ve**, leyendo cada interacción nueva o modificada:
   - **C4** ¿las aserciones del consumer afirman forma, o se colaron valores que decide el provider?
   - **C5 / C6** ¿cada dato está del lado correcto? Un id del provider escrito literal en la
     respuesta hace que el contrato solo pase con ese id; un dato del consumer con matcher deja de
     comprobar lo que la app realmente manda.
   - **C7** ¿el contrato pide campos que la app no lee? Compará con los records de
     `ClienteLogistica`.
   - **C8** ¿el estado se entiende sin leer el código del provider?
   - **V2** ¿el `@State` alcanza el estado por los casos de uso, o lo fuerza por detrás?
   - **V4** ¿el estado depende de datos que sembró otra prueba, o del orden de ejecución?
4. Reportá.

## Formato de salida

```
pact-checker  —  <PASS | FAIL>

Chequeo mecánico:  <N> consumer, <N> provider, <N> violaciones
  <archivo>:<línea>  K<n>  <qué se violó>

Pacto:  <archivo>  (<consumer> -> <provider>)
  <interacción>  estado "<estado>"  @State <SI|NO>
  K1 regenerado idéntico: <SI|NO>
Cobertura del cliente:  <método>  <SI|NO>

Juicio por regla:
  <metodoPact | @State>  C<n>/V<n>  <OK | violación y por qué>

Verificación del provider:  <N> interacciones, <N> fallidas
  <campo>  esperado <...>  recibido <...>
Veredicto: <PASS: el provider cumple el contrato y las reglas se cumplen | FAIL: corregir lo anterior>
```
