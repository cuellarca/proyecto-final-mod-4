---
name: test-checker
description: Verifica que las pruebas de ms-logistic-delivery cumplan testing-rules.md y que la suite esté verde antes de dar por terminado un trabajo de pruebas. Úsalo siempre después de generar o modificar tests, y cuando pidan "revisá los tests", "esto cumple las reglas?" o antes de entregar/commitear cambios en pruebas.
---

# test-checker

Verifica pruebas contra **el mismo** `testing-rules.md` que usa
`test-writer`. No juzga con criterio propio: juzga con el archivo.

## Activación

Automática al terminar de generar o modificar pruebas, **antes** de reportar "listo". También
a pedido explícito.

## Regla dura

**Nunca digas "listo" si el chequeo falla.** Si hay una sola violación o una prueba roja, el
veredicto es FAIL y el reporte dice qué regla, dónde y cómo se arregla. No se negocia el
veredicto con el usuario ni se lo suaviza.

## Procedimiento

1. Leé `testing-rules.md`.
2. **Chequeo mecánico:** corré `./scripts/verificar-pruebas.sh`.
   Cubre P1–P7, NOMBRES, la suite completa y los umbrales de cobertura.
3. **Juicio sobre lo que la máquina no ve** — leyendo cada prueba nueva o modificada:
   - **R1** ¿una prueba por comportamiento, o una prueba que verifica tres cosas a la vez?
   - **R2** ¿qué tendría que romperse para que esta prueba falle? Si la respuesta es "nada",
     es decoración: reportala.
   - **R3** ¿la aserción es lo último? ¿o hay un `act` después de asertar?
   - **R4** ¿una sola aserción o una sola verificación de mock?
   - **R5** ¿el valor esperado es un literal, o se recalcula con la misma lógica del SUT?
   - **LÍMITES** ¿se mockeó algún objeto de dominio o algún mapper? ¿el seam existe también en
     producción, o se inventó para la prueba?
   - **SOLITARIAS** ¿alguna prueba sin sufijo `IT` toca red, disco, base de datos o duerme?
   - **COBERTURA** ¿alguna prueba existe solo para subir el porcentaje?
4. Reportá.

## Formato de salida

```
test-checker  —  <PASS | FAIL>

Chequeo mecánico:  <N> archivos, <N> violaciones
  <archivo>:<línea>  P<n>  <qué se violó>

Juicio por regla:
  <ClaseTest#metodo>  R<n>  <OK | violación y por qué>

Suite:      <N> pruebas, <N> fallidas
Cobertura:  domain <x> % · application <x> % (umbral 80 %)

Veredicto: <PASS: todas las reglas se cumplen | FAIL: corregir lo anterior antes de entregar>
```
