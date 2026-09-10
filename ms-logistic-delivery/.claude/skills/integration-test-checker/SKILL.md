---
name: integration-test-checker
description: Verifica que las pruebas de integración (*IT) de ms-logistic-delivery cumplan integration-testing-rules.md y que la suite sociable esté verde antes de dar por terminado un trabajo. Úsalo siempre después de generar o modificar tests de integración, y cuando pidan "revisá los IT", "esto cumple las reglas de integración?" o antes de entregar/commitear.
---

# integration-test-checker

Verifica pruebas sociables contra **el mismo** `integration-testing-rules.md` que usa
`integration-test-writer`. No juzga con criterio propio: juzga con el archivo.

## Activación

Automática al terminar de generar o modificar pruebas de integración, **antes** de reportar "listo".
También a pedido explícito.

## Regla dura

**Nunca digas "listo" si el chequeo falla.** Si hay una sola violación, una prueba roja o una
frontera sin su par correcto/incorrecto, el veredicto es FAIL y el reporte dice qué regla, dónde y
cómo se arregla. No se negocia el veredicto con el usuario ni se lo suaviza.

## Procedimiento

1. Leé `integration-testing-rules.md`.
2. **Chequeo mecánico:** corré `./scripts/verificar-integracion.sh`. Cubre las precondiciones de
   Docker, Q1–Q6, NOMBRES, la matriz frontera × flujo y la suite de failsafe.
3. **Juicio sobre lo que la máquina no ve** — leyendo cada prueba nueva o modificada:
   - **I1** ¿cruza de verdad una frontera, o es una prueba solitaria disfrazada de `IT`?
   - **I2** ¿la frontera quedó con su flujo correcto **y** su flujo incorrecto?
   - **I3** ¿la aserción es sobre el efecto que cruzó la frontera, o sobre algo que un mock también
     satisfaría? Ésta es la pregunta que más veces encuentra pruebas decorativas.
   - **I4** ¿los datos son propios de la prueba? ¿pasaría si se corriera sola, o dos veces seguidas?
   - **I5** ¿la espera es con Awaitility y timeout explícito?
   - **LÍMITES** ¿aparece algún doble dentro del proceso? ¿se levantó de verdad lo que el contrato
     dice que se levanta?
   - **CONTEXTO** ¿declaró `@SpringBootTest` propio pudiendo usar `@PruebaDeIntegracion`? Cada
     contexto extra es un par de contenedores extra.
4. Reportá.

## Formato de salida

```
integration-test-checker  —  <PASS | FAIL>

Chequeo mecánico:  <N> archivos, <N> violaciones
  <archivo>:<línea>  Q<n>  <qué se violó>

Matriz frontera × flujo:
  <n>. <frontera>  correcto <SI|NO>  incorrecto <SI|NO>

Juicio por regla:
  <ClaseIT#metodo>  I<n>  <OK | violación y por qué>

Suite de integración:  <N> pruebas, <N> fallidas, <N> omitidas
Veredicto: <PASS: todas las reglas se cumplen | FAIL: corregir lo anterior antes de entregar>
```
