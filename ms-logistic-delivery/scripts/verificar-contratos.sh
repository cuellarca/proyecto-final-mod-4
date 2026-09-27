#!/usr/bin/env bash
#
# verificar-contratos.sh — entorno validado de las pruebas de contrato (Pact) de ms-logistic-delivery.
#
# Hace cumplir contract-testing-rules.md sin depender de que alguien se acuerde de las reglas:
#   1. precondiciones: las reglas, jq y (salvo --rapido) un Docker que Testcontainers pueda usar
#   2. chequeo mecanico de las RESTRICCIONES (K2-K7) y NOMBRES sobre *PactTest y *PactIT
#   3. consumer: regenera el pacto y comprueba que no difiera del que habia en disco (K1)
#   4. el pacto: >= 2 interacciones, cada estado con su @State (K8) y cada metodo del cliente cubierto
#   5. provider: verifica el pacto contra la aplicacion real (failsafe, Testcontainers)
#
# Lo corre la persona que programa, el skill pact-writer al terminar y el skill pact-checker como
# chequeo mecanico previo a su juicio. Devuelve 0 solo si todo pasa.
#
# Uso:  ./scripts/verificar-contratos.sh [--rapido]
#         --rapido  omite maven: solo el chequeo mecanico y el analisis del pacto que ya esta en disco
#
set -uo pipefail
cd "$(dirname "$0")/.."

ROJO=$'\033[31m'; VERDE=$'\033[32m'; AMARILLO=$'\033[33m'; NEGRITA=$'\033[1m'; FIN=$'\033[0m'
[ -t 1 ] || { ROJO=""; VERDE=""; AMARILLO=""; NEGRITA=""; FIN=""; }

REGLAS=contract-testing-rules.md
CONSUMIDOR=consumidor-app-repartidor
PACTOS=pacts
RAPIDO=0
for arg in "$@"; do
    case "$arg" in
        --rapido) RAPIDO=1 ;;
        *) echo "Opcion desconocida: $arg"; exit 2 ;;
    esac
done

# --- 1. Precondiciones -------------------------------------------------------------------------
echo "${NEGRITA}== 1. Precondiciones ==${FIN}"
if [ ! -f "$REGLAS" ]; then
    echo "${ROJO}FALTA $REGLAS: sin las reglas escritas no hay nada que verificar.${FIN}"
    exit 1
fi
if ! command -v jq > /dev/null; then
    echo "${ROJO}Falta jq: se usa para leer el pacto generado.${FIN}"
    exit 1
fi
echo "  $REGLAS y jq ${VERDE}OK${FIN}"
if [ "$RAPIDO" = "0" ]; then
    if ! docker info > /dev/null 2>&1; then
        echo "${ROJO}Docker no responde. La verificacion del provider corre sobre Testcontainers.${FIN}"
        exit 1
    fi
    echo "  Docker ${VERDE}OK${FIN} (Engine $(docker version --format '{{.Server.Version}}' 2>/dev/null))"
fi

# --- 2. Chequeo mecanico -----------------------------------------------------------------------
echo
echo "${NEGRITA}== 2. Chequeo mecanico contra $REGLAS ==${FIN}"

CONSUMER=$(find "$CONSUMIDOR" -path '*/target' -prune -o -path '*/src/test/java/*' -name '*PactTest.java' -print | sort)
PROVIDER=$(find bootstrap -path '*/target' -prune -o -path '*/src/test/java/*' -name '*PactIT.java' -print | sort)
N_CONSUMER=$(printf '%s\n' "$CONSUMER" | grep -c . )
N_PROVIDER=$(printf '%s\n' "$PROVIDER" | grep -c . )

