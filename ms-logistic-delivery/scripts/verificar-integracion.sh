#!/usr/bin/env bash
#
# verificar-integracion.sh — entorno validado de pruebas de integracion de ms-logistic-delivery.
#
# Hace cumplir integration-testing-rules.md sin depender de que alguien se acuerde de las reglas:
#   1. precondiciones: un entorno Docker que Testcontainers pueda usar
#   2. chequeo mecanico de las RESTRICCIONES (Q1-Q6), NOMBRES y la matriz frontera x flujo
#   3. ejecuta la suite de pruebas sociables (failsafe, mvn verify)
#   4. opcional: la coleccion Postman contra el stack levantado (newman)
#
# Lo corre la persona que programa, el skill integration-test-writer al terminar y el skill
# integration-test-checker como chequeo mecanico previo a su juicio. Devuelve 0 solo si todo pasa.
#
# Uso:  ./scripts/verificar-integracion.sh [--rapido] [--con-postman]
#         --rapido        omite maven; solo precondiciones y chequeo mecanico
#         --con-postman   ademas corre newman contra http://localhost:8080 (hay que levantarlo antes)
#
set -uo pipefail
cd "$(dirname "$0")/.."

ROJO=$'\033[31m'; VERDE=$'\033[32m'; AMARILLO=$'\033[33m'; NEGRITA=$'\033[1m'; FIN=$'\033[0m'
[ -t 1 ] || { ROJO=""; VERDE=""; AMARILLO=""; NEGRITA=""; FIN=""; }

REGLAS=integration-testing-rules.md
RAPIDO=0
POSTMAN=0
for arg in "$@"; do
    case "$arg" in
        --rapido) RAPIDO=1 ;;
        --con-postman) POSTMAN=1 ;;
        *) echo "Opcion desconocida: $arg"; exit 2 ;;
    esac
done

if [ ! -f "$REGLAS" ]; then
    echo "${ROJO}FALTA $REGLAS: sin las reglas escritas no hay nada que verificar.${FIN}"
    exit 1
fi

# --- 1. Precondiciones -------------------------------------------------------------------------
echo "${NEGRITA}== 1. Precondiciones ==${FIN}"
if ! docker info > /dev/null 2>&1; then
    echo "${ROJO}Docker no responde. Testcontainers necesita un daemon accesible.${FIN}"
    exit 1
fi
echo "  Docker ${VERDE}OK${FIN} (Engine $(docker version --format '{{.Server.Version}}' 2>/dev/null))"

# --- 2. Chequeo mecanico -----------------------------------------------------------------------
echo
echo "${NEGRITA}== 2. Chequeo mecanico contra $REGLAS ==${FIN}"

# Pruebas sociables: *IT.java y las clases base que comparten (*Tests.java).
ARCHIVOS=$(find . -path ./target -prune -o -path '*/src/test/java/*' \
    \( -name '*IT.java' -o -name '*Tests.java' \) -print | sort)
TOTAL_ARCHIVOS=$(printf '%s\n' "$ARCHIVOS" | grep -c . )

