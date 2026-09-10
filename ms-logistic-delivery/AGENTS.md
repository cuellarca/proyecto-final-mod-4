# AGENTS.md — cómo trabajar con pruebas en este repositorio

Instrucciones para cualquier agente de IA que edite este proyecto (Claude Code, opencode, Cursor,
Codex y cualquier otro que lea `AGENTS.md`). No duplican las reglas: **apuntan** a los archivos que
las contienen.

## Contrato de pruebas

Las reglas de prueba del microservicio están en dos archivos, uno por tipo de prueba. Son la única
fuente: no las repitas en un prompt, no las reinventes por conversación y no crees una copia en otro
archivo.

| Tipo de prueba | Contrato | Verificador |
|---|---|---|
| Solitaria (`*Test`) | [`testing-rules.md`](testing-rules.md) | `./scripts/verificar-pruebas.sh` |
| Sociable / integración (`*IT`) | [`integration-testing-rules.md`](integration-testing-rules.md) | `./scripts/verificar-integracion.sh` |

Antes de escribir o modificar una sola prueba:

1. Leé completo el contrato que corresponda al tipo de prueba.
2. Escribí las pruebas cumpliéndolo, citando qué regla justifica cada aserción.
3. Verificá con el script de ese contrato.
4. **No reportes "listo" si ese script no termina en PASS.**

## Skills

En Claude Code el flujo está empaquetado en cuatro skills que leen esos mismos archivos:

| Skill | Definición | Rol |
|---|---|---|
| `test-writer` | [`.claude/skills/test-writer/SKILL.md`](.claude/skills/test-writer/SKILL.md) | Genera pruebas solitarias según `testing-rules.md` y cita la regla de cada aserción |
| `test-checker` | [`.claude/skills/test-checker/SKILL.md`](.claude/skills/test-checker/SKILL.md) | Verifica contra las mismas reglas; PASS/FAIL, sin negociación |
| `integration-test-writer` | [`.claude/skills/integration-test-writer/SKILL.md`](.claude/skills/integration-test-writer/SKILL.md) | Genera pruebas de integración según `integration-testing-rules.md` |
| `integration-test-checker` | [`.claude/skills/integration-test-checker/SKILL.md`](.claude/skills/integration-test-checker/SKILL.md) | Verifica los `*IT` contra ese contrato; PASS/FAIL, sin negociación |

Un agente sin soporte de skills obtiene el mismo comportamiento leyendo esos `SKILL.md` como
instrucciones y ejecutando el script de verificación que corresponda.

## Entorno validado

Los dos scripts son la parte no-negociable del harness:

```bash
./scripts/verificar-pruebas.sh              # reglas + suite solitaria + umbrales de cobertura
./scripts/verificar-pruebas.sh --rapido     # solo el chequeo mecánico de las reglas

./scripts/verificar-integracion.sh          # Docker + reglas + matriz frontera×flujo + suite *IT
./scripts/verificar-integracion.sh --rapido # sin Maven: precondiciones y chequeo mecánico
```

`verificar-pruebas.sh` hace tres cosas: chequeo mecánico de las RESTRICCIONES y los NOMBRES sobre las
pruebas solitarias, ejecución de la suite (surefire) y verificación de los umbrales de JaCoCo.

`verificar-integracion.sh` hace cuatro: comprueba que Testcontainers tenga un Docker usable, aplica
Q1–Q6 y NOMBRES sobre los `*IT`, verifica que cada frontera tenga su flujo correcto **y** su flujo
incorrecto, y corre la suite sociable con failsafe. Con `--con-postman` agrega la colección Postman
con newman contra el stack levantado.

Ambos devuelven 0 solo si todos sus pasos pasan.

## Estructura

Microservicio de logística de entrega (Java 21, Spring Boot, Maven multi-módulo con arquitectura
hexagonal): `domain`, `application`, `infrastructure`, `bootstrap`.

Las pruebas **solitarias** (`*Test.java`) corren con `mvn test` y no necesitan nada levantado. Las
**sociables** (`*IT.java`) corren con `mvn verify` y requieren Docker (Testcontainers: PostGIS y
RabbitMQ).

Cómo levantar el servicio, la API, la demo y la configuración están en [`README.md`](README.md);
no dupliques esas instrucciones aquí.
