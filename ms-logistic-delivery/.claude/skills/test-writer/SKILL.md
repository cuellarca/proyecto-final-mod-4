---
name: test-writer
description: Genera pruebas unitarias para una clase o método de ms-logistic-delivery siguiendo testing-rules.md. Úsalo cuando pidan "escribí pruebas para X", "cubrí con tests esta clase", "faltan pruebas de Y" o cuando se agregue código nuevo al microservicio y haya que probarlo.
---

# test-writer

Genera pruebas para `ms-logistic-delivery` **según las reglas escritas del proyecto**, no según
criterio propio. Las reglas viven fuera de este skill a propósito: `test-checker` lee el mismo
archivo, así que ninguno de los dos puede desviarse por su cuenta.

## Activación

Cuando pidan pruebas para un método, clase o caso de uso del microservicio.

## Procedimiento

1. **Leé las reglas primero.** `testing-rules.md`, completo, antes de
   escribir una línea. Sin ese archivo en contexto no generes nada: pedilo o deténte.
2. **Leé el código bajo prueba** y sus dependencias de constructor. Los constructores dicen
   cuáles son los seams reales; no inventes otros.
3. **Enumerá los comportamientos observables** antes de codear: camino feliz, invariantes
   violadas, bordes (colección vacía, valor nulo opcional, límite de intentos), y efectos sobre
   colaboradores (guardar, publicar, no reprocesar). Un comportamiento = una prueba (R1).
4. **Clasificá cada prueba** como solitaria o sociable según la sección SOLITARIAS vs. SOCIABLES.
   Si el escenario exige Postgres o RabbitMQ, no lo disfraces de unitaria: es un `IT`.
5. **Elegí los dobles** por la sección LÍMITES. Puertos, repositorios Spring Data, `RabbitTemplate`
   y el reloj se mockean; dominio y mappers, jamás.
6. **Escribí las pruebas** en el módulo que corresponde a la clase bajo prueba
   (`<módulo>/src/test/java/...`, mismo paquete que el SUT), respetando NOMBRES y DATOS DE PRUEBA.
7. **Citá la regla de cada aserción.** En el reporte final, una línea por prueba nueva:
   `ClaseTest#metodo → R<n>` (y `→ P<n>` si evitaste una restricción no obvia). Sin cita, la
   prueba no se entrega.
8. **Ejecutá y verificá.** `./scripts/verificar-pruebas.sh`. Si la
   suite queda roja o el script reporta violaciones, arreglá antes de reportar.
9. **Invocá `test-checker`** al terminar, antes de decir que está listo.

## Qué NO generar

- Pruebas de getters, `record` de DTO, entidades JPA o clases `@Configuration`: no tienen
  comportamiento propio (sección COBERTURA).
- Pruebas de métodos privados o de características del framework/librería estándar.
- Pruebas que solo suben el porcentaje de JaCoCo. Si no podés nombrar qué se rompería para que
  falle, no la escribas (R2).

## Formato de salida

```
Pruebas generadas
  <ruta del archivo>
    <metodo>  → R1, R4   (qué comportamiento afirma, en una línea)
    ...
Ejecución: <N> pruebas, <N> fallidas
Cobertura del módulo: <antes> % → <después> % (línea)
Verificación: pendiente de test-checker
```