VIOLACIONES=$(printf '%s\n' "$ARCHIVOS" | while read -r f; do
    [ -n "$f" ] || continue
    awk -v F="$f" '
        function reportar(linea, regla, detalle) {
            printf "%s:%d  %s  %s\n", F, linea, regla, detalle
        }
        /Thread\.sleep *\(/                { reportar(NR, "Q1", "Thread.sleep: la espera va con Awaitility") }
        /@Disabled/ && $0 !~ /\/\// && prev !~ /\/\// {
                                             reportar(NR, "Q2", "@Disabled sin comentario que explique la causa") }
        /@EnabledIf/                       { reportar(NR, "Q3", "prueba condicionada: no corre en mvn verify, no es evidencia") }
        /localhost:(5432|5435|5672|15672)|"localhost"/ {
                                             reportar(NR, "Q4", "host/puerto del stack hardcodeado: usar @ServiceConnection") }
        /@MockitoBean|@MockBean/           { reportar(NR, "LIMITES", "doble dentro del proceso: deja de ser prueba de integracion") }
        /@DirtiesContext/                  { reportar(NR, "Q6", "@DirtiesContext reinicia los contenedores: aislar por dato (I4)") }

        # Cuerpo de cada metodo de prueba: Q5 (sin asercion), NOMBRES y @DisplayName obligatorio.
        /@DisplayName/                     { tiene_nombre = 1 }
        /@Test|@ParameterizedTest|@RepeatedTest/ { pendiente = 1 }
        pendiente && /(public|private|protected)? *void +[A-Za-z0-9_]+ *\(/ {
            match($0, /void +[A-Za-z0-9_]+/)
            nombre = substr($0, RSTART + 5, RLENGTH - 5)
            gsub(/ /, "", nombre)
            if (nombre ~ /^test[A-Z0-9_]/)
                reportar(NR, "NOMBRES", "el metodo no debe empezar con \"test\": " nombre)
            if (!tiene_nombre)
                reportar(NR, "NOMBRES", "prueba sin @DisplayName \"HU-n - flujo correcto/incorrecto\": " nombre)
            pendiente = 0; tiene_nombre = 0; en_metodo = 1; metodo = nombre; metodo_linea = NR
            afirma = 0; profundidad = 0
        }
        en_metodo {
            if ($0 ~ /assert|verify|andExpect|fail *\(/) afirma = 1
            n = gsub(/\{/, "{"); profundidad += n
            n = gsub(/\}/, "}"); profundidad -= n
            if (profundidad <= 0 && NR > metodo_linea - 1 && index($0, "}") > 0) {
                if (!afirma)
                    reportar(metodo_linea, "Q5", "prueba sin ninguna asercion: " metodo)
                en_metodo = 0
            }
        }
        { prev = $0 }
    ' "$f"
done)

N_VIOLACIONES=$(printf '%s\n' "$VIOLACIONES" | grep -c . )

echo "Archivos de prueba sociables analizados: ${TOTAL_ARCHIVOS}"
if [ "$N_VIOLACIONES" -gt 0 ]; then
    echo "${ROJO}Violaciones: ${N_VIOLACIONES}${FIN}"
    printf '%s\n' "$VIOLACIONES" | sed -e 's|^\./||' -e 's|^|  |'
else
    echo "${VERDE}Sin violaciones de las RESTRICCIONES ni de NOMBRES.${FIN}"
fi

# --- Matriz frontera x flujo -------------------------------------------------------------------
# Las fronteras y sus clases salen de la tabla FRONTERAS del propio archivo de reglas: la doc es la
# fuente, no una copia dentro del script.
echo
echo "${NEGRITA}== Matriz frontera x flujo (seccion FRONTERAS de $REGLAS) ==${FIN}"

flujos_de_clase() {  # imprime los @DisplayName de la clase y los de su clase base, si extiende una
    local clase="$1" archivo base
    archivo=$(printf '%s\n' "$ARCHIVOS" | grep -E "/${clase}\.java$" | head -1)
    [ -n "$archivo" ] || return 0
    grep -h '@DisplayName' "$archivo"
    base=$(grep -oE "class ${clase} extends [A-Za-z0-9_]+" "$archivo" | awk '{print $4}')
    if [ -n "$base" ]; then
        archivo=$(printf '%s\n' "$ARCHIVOS" | grep -E "/${base}\.java$" | head -1)
        [ -n "$archivo" ] && grep -h '@DisplayName' "$archivo"
    fi
}

FALTANTES=0
while IFS='|' read -r _ numero frontera clases _; do
    numero=$(echo "$numero" | tr -d ' ')
    case "$numero" in ''|*[!0-9]*) continue ;; esac
    frontera=$(echo "$frontera" | sed -e 's/^ *//' -e 's/ *$//' -e 's/`//g')
    nombres=$(echo "$clases" | grep -oE '`[A-Za-z0-9_]+IT`' | tr -d '`')

    correcto="  "; incorrecto="  "
    todos=""
    for c in $nombres; do todos="$todos$(flujos_de_clase "$c")"$'\n'; done
    printf '%s' "$todos" | grep -q 'flujo correcto'   && correcto="SI"
    printf '%s' "$todos" | grep -q 'flujo incorrecto' && incorrecto="SI"
    [ "$correcto" = "SI" ] && [ "$incorrecto" = "SI" ] || FALTANTES=$((FALTANTES + 1))

    printf "  %s. %-44s  correcto %-3s incorrecto %s\n" "$numero" "$frontera" "$correcto" "$incorrecto"
done < "$REGLAS"

if [ "$FALTANTES" -gt 0 ]; then
    echo "${ROJO}  ${FALTANTES} frontera(s) sin el par correcto/incorrecto que exige la regla I2.${FIN}"
fi

# --- 3. Suite de pruebas sociables -------------------------------------------------------------
ESTADO_MVN=0
if [ "$RAPIDO" = "0" ]; then
echo
echo "${NEGRITA}== 3. Suite de pruebas sociables (failsafe) ==${FIN}"
mkdir -p target
LOG=target/verificacion-integracion.log
mvn verify > "$LOG" 2>&1
ESTADO_MVN=$?

awk '/failsafe:.*:integration-test/ { en_it = 1 }
     en_it && /^\[(INFO|ERROR)\] Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$/ {
        gsub(/[^0-9,]/, ""); split($0, n, ",")
        corridas += n[1]; fallos += n[2]; errores += n[3]; omitidas += n[4]
     }
     END { printf "  Pruebas de integracion: %d  ·  fallos: %d  ·  errores: %d  ·  omitidas: %d\n",
                  corridas, fallos, errores, omitidas }' "$LOG"

if [ "$ESTADO_MVN" -ne 0 ]; then
    echo "${ROJO}  Maven fallo. Detalle:${FIN}"
    grep -E "^\[ERROR\]|<<< (FAILURE|ERROR)!" "$LOG" | head -30 | sed 's|^|    |'
    echo "  Log completo: $LOG"
fi
fi

# --- 4. Coleccion Postman (opcional) -----------------------------------------------------------
ESTADO_POSTMAN=0
if [ "$POSTMAN" = "1" ]; then
    echo
    echo "${NEGRITA}== 4. Coleccion Postman (newman) ==${FIN}"
    if ! curl -sf http://localhost:8080/actuator/health > /dev/null; then
        echo "${ROJO}  El servicio no responde en http://localhost:8080 (docker compose up -d).${FIN}"
        ESTADO_POSTMAN=1
    else
        npx --yes newman run postman/ms-logistic-delivery.postman_collection.json \
            > target/verificacion-postman.log 2>&1
        ESTADO_POSTMAN=$?
        grep -E "(requests|assertions|test-scripts)[^a-z]" target/verificacion-postman.log \
            | tr -d '│' | awk '{ printf "  %-14s ejecutadas %-4s fallidas %s\n", $1, $2, $3 }'
        [ "$ESTADO_POSTMAN" -ne 0 ] && echo "  Log completo: target/verificacion-postman.log"
    fi
fi

echo
if [ "$N_VIOLACIONES" -gt 0 ] || [ "$FALTANTES" -gt 0 ] || [ "$ESTADO_MVN" -ne 0 ] || [ "$ESTADO_POSTMAN" -ne 0 ]; then
    echo "${ROJO}${NEGRITA}FAIL${FIN} — corregir antes de entregar (reglas: $REGLAS)."
    exit 1
fi
if [ "$RAPIDO" = "1" ]; then
    echo "${VERDE}${NEGRITA}PASS${FIN} — precondiciones, reglas y matriz en verde."
    echo "${AMARILLO}(--rapido: la suite de failsafe no se ejecuto)${FIN}"
else
    echo "${VERDE}${NEGRITA}PASS${FIN} — precondiciones, reglas, matriz y suite de integracion en verde."
fi
