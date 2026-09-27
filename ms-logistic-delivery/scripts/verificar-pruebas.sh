#!/usr/bin/env bash
#
# verificar-pruebas.sh — entorno validado de pruebas de ms-logistic-delivery.
#
# Hace cumplir testing-rules.md sin depender de que alguien se acuerde de las reglas:
#   1. chequeo mecanico de las RESTRICCIONES (P1-P7) y de NOMBRES sobre las pruebas solitarias
#   2. ejecuta la suite completa de pruebas solitarias (surefire)
#   3. verifica los umbrales de cobertura por modulo (JaCoCo, perfil 'cobertura')
#
# Lo corre la persona que programa, el skill test-writer al terminar y el skill test-checker
# como chequeo mecanico previo a su juicio. Devuelve 0 solo si todo pasa.
#
# Uso:  ./scripts/verificar-pruebas.sh [--rapido]
#         --rapido  omite maven; solo el chequeo mecanico de las reglas
#
set -uo pipefail
cd "$(dirname "$0")/.."

ROJO=$'\033[31m'; VERDE=$'\033[32m'; AMARILLO=$'\033[33m'; NEGRITA=$'\033[1m'; FIN=$'\033[0m'
[ -t 1 ] || { ROJO=""; VERDE=""; AMARILLO=""; NEGRITA=""; FIN=""; }

RAPIDO=0
[ "${1:-}" = "--rapido" ] && RAPIDO=1

echo "${NEGRITA}== 1. Chequeo mecanico contra testing-rules.md ==${FIN}"

if [ ! -f testing-rules.md ]; then
    echo "${ROJO}FALTA testing-rules.md: sin las reglas escritas no hay nada que verificar.${FIN}"
    exit 1
fi

# Pruebas solitarias: *Test.java. Las sociables (*IT.java) las corre failsafe y no pasan por aqui, y
# las de contrato (*PactTest.java) las gobierna contract-testing-rules.md (verificar-contratos.sh).
ARCHIVOS=$(find . -path ./target -prune -o -path '*/src/test/java/*' -name '*Test.java' \
    ! -name '*PactTest.java' -print | sort)
TOTAL_ARCHIVOS=$(printf '%s\n' "$ARCHIVOS" | grep -c . )

