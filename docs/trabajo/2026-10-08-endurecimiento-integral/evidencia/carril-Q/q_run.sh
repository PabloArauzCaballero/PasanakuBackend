#!/usr/bin/env bash
# q_run.sh <etiqueta> <args gradle...>: bajo el guardian, log en q/<etiqueta>.log
SP="C:/Users/Usuario/AppData/Local/Temp/claude/C--Users-Usuario-Downloads-Entrypoint-GitHUb-Pasanaku/c468f89e-ca7f-4172-b7d5-74080b60e5a8/scratchpad/q"
et="$1"; shift
MAX_WAIT_S=3000 bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash "$SP/q_jj.sh" "$@" > "$SP/$et.log" 2>&1
echo "FIN RC=$?" >> "$SP/$et.log"
