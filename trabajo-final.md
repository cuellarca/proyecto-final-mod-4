# Trabajo final — Capa de testing de ms-logistic-delivery

**Alumno:** Carlos Cuellar
**Microservicio:** `ms-logistic-delivery` (Nur-tricenter) · Java 21 · Spring Boot 3.5 · Maven multi-módulo

## 1. Unit tests y cobertura

- 148 pruebas unitarias (JUnit 5, Mockito, AssertJ) en `domain`, `application` e `infrastructure`.
- Cobertura medida con JaCoCo. El build falla si un módulo baja del 80 % de líneas
  (`mvn test -Pcobertura`).

| Módulo | Líneas | Ramas |
|---|---:|---:|
| `domain` | 85,3 % | 74,3 % |
| `application` | 95,9 % | 100 % |
| `infrastructure` | 90,2 % | 63,4 % |
| **Total (reporte agregado)** | **92,3 %** | **70,7 %** |

**Reporte de cobertura:** [`reportes/cobertura/index.html`](reportes/cobertura/index.html)

## 2. Integration tests agrupados por flujo

22 pruebas de integración con Testcontainers (PostGIS y RabbitMQ reales), agrupadas con
`@Tag("flujo-a")` y `@Tag("flujo-b")`:

| Flujo | Recorrido | Prueba de punta a punta | Otras pruebas del flujo |
|---|---|---|---|
| **A · Entrega exitosa** | Producción publica los paquetes → se planifica la ruta → el repartidor la recorre → confirma la entrega con constancia → llega el aviso a ms-notificaciones → el historial la muestra confirmada | `FlujoEntregaExitosaIT` | `BordeRestAmqpIT`, `FlujoPersistenciaOutboxIT`, `OutboxPublicacionIT`, `LecturaReadModelIT`, `PersistenciaRoundTripIT`, `SagaEntregaIT` y el contrato Pact `ContratoAppRepartidorPactIT` |
| **B · Entrega no concretada** | Se planifica la ruta → la parada falla → la entrega se reintenta hasta agotar los intentos → llega la reprogramación a ms-catering → el historial la muestra no concretada | `FlujoEntregaNoConcretadaIT` | `PersistenciaRoundTripIT`, `SagaEntregaIT` |

Los mismos dos flujos están en la colección Postman
(`ms-logistic-delivery/postman/`), en las carpetas *Flujo A* y *Flujo B*, más una carpeta de errores.

## 3. Skills y reglas usadas

| Tipo de prueba | Reglas | Skills | Script de verificación |
|---|---|---|---|
| Unitarias | `testing-rules.md` | `test-writer`, `test-checker` | `scripts/verificar-pruebas.sh` |
| Integración | `integration-testing-rules.md` | `integration-test-writer`, `integration-test-checker` | `scripts/verificar-integracion.sh` |
| Contrato (Pact) | `contract-testing-rules.md` | `pact-writer`, `pact-checker` | `scripts/verificar-contratos.sh` |

Todo está en `ms-logistic-delivery/`. Los skills están en `.claude/skills/`, y `AGENTS.md` indica a
cualquier agente de IA qué reglas y qué script usar.

## 4. Cómo ejecutarlo

Desde `ms-logistic-delivery` (las pruebas de integración necesitan Docker):

```bash
./scripts/verificar-pruebas.sh --publicar-reporte   # unit tests + umbral 80 % + actualiza reportes/cobertura
./scripts/verificar-integracion.sh                  # integration tests + matriz por flujo
./scripts/verificar-contratos.sh                    # contrato Pact (consumer y provider)

mvn verify -Dgroups=flujo-a                         # solo el flujo A (o flujo-b)

docker compose up -d --build                        # levantar el servicio para Postman
npx newman run postman/ms-logistic-delivery.postman_collection.json
```

Resultado actual:

| Script | Resultado |
|---|---|
| `verificar-pruebas.sh` | PASS · 148 unitarias, 0 fallos, cobertura total 92,3 % |
| `verificar-integracion.sh` | PASS · 22 de integración, 0 fallos · flujo A 8/8 clases y flujo B 3/3 en verde |
| `verificar-contratos.sh` | PASS · 2 interacciones verificadas |
| newman | 26 requests, 48 aserciones, 0 fallos |
