# AGENTS.md — cómo trabajar con pruebas en este repositorio

Instrucciones para cualquier agente de IA que edite este proyecto (Claude Code, opencode, Cursor,
Codex y cualquier otro que lea `AGENTS.md`). No duplican las reglas: **apuntan** al único archivo
que las contiene.

## Contrato de pruebas

Las reglas de prueba del microservicio están en **[`testing-rules.md`](testing-rules.md)**.
Es la única fuente: no las repitas en un prompt, no las reinventes por conversación y no crees una
copia en otro archivo.

Antes de escribir o modificar una sola prueba:

1. Leé `testing-rules.md` completo.
2. Escribí las pruebas cumpliéndolo, citando qué regla justifica cada aserción.
3. Verificá con `./scripts/verificar-pruebas.sh`.
4. **No reportes "listo" si ese script no termina en PASS.**

## Skills

En Claude Code el flujo está empaquetado en dos skills que leen ese mismo archivo:

| Skill | Definición | Rol |
|---|---|---|
| `test-writer` | [`.claude/skills/test-writer/SKILL.md`](.claude/skills/test-writer/SKILL.md) | Genera pruebas según las reglas y cita la regla de cada aserción |
| `test-checker` | [`.claude/skills/test-checker/SKILL.md`](.claude/skills/test-checker/SKILL.md) | Verifica contra las mismas reglas; PASS/FAIL, sin negociación |

Un agente sin soporte de skills obtiene el mismo comportamiento leyendo esos dos `SKILL.md` como
instrucciones y ejecutando el script de verificación.

## Entorno validado

`scripts/verificar-pruebas.sh` es la parte no-negociable del harness:

```bash
./scripts/verificar-pruebas.sh            # reglas + suite + umbrales de cobertura
./scripts/verificar-pruebas.sh --rapido   # solo el chequeo mecánico de las reglas
```

Hace tres cosas: chequeo mecánico de las RESTRICCIONES y los NOMBRES sobre las pruebas solitarias,
ejecución de la suite (surefire) y verificación de los umbrales de cobertura de JaCoCo. Devuelve 0
solo si las tres pasan.

## Estructura

Microservicio de logística de entrega (Java 21, Spring Boot, Maven multi-módulo con arquitectura
hexagonal): `domain`, `application`, `infrastructure`, `bootstrap`.

Las pruebas **solitarias** (`*Test.java`) corren con `mvn test` y no necesitan nada levantado. Las
**sociables** (`*IT.java`) corren con `mvn verify` y requieren Docker (Testcontainers: PostGIS y
RabbitMQ).

Cómo levantar el servicio, la API, la demo y la configuración están en [`README.md`](README.md);
no dupliques esas instrucciones aquí.
