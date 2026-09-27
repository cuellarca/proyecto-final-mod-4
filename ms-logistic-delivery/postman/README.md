# Colección Postman — ms-logistic-delivery

`ms-logistic-delivery.postman_collection.json` — colección (schema v2.1) con las HU-1…HU-6
agrupadas en dos flujos (entrega exitosa y entrega no concretada) y 6 casos de error.

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

## Los flujos

Las peticiones van numeradas porque el orden importa. Las carpetas se ejecutan de arriba abajo:
el flujo B continúa sobre la ruta que planifica el flujo A.

| Carpeta | Peticiones | HU |
|---|---|---|
| 00 · Salud | Health | — |
| Flujo A · Entrega exitosa | 01 geocodificar · 02 planificar la ruta (2 paquetes) · 03 rutas del repartidor · 04–06 iniciar la ruta, avanzar y completar la parada 1 · 09 confirmar la entrega 1 con constancia · 12–14 historial y constancia | HU-1…HU-4, HU-6 |
| Flujo B · Entrega no concretada | 07–08 avanzar y fallar la parada 2 · 10–11.4 fallo y reintento de la entrega 2 hasta agotar los intentos · 15 historial del paciente 2 con la entrega NO_CONCRETADA | HU-3, HU-5, HU-6 |
| Errores | E1–E6, casos de error `application/problem+json` | — |

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
  Outbox drena cada `OUTBOX_POLL_DELAY_MS` (2 s por defecto). Las peticiones 12, 14 y 15 esperan y
  reintentan hasta 5 veces antes de dar la proyección por fallida.
- **Reintentos del flujo B.** Con `REINTENTOS_MAXIMO=2` (valor por defecto) la entrega se reprograma
  dos veces y el tercer reintento la da por no concretada.
- **Tests incluidos.** Cada petición verifica el código de estado y el contrato de la respuesta;
  el Runner debe terminar sin fallos con el stack recién levantado.

## Desde la línea de comandos

Con [newman](https://github.com/postmanlabs/newman):

```bash
npx newman run postman/ms-logistic-delivery.postman_collection.json
```
