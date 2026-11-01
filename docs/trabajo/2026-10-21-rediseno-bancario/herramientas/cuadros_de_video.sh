#!/usr/bin/env bash
# Tres cuadros clave (inicio, medio, fin) de un video de movimiento, para la evidencia
# de H5: evidencia/movimiento/<pieza>__t0|t50|t100.png.
# Uso: bash cuadros_de_video.sh <video> <prefijo_de_salida> [desde_s] [hasta_s]
set -euo pipefail
video="$1"; salida="$2"
duracion=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$video")
desde="${3:-0}"; hasta="${4:-$duracion}"
for pct in 0 50 100; do
  t=$(python3 -c "d=$hasta-$desde; print(max(0, min($hasta - 0.05, $desde + d * $pct / 100)))")
  ffmpeg -v error -y -ss "$t" -i "$video" -frames:v 1 "${salida}__t${pct}.png"
done
echo "3 cuadros de $video ($desde–$hasta s) → ${salida}__t{0,50,100}.png"