VIOLACIONES=$(printf '%s\n' "$ARCHIVOS" | while read -r f; do
    [ -n "$f" ] || continue
    awk -v F="$f" '
        function reportar(linea, regla, detalle) {
            printf "%s:%d  %s  %s\n", F, linea, regla, detalle
        }
        # --- P6: @Disabled sin justificacion ---
        /@Disabled/ && $0 !~ /\/\// && prev !~ /\/\// {
            reportar(NR, "P6", "@Disabled sin comentario que explique la causa")
        }
        # --- P1: aserciones tautologicas ---
        /assertTrue *\( *true *\)|assertFalse *\( *false *\)|assertThat *\( *true *\) *\.isTrue/ {
            reportar(NR, "P1", "asercion tautologica: no afirma nada")
        }
        # assertEquals(x, x) con los dos lados identicos: se compara en codigo (ERE no tiene backreferences)
        /assert(Equals|Same) *\(/ {
            if (match($0, /assert(Equals|Same) *\([^()]*\)/)) {
                args = substr($0, RSTART, RLENGTH)
                sub(/^assert(Equals|Same) *\(/, "", args)
                sub(/\)$/, "", args)
                if (split(args, lado, ",") == 2) {
                    gsub(/^[ \t]+|[ \t]+$/, "", lado[1])
                    gsub(/^[ \t]+|[ \t]+$/, "", lado[2])
                    if (lado[1] == lado[2])
                        reportar(NR, "P1", "asercion tautologica: los dos lados son lo mismo")
                }
            }
        }
        # --- P3: dormir el hilo en una prueba solitaria ---
        /Thread\.sleep *\(/ {
            reportar(NR, "P3", "Thread.sleep en prueba solitaria")
        }
        # --- P4: el tiempo tiene que entrar por Clock ---
        /(Instant|LocalDate|LocalDateTime|LocalTime|ZonedDateTime)\.now *\( *\)/ {
            reportar(NR, "P4", "tiempo no determinista: usar Clock.fixed(...)")
        }
        # --- P7: verificacion de arrastre ---
        /verifyNoMoreInteractions/ {
            reportar(NR, "P7", "verifyNoMoreInteractions sobreespecifica la implementacion")
        }
        # --- SOLITARIAS: una prueba sin sufijo IT no puede cruzar fronteras ---
        /@SpringBootTest|Testcontainers|DriverManager|new +Socket|@DataJpaTest/ {
            reportar(NR, "SOLITARIAS", "cruza una frontera: deberia ser una clase *IT")
        }
        # --- P2: catch vacio (misma linea, o llave de cierre en la siguiente) ---
        /catch *\(.*\) *\{ *\} *$/ {
            reportar(NR, "P2", "catch vacio: usar assertThatThrownBy/assertThrows")
        }
        catch_abierto && /^[[:space:]]*\}/ {
            reportar(catch_linea, "P2", "catch vacio: usar assertThatThrownBy/assertThrows")
        }
        { catch_abierto = 0 }
        /catch *\(.*\) *\{ *$/ { catch_abierto = 1; catch_linea = NR }

        # --- Seguimiento del cuerpo de cada metodo de prueba (P5 y NOMBRES) ---
        /@Test|@ParameterizedTest|@RepeatedTest/ { pendiente = 1 }
        pendiente && /(public|private|protected)? *void +[A-Za-z0-9_]+ *\(/ {
            match($0, /void +[A-Za-z0-9_]+/)
            nombre = substr($0, RSTART + 5, RLENGTH - 5)
            gsub(/ /, "", nombre)
            if (nombre ~ /^test[A-Z0-9_]/)
                reportar(NR, "NOMBRES", "el metodo no debe empezar con \"test\": " nombre)
            pendiente = 0; en_metodo = 1; metodo = nombre; metodo_linea = NR
            afirma = 0; profundidad = 0
        }
        en_metodo {
            # Formas de afirmar aceptadas: AssertJ/JUnit, verificaciones de Mockito y
            # las expectativas de MockMvc (.andExpect / .andExpectAll).
            if ($0 ~ /assert|verify|andExpect|fail *\(/) afirma = 1
            n = gsub(/\{/, "{"); profundidad += n
            n = gsub(/\}/, "}"); profundidad -= n
            if (profundidad <= 0 && NR > metodo_linea - 1 && index($0, "}") > 0) {
                if (!afirma)
                    reportar(metodo_linea, "P5", "prueba sin ninguna asercion ni verificacion: " metodo)
                en_metodo = 0
            }
        }
        { prev = $0 }
    ' "$f"
done)

N_VIOLACIONES=$(printf '%s\n' "$VIOLACIONES" | grep -c . )

echo "Archivos de prueba solitarios analizados: ${TOTAL_ARCHIVOS}"
if [ "$N_VIOLACIONES" -gt 0 ]; then
    echo "${ROJO}Violaciones: ${N_VIOLACIONES}${FIN}"
    printf '%s\n' "$VIOLACIONES" | sed -e 's|^\./||' -e 's|^|  |'
else
    echo "${VERDE}Sin violaciones de las RESTRICCIONES ni de NOMBRES.${FIN}"
fi

if [ "$RAPIDO" = "1" ]; then
    [ "$N_VIOLACIONES" -gt 0 ] && exit 1
    echo "${AMARILLO}(--rapido: no se ejecuto la suite ni la cobertura)${FIN}"
    exit 0
fi

echo
echo "${NEGRITA}== 2. Suite de pruebas solitarias + 3. umbrales de cobertura ==${FIN}"
mkdir -p target
LOG=target/verificacion-pruebas.log
mvn test -Pcobertura > "$LOG" 2>&1
ESTADO_MVN=$?

# Resumen de surefire (una linea de totales por modulo).
awk '/^\[INFO\] Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$/ {
        gsub(/[^0-9,]/, ""); split($0, n, ",")
        corridas += n[1]; fallos += n[2]; errores += n[3]; omitidas += n[4]
     }
     END { printf "  Pruebas: %d  ·  fallos: %d  ·  errores: %d  ·  omitidas: %d\n",
                  corridas, fallos, errores, omitidas }' "$LOG"

if [ "$ESTADO_MVN" -ne 0 ]; then
    echo "${ROJO}  Maven fallo. Detalle:${FIN}"
    grep -E "^\[ERROR\]|<<< (FAILURE|ERROR)!|Rule violated" "$LOG" | head -30 | sed 's|^|    |'
    echo "  Log completo: $LOG"
fi

echo
echo "${NEGRITA}== Cobertura por modulo (JaCoCo, linea / rama) ==${FIN}"
for m in domain application infrastructure bootstrap; do
    csv="$m/target/site/jacoco/jacoco.csv"
    if [ -f "$csv" ]; then
        awk -F, -v M="$m" 'NR>1 {ml+=$8; cl+=$9; mr+=$6; cr+=$7}
            END {
                if (cl + ml == 0) { printf "  %-16s (sin clases medibles: todas excluidas)\n", M; exit }
                printf "  %-16s linea %5.1f%%   rama %5.1f%%\n", M,
                    100*cl/(cl+ml),
                    (cr+mr) ? 100*cr/(cr+mr) : 0
            }' "$csv"
    else
        printf "  %-16s (sin reporte)\n" "$m"
    fi
done
echo "  Reporte HTML: <modulo>/target/site/jacoco/index.html"

echo
if [ "$N_VIOLACIONES" -gt 0 ] || [ "$ESTADO_MVN" -ne 0 ]; then
    echo "${ROJO}${NEGRITA}FAIL${FIN} — corregir antes de entregar (reglas: testing-rules.md)."
    exit 1
fi
echo "${VERDE}${NEGRITA}PASS${FIN} — reglas, suite y umbrales de cobertura en verde."