VIOLACIONES_CONSUMER=$(printf '%s\n' "$CONSUMER" | while read -r f; do
    [ -n "$f" ] || continue
    awk -v F="$f" '
        function reportar(linea, regla, detalle) {
            printf "%s:%d  %s  %s\n", F, linea, regla, detalle
        }
        function cerrar_pact() {
            if (!givens)   reportar(pact_linea, "K5", "interaccion sin estado: falta .given(...) en " pact)
            if (ups != 1)  reportar(pact_linea, "K5", ups " uponReceiving en " pact ": una interaccion por @Pact")
            en_pact = 0
        }
        /http:\/\/localhost|"localhost"/   { reportar(NR, "K2", "URL hardcodeada: el cliente va contra mockServer.getUrl()") }
        /\.body\( *"/                      { reportar(NR, "K3", "cuerpo como string literal: usar LambdaDsl") }
        /@Disabled/ && $0 !~ /\/\// && prev !~ /\/\// {
                                             reportar(NR, "K6", "@Disabled sin comentario que explique la causa") }

        # --- Metodos @Pact: estado, una interaccion y respuesta con matchers ---
        /@Pact\(/                          { if (en_pact) cerrar_pact()
                                             en_pact = 1; pact_linea = NR; pact = "?"; givens = 0; ups = 0; en_resp = 0 }
        en_pact && pact == "?" && /[A-Za-z0-9_]+ *\( *PactDsl/ {
            match($0, /[A-Za-z0-9_]+ *\( *PactDsl/); pact = substr($0, RSTART, RLENGTH)
            sub(/ *\(.*/, "", pact)
        }
        en_pact && /\.given\(/             { givens++ }
        en_pact && /uponReceiving\(/       { ups++ }
        en_pact && /willRespondWith\(/     { en_resp = 1 }
        en_pact && en_resp && /(stringValue|numberValue|booleanValue) *\(/ {
                                             reportar(NR, "K4", "valor fijo en la respuesta: la respuesta va con matchers (C6)") }
        en_pact && /\.toPact\(/            { cerrar_pact() }

        # --- Metodos @Test: nombre, @DisplayName, asercion y cliente contra el mock server ---
        /@DisplayName/                     { tiene_nombre = 1 }
        /@Test/                            { pendiente = 1 }
        pendiente && /void +[A-Za-z0-9_]+ *\(/ {
            match($0, /void +[A-Za-z0-9_]+/)
            nombre = substr($0, RSTART + 5, RLENGTH - 5)
            gsub(/ /, "", nombre)
            if (nombre ~ /^test[A-Z0-9_]/)
                reportar(NR, "NOMBRES", "el metodo no debe empezar con \"test\": " nombre)
            if (!tiene_nombre)
                reportar(NR, "NOMBRES", "prueba sin @DisplayName en lenguaje de negocio: " nombre)
            pendiente = 0; tiene_nombre = 0; en_metodo = 1; metodo = nombre; metodo_linea = NR
            afirma = 0; usa_mock = 0; profundidad = 0
        }
        en_metodo {
            if ($0 ~ /assert|verify|fail *\(/) afirma = 1
            if ($0 ~ /mockServer\.getUrl\(\)/) usa_mock = 1
            n = gsub(/\{/, "{"); profundidad += n
            n = gsub(/\}/, "}"); profundidad -= n
            if (profundidad <= 0 && index($0, "}") > 0) {
                if (!afirma)   reportar(metodo_linea, "K6", "prueba sin ninguna asercion: " metodo)
                if (!usa_mock) reportar(metodo_linea, "K2", "el cliente no se construye con mockServer.getUrl(): " metodo)
                en_metodo = 0
            }
        }
        { prev = $0 }
        END { if (en_pact) cerrar_pact() }
    ' "$f"
done)

VIOLACIONES_PROVIDER=$(printf '%s\n' "$PROVIDER" | while read -r f; do
    [ -n "$f" ] || continue
    case "$(basename "$f")" in
        Contrato*PactIT.java) ;;
        *) echo "$f:1  NOMBRES  la verificacion del provider se llama Contrato<Consumidor>PactIT" ;;
    esac
    awk -v F="$f" '
        function reportar(linea, regla, detalle) {
            printf "%s:%d  %s  %s\n", F, linea, regla, detalle
        }
        /@PactFolder/                      { carpeta = 1
                                             if ($0 !~ /@PactFolder\( *"\.\.\/pacts" *\)/)
                                                 reportar(NR, "K7", "el pacto se lee de @PactFolder(\"../pacts\")") }
        /@MockitoBean|@MockBean/           { reportar(NR, "K7", "doble dentro del proceso: el provider se verifica real (V2)") }
        /@Disabled/ && $0 !~ /\/\// && prev !~ /\/\// {
                                             reportar(NR, "K6", "@Disabled sin comentario que explique la causa") }
        /@DisplayName/                     { nombre_linea = $0 }
        /@TestTemplate/                    { plantilla = NR }
        plantilla && /void +[A-Za-z0-9_]+ *\(/ {
            if (nombre_linea !~ /HU-[0-9].*flujo correcto/)
                reportar(NR, "NOMBRES", "@DisplayName con el formato de integracion: \"HU-n · flujo correcto: ...\"")
            plantilla = 0; nombre_linea = ""
        }
        { prev = $0 }
        END { if (!carpeta) reportar(1, "K7", "falta @PactFolder(\"../pacts\")") }
    ' "$f"
done)

VIOLACIONES=$(printf '%s\n%s\n' "$VIOLACIONES_CONSUMER" "$VIOLACIONES_PROVIDER" | grep .)
N_VIOLACIONES=$(printf '%s\n' "$VIOLACIONES" | grep -c . )

echo "Pruebas analizadas: ${N_CONSUMER} consumer (*PactTest) · ${N_PROVIDER} provider (*PactIT)"
if [ "$N_CONSUMER" -eq 0 ] || [ "$N_PROVIDER" -eq 0 ]; then
    echo "${ROJO}  Falta uno de los dos lados del contrato.${FIN}"
    N_VIOLACIONES=$((N_VIOLACIONES + 1))
fi
if [ -n "$VIOLACIONES" ]; then
    echo "${ROJO}Violaciones: $(printf '%s\n' "$VIOLACIONES" | grep -c .)${FIN}"
    printf '%s\n' "$VIOLACIONES" | sed -e 's|^\./||' -e 's|^|  |'
else
    echo "${VERDE}Sin violaciones de las RESTRICCIONES ni de NOMBRES.${FIN}"
fi

# --- 3. Consumer: regenerar el pacto -----------------------------------------------------------
ESTADO_CONSUMER=0
DIFIERE=0
mkdir -p target
if [ "$RAPIDO" = "0" ]; then
    echo
    echo "${NEGRITA}== 3. Consumer: pruebas Pact y regeneracion del pacto ==${FIN}"
    ANTES=$(cat "$PACTOS"/*.json 2>/dev/null | shasum | awk '{print $1}')
    LOG_CONSUMER=target/verificacion-contratos-consumer.log
    mvn -pl "$CONSUMIDOR" -am test -Dtest='*PactTest' -Dsurefire.failIfNoSpecifiedTests=false \
        > "$LOG_CONSUMER" 2>&1
    ESTADO_CONSUMER=$?
    grep -E "^\[(INFO|ERROR|WARNING)\] Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$" \
        "$LOG_CONSUMER" | tail -1 | sed -e 's/^\[[A-Z]*\] /  /'
    if [ "$ESTADO_CONSUMER" -ne 0 ]; then
        echo "${ROJO}  Las pruebas del consumer fallaron:${FIN}"
        grep -E "^\[ERROR\]|<<< (FAILURE|ERROR)!" "$LOG_CONSUMER" | head -20 | sed 's|^|    |'
        echo "  Log completo: $LOG_CONSUMER"
    else
        DESPUES=$(cat "$PACTOS"/*.json 2>/dev/null | shasum | awk '{print $1}')
        if [ "$ANTES" != "$DESPUES" ]; then
            DIFIERE=1
            echo "${ROJO}  K1: el pacto en disco no era el que genera el consumer (editado a mano o sin${FIN}"
            echo "${ROJO}      regenerar tras cambiar la prueba). Ya quedo regenerado: revisar 'git diff $PACTOS/'${FIN}"
            echo "${ROJO}      y volver a correr el script.${FIN}"
        else
            echo "  K1: el pacto en disco es identico al que genera el consumer ${VERDE}OK${FIN}"
        fi
    fi
fi

# --- 4. Analisis del pacto ---------------------------------------------------------------------
echo
echo "${NEGRITA}== 4. El pacto: interacciones, estados y cobertura del cliente ==${FIN}"
FALTANTES=0
ARCHIVOS_PACTO=$(ls "$PACTOS"/*.json 2>/dev/null)
if [ -z "$ARCHIVOS_PACTO" ]; then
    echo "${ROJO}  No hay pactos en $PACTOS/: el consumer todavia no los genero.${FIN}"
    FALTANTES=1
fi
for pacto in $ARCHIVOS_PACTO; do
    consumidor=$(jq -r '.consumer.name' "$pacto"); proveedor=$(jq -r '.provider.name' "$pacto")
    n=$(jq '.interactions | length' "$pacto")
    echo "  $(basename "$pacto")  ($consumidor -> $proveedor)"
    if [ "$n" -lt 2 ]; then
        echo "${ROJO}    K8: $n interaccion(es); el contrato necesita al menos 2${FIN}"
        FALTANTES=$((FALTANTES + 1))
    fi
    while IFS=$'\t' read -r descripcion estado; do
        if [ -z "$estado" ]; then
            marca="${ROJO}sin estado${FIN}"; FALTANTES=$((FALTANTES + 1))
        elif printf '%s\n' "$PROVIDER" | xargs grep -qF "@State(\"$estado\")" 2>/dev/null; then
            marca="@State ${VERDE}SI${FIN}"
        else
            marca="@State ${ROJO}NO${FIN}"; FALTANTES=$((FALTANTES + 1))
        fi
        printf "    - %-52s %s\n" "$descripcion" "$marca"
        [ -n "$estado" ] && printf "      estado: %s\n" "$estado"
    done < <(jq -r '.interactions[] | [.description, (.providerStates[0].name // "")] | @tsv' "$pacto")
done

# Cada metodo publico del cliente tiene que ejercitarse en alguna prueba del consumer.
for cliente in $(find "$CONSUMIDOR/src/main/java" -name 'Cliente*.java'); do
    clase=$(basename "$cliente" .java)
    for metodo in $(grep -oE '^    public [A-Za-z<>.,? ]+ [a-z][A-Za-z0-9]* *\(' "$cliente" \
                    | sed -E 's/ *\($//; s/.* //'); do
        if printf '%s\n' "$CONSUMER" | xargs grep -qE "\.${metodo} *\(" 2>/dev/null; then
            printf "  %-52s %s\n" "$clase.$metodo()" "cubierto ${VERDE}SI${FIN}"
        else
            printf "  %-52s %s\n" "$clase.$metodo()" "cubierto ${ROJO}NO${FIN}"
            FALTANTES=$((FALTANTES + 1))
        fi
    done
done

# --- 5. Provider: verificacion del pacto -------------------------------------------------------
ESTADO_PROVIDER=0
if [ "$RAPIDO" = "0" ] && [ "$ESTADO_CONSUMER" -eq 0 ]; then
    echo
    echo "${NEGRITA}== 5. Provider: verificacion contra la aplicacion real (failsafe) ==${FIN}"
    LOG_PROVIDER=target/verificacion-contratos-provider.log
    mvn -pl bootstrap -am verify -Dtest=NingunaPruebaUnitaria -Dsurefire.failIfNoSpecifiedTests=false \
        -Dit.test='*PactIT' > "$LOG_PROVIDER" 2>&1
    ESTADO_PROVIDER=$?
    grep -E "^Verifying a pact|^  Given |^    returns a response|^      has |^      includes headers" \
        "$LOG_PROVIDER" | sed -e 's/^/  /' \
        -e "s/(OK)/(${VERDE}OK${FIN})/" -e "s/(FAILED)/(${ROJO}FAILED${FIN})/"
    awk '/failsafe:.*:integration-test/ { en_it = 1 }
         en_it && /^\[(INFO|ERROR)\] Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+$/ {
            gsub(/[^0-9,]/, ""); split($0, n, ",")
            corridas += n[1]; fallos += n[2]; errores += n[3]; omitidas += n[4]
         }
         END { printf "  Interacciones verificadas: %d  ·  fallos: %d  ·  errores: %d  ·  omitidas: %d\n",
                      corridas, fallos, errores, omitidas }' "$LOG_PROVIDER"
    if [ "$ESTADO_PROVIDER" -ne 0 ]; then
        echo "${ROJO}  La verificacion del provider fallo. Detalle:${FIN}"
        # Si Pact llego a comparar, su diagnostico (campo, esperado, recibido) es lo que sirve; si no
        # (no compila, no levanta el contexto), los errores de Maven.
        DESAJUSTES=$(grep -E "^    [0-9]+\.[0-9]+\) " "$LOG_PROVIDER" | sort -u)
        if [ -n "$DESAJUSTES" ]; then
            printf '%s\n' "$DESAJUSTES" | sed 's|^ *|    |'
        else
            grep -E "^\[ERROR\]|<<< (FAILURE|ERROR)!" "$LOG_PROVIDER" | head -20 | sed 's|^|    |'
        fi
        echo "  Log completo: $LOG_PROVIDER"
    fi
fi

echo
if [ "$N_VIOLACIONES" -gt 0 ] || [ "$FALTANTES" -gt 0 ] || [ "$DIFIERE" -ne 0 ] \
   || [ "$ESTADO_CONSUMER" -ne 0 ] || [ "$ESTADO_PROVIDER" -ne 0 ]; then
    echo "${ROJO}${NEGRITA}FAIL${FIN} — corregir antes de entregar (reglas: $REGLAS)."
    exit 1
fi
if [ "$RAPIDO" = "1" ]; then
    echo "${VERDE}${NEGRITA}PASS${FIN} — reglas y pacto en verde."
    echo "${AMARILLO}(--rapido: ni el consumer ni el provider se ejecutaron)${FIN}"
else
    echo "${VERDE}${NEGRITA}PASS${FIN} — reglas, pacto regenerado y verificacion del provider en verde."
fi
