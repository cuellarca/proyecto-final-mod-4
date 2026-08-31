#!/usr/bin/env bash
# Demo end-to-end de ms-logistic-delivery vía REST (curl). Requiere la app en http://localhost:8080.
# Uso: ./demo.sh
set -euo pipefail

HOST="${HOST:-http://localhost:8080}"
REPARTIDOR="rep-demo-$$"
FECHA="2026-07-10"

jqget() { python3 -c "import sys,json;print(json.load(sys.stdin)$1)"; }

echo "== HU-2: geocoding (OHS) =="
curl -s -X POST "$HOST/api/v1/geocoding" -H 'Content-Type: application/json' \
  -d '{"direccion":"Av. Banzer 3er anillo, Santa Cruz"}'; echo

echo "== HU-1/2/3: planificar ruta =="
PLAN=$(curl -s -X POST "$HOST/api/v1/rutas" -H 'Content-Type: application/json' -d "{
  \"repartidorId\":\"$REPARTIDOR\",\"fecha\":\"$FECHA\",\"origenLat\":-17.78,\"origenLon\":-63.18,
  \"paquetes\":[
    {\"paqueteId\":\"pkg-1\",\"pacienteId\":\"pac-1\",\"lat\":-17.7833,\"lon\":-63.1821},
    {\"paqueteId\":\"pkg-2\",\"pacienteId\":\"pac-2\",\"lat\":-17.7900,\"lon\":-63.1900}
  ]}")
echo "$PLAN"
RUTA_ID=$(echo "$PLAN" | jqget "['rutaId']")
ENTREGA1=$(echo "$PLAN" | jqget "['entregas'][0]['entregaId']")
ENTREGA2=$(echo "$PLAN" | jqget "['entregas'][1]['entregaId']")

echo "== HU-1: rutas del repartidor =="
RUTAS=$(curl -s "$HOST/api/v1/repartidores/$REPARTIDOR/rutas?fecha=$FECHA")
echo "$RUTAS"
PARADA1=$(echo "$RUTAS" | jqget "[0]['paradas'][0]['paradaId']")

echo "== HU-3: ejecutar ruta (inicio -> avance -> completar) =="
curl -s -o /dev/null -w "inicio: %{http_code}\n"     -X POST "$HOST/api/v1/rutas/$RUTA_ID/inicio"
curl -s -o /dev/null -w "avance: %{http_code}\n"     -X POST "$HOST/api/v1/rutas/$RUTA_ID/paradas/$PARADA1/avance"
curl -s -o /dev/null -w "completar: %{http_code}\n"  -X POST "$HOST/api/v1/rutas/$RUTA_ID/paradas/$PARADA1/completar"

echo "== HU-4: confirmar entrega 1 con constancia =="
curl -s -o /dev/null -w "confirmacion: %{http_code}\n" -X POST "$HOST/api/v1/entregas/$ENTREGA1/confirmacion" \
  -H 'Content-Type: application/json' \
  -d '{"lat":-17.7833,"lon":-63.1821,"urlEvidencia":"https://storage.example.com/evidencia/pkg-1.jpg","nombreReceptor":"Maria Perez"}'

echo "== HU-5: fallar entrega 2 y reintentar =="
curl -s -o /dev/null -w "fallo: %{http_code}\n"     -X POST "$HOST/api/v1/entregas/$ENTREGA2/fallo" \
  -H 'Content-Type: application/json' -d '{"motivo":"AUSENTE"}'
curl -s -o /dev/null -w "reintento: %{http_code}\n" -X POST "$HOST/api/v1/entregas/$ENTREGA2/reintento"

echo "== HU-6: consultar el respaldo (esperando la proyección) =="
sleep 3
echo "-- historial pac-1:"; curl -s "$HOST/api/v1/pacientes/pac-1/entregas"; echo
echo "-- constancia entrega 1:"; curl -s "$HOST/api/v1/entregas/$ENTREGA1/constancia"; echo

echo "== Errores (problem+json) =="
echo "-- confirmar inexistente (404):"
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$HOST/api/v1/entregas/00000000-0000-0000-0000-000000000000/confirmacion" \
  -H 'Content-Type: application/json' -d '{"lat":-17.78,"lon":-63.18,"urlEvidencia":"https://s/x.jpg","nombreReceptor":"X"}'
echo "-- cuerpo invalido (400):"
curl -s -o /dev/null -w "%{http_code}\n" -X POST "$HOST/api/v1/rutas" -H 'Content-Type: application/json' -d '{}'

echo "== Demo completada =="
