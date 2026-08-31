# Colección Postman — ms-logistic-delivery

`ms-logistic-delivery.postman_collection.json` — colección (schema v2.1) con los 14 endpoints del
flujo HU-1…HU-6 y 6 casos de error.

Es un único fichero: las variables (`baseUrl`, `fecha`, `pacienteId`, los ids que se encadenan…)
viven dentro de la colección, así que se importa y funciona contra `localhost:8080` sin configurar
nada. Para apuntar a otro despliegue, editar `baseUrl` en la pestaña **Variables** de la colección.

## Uso

1. Levantar el stack (ver el [README del microservicio](../README.md)):

   ```bash
   docker compose up -d --build
   curl http://localhost:8080/actuator/health   # debe responder UP
   ```

2. En Postman: **Import** → arrastrar el fichero.

3. Ejecutar la colección completa con el **Collection Runner**, o petición por petición **de arriba
   abajo**: cada respuesta alimenta las variables de las siguientes.

## El flujo

Las peticiones van numeradas porque el orden importa: reproducen la misma jornada que `demo/demo.sh`.

| # | Petición | HU |
|---|---|---|
| 00.1 | Health | — |
| 01 | Geocodificar una dirección | HU-2 |
| 02 | Planificar la ruta del día | HU-1/2/3 |
| 03 | Rutas del repartidor | HU-1 |
| 04–08 | Iniciar la ruta, avanzar/completar la parada 1, avanzar/fallar la parada 2 | HU-3 |
| 09 | Confirmar la entrega 1 con su constancia | HU-4 |
| 10–11 | Registrar el fallo de la entrega 2 y reintentarla | HU-5 |
| 12–14 | Historial del paciente (con y sin filtro de fechas) y constancia | HU-6 |
| E1–E6 | Casos de error `application/problem+json` | — |

Estado final de la corrida: parada 1 COMPLETADA + parada 2 FALLIDA ⇒ ruta FINALIZADA, que es
justo lo que comprueba el caso `E5 · Iniciar una ruta ya finalizada → 422`.

## Variables

| Variable | Uso |
|---|---|
| `baseUrl` | Raíz del servicio (`http://localhost:8080`) |
| `repartidorId` | Se regenera en cada ejecución de «02 · Planificar la ruta del día» |
| `fecha` | Fecha de la ruta (ISO `yyyy-MM-dd`) |
| `fechaDesde` / `fechaHasta` | Filtros opcionales del historial |
| `pacienteId`, `pacienteId2`, `paqueteId1`, `paqueteId2` | Datos de la carga a planificar |
| `rutaId`, `entregaId1`, `entregaId2`, `paradaId1`, `paradaId2` | Se capturan automáticamente de las respuestas |

## Detalles que conviene conocer

- **Encadenado por `paqueteId`, no por posición.** La planificación optimiza la secuencia de
  paradas, así que los scripts correlacionan entregas y paradas por `paqueteId` (`pkg-1`, `pkg-2`)
  en vez de fiarse del índice del array.
- **Un repartidor nuevo por ejecución.** La petición 02 genera `rep-postman-<timestamp>`, de modo
  que la colección se puede correr las veces que haga falta sin mezclar rutas de corridas previas.
- **Consistencia eventual en HU-6.** El read model se alimenta de los eventos que el publicador del
  Outbox drena cada `OUTBOX_POLL_DELAY_MS` (2 s por defecto). Las peticiones 12 y 14 esperan y
  reintentan hasta 5 veces antes de dar la proyección por fallida.
- **Tests incluidos.** Cada petición verifica el código de estado y el contrato de la respuesta;
  el Runner debe terminar sin fallos con el stack recién levantado.

## Desde la línea de comandos

Con [newman](https://github.com/postmanlabs/newman):

```bash
npx newman run postman/ms-logistic-delivery.postman_collection.json
```
