#!/usr/bin/env bash
# Benchmark en frío en el teléfono conectado por adb (RF-05, CA-05).
# Cada corrida: force-stop de la app → BenchActivity (proceso nuevo) → motor + conexión + consulta → fin.
#
# Uso: ./bench.sh <motor> <número E.164 sin +> [corridas=20] [red=wifi]
#   ./bench.sh whatsmeow 51XXXXXXXXX 20 wifi
#   ./bench.sh whatsmeow 51XXXXXXXXX 20 datos
# Usar SOLO con el número secundario. Los resultados quedan en results/ (fuera de git) con el número enmascarado.
set -euo pipefail
export PYTHONIOENCODING=utf-8  # la consola de Windows usa cp1252 por defecto
cd "$(dirname "$0")"

ENGINE=${1:?motor (whatsmeow)}
PHONE=${2:?número E.164 sin +}
RUNS=${3:-20}
NETWORK=${4:-wifi}
ADB=${ADB:-"${LOCALAPPDATA:-$HOME}/Android/Sdk/platform-tools/adb"}
PKG=com.whatscheck.engines
REMOTE_DIR=files/results

(( RUNS >= 1 && RUNS <= 30 )) || { echo "corridas entre 1 y 30" >&2; exit 1; }
remote_cat() { "$ADB" exec-out run-as "$PKG" sh -c "cat $REMOTE_DIR/*.jsonl 2>/dev/null" | tr -d '\r'; }
lines() { remote_cat | grep -c . || true; }

START=$(date -u +%Y-%m-%dT%H:%M:%S)
echo "Benchmark $ENGINE · $RUNS corridas en frío · red: $NETWORK · pausa 10–15 s"
for i in $(seq 1 "$RUNS"); do
  before=$(lines)
  "$ADB" shell am force-stop "$PKG"
  "$ADB" shell am start -n "$PKG/.BenchActivity" --es engine "$ENGINE" --es phone "$PHONE" --es network "$NETWORK" >/dev/null
  for _ in $(seq 1 80); do sleep 0.5; (( $(lines) > before )) && break; done
  last=$(remote_cat | tail -1)
  python -c "import json,sys; r=json.loads(sys.argv[1]); print(f\"  Corrida $i/$RUNS: {r['status']}{' ('+str(r['reason'])+')' if r['reason'] else ''} · proceso {r['tProcess']} ms · motor {r['tEngine']} ms · sesión {r['tReady']} ms · consulta {r['tQuery']} ms · total {r['tTotal']} ms\")" "$last"
  (( i < RUNS )) && sleep $(( 10 + RANDOM % 6 ))
done

mkdir -p results
DEVICE=$("$ADB" shell getprop ro.product.model | tr -d '\r' | tr ' ' '_')
OUT="results/android-$(date +%F)-${DEVICE}.jsonl"  # un archivo por teléfono: antes se sobrescribían
remote_cat > "$OUT"
echo "Resultados del teléfono copiados a $OUT"

python - "$OUT" "$START" "$ENGINE" "$NETWORK" <<'EOF'
import json, math, sys
path, start, engine, network = sys.argv[1:5]
rows = [json.loads(l) for l in open(path, encoding="utf-8") if l.strip()]
rows = [r for r in rows if r["kind"] == "bench-android" and r["engine"] == engine and r["network"] == network and r["ts"] >= start]
ok = [r for r in rows if r["status"] != "UNKNOWN"]

def pct(v, p):  # rango más cercano, igual que baileys-cli/src/stats.ts
    v = sorted(v); return v[min(max(math.ceil(p / 100 * len(v)), 1), len(v)) - 1]

print(f"\n{'Fase':<28}{'n':>4}{'mín':>9}{'p50':>9}{'p95':>9}{'máx':>9}")
fases = [("Arranque del proceso", "tProcess"), ("Motor (runtime + store)", "tEngine"),
         ("Sesión lista (incluye motor)", "tReady"), ("Consulta", "tQuery"), ("Total en frío", "tTotal")]
for name, key in fases:
    v = [r[key] for r in ok if r[key] is not None]
    if v:
        print(f"{name:<28}{len(v):>4}" + "".join(f"{x/1000:>8.2f}s" for x in (min(v), pct(v, 50), pct(v, 95), max(v))))
fails = len(rows) - len(ok)
pss = [r["pssKb"] for r in ok]
print(f"\nSin verificar (❔): {fails} de {len(rows)} · PSS p50: {pct(pss, 50)/1024:.0f} MB" if pss else f"\nSin verificar: {fails} de {len(rows)}")
if ok:
    p95 = pct([r["tTotal"] for r in ok], 95)
    lat = "GO" if p95 <= 3000 else "GO_CON_RESERVAS" if p95 <= 8000 else "NO_GO"
    fal = "GO" if fails == 0 else "GO_CON_RESERVAS" if fails <= 2 else "NO_GO"
    order = ["GO", "GO_CON_RESERVAS", "NO_GO"]
    print(f"Veredicto (latencia + fallos, validation.md): {order[max(order.index(lat), order.index(fal))]} · p95 total {p95/1000:.2f} s")
EOF
