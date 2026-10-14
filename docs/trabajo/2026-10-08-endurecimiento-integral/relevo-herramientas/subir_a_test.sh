#!/usr/bin/env bash
# Sube a origin/test (autodeploy en Coolify) el arbol EXACTO de la rama de cierre,
# como un commit nuevo encima de origin/test. No reescribe historia, no usa --force.
# Uso:  bash subir_a_test.sh        (desde cualquier carpeta)
set -euo pipefail
REPO="/c/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuBackend-cierre"
RAMA="justin/feature/cierre-endurecimiento-2026-10-08"
cd "$REPO"

echo "== Subida a TEST: claves cargadas y respaldo hechos por el usuario (pre-subida-pasanaku.dump, 2.6M)"
git fetch origin
ORIGEN="origin/$RAMA"   # se sube lo YA empujado a GitHub, no el arbol de trabajo
TREE=$(git rev-parse "$ORIGEN^{tree}")
PADRE=$(git rev-parse origin/test)
NUEVO=$(git commit-tree "$TREE" -p "$PADRE" -m "Subida a TEST desde $RAMA ($(git rev-parse --short "$ORIGEN")): endurecimiento integral

Esquema expandido de forma aditiva por despliegue/aplicar-esquema.sh.
Requiere QR_INTERNO_CLAVE e INVITACIONES_CLAVE en Coolify.
Marcha atras: git revert de este commit.

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>")
echo "Commit de subida: $NUEVO (padre: $PADRE)"
git push origin "$NUEVO:refs/heads/test"
echo "Listo. Coolify desplegara solo. Vigila con:  curl -s -o /dev/null -w '%{http_code}\n' https://api.aportaya.161.97.85.216.sslip.io/actuator/health"
