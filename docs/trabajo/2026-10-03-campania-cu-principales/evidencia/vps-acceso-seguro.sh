#!/usr/bin/env bash
# Deja el acceso al VPS de Contabo seguro y listo para traer variables de Atlas SIN pasar contraseñas por el chat.
#
# LO CORRES TU, en tu propia terminal (Git Bash o WSL), porque `ssh-copy-id` te pide la contraseña de root
# UNA vez de forma interactiva. Esa contraseña no se guarda ni se imprime en ningun lado.
#
#   export VPS_HOST=161.97.85.216
#   export RUTA_ENV=/ruta/real/del/.env        # donde esta el .env de Atlas en el VPS
#   bash vps-acceso-seguro.sh paso1            # llave + usuario de solo lectura + nombres de variables
#   bash vps-acceso-seguro.sh nombres          # solo lista los NOMBRES de las variables (nunca valores)
#   bash vps-acceso-seguro.sh traer GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET ...   # trae SOLO esas variables
#   bash vps-acceso-seguro.sh endurecer        # imprime los pasos manuales para cerrar root por contraseña
#
# Que hace y que NO hace:
#  - NO cambia sshd ni desactiva el login por contraseña (eso lo haces tu al final, a mano, con la llave ya probada).
#  - El usuario `lectorenv` solo puede LEER el .env (setfacl); no tiene sudo ni escritura.
#  - `traer` escribe en .env.atlas.local (en la raiz del repo, ignorado por git) y NO imprime los valores.
set -euo pipefail

HOST="${VPS_HOST:?define VPS_HOST (p.ej. 161.97.85.216)}"
ADMIN="${VPS_ADMIN:-root}"
LECTOR="${VPS_LECTOR:-lectorenv}"
KEY="${VPS_KEY:-$HOME/.ssh/id_ed25519_contabo}"
SSH_OPTS=(-i "$KEY" -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new)

necesita_ruta() { : "${RUTA_ENV:?define RUTA_ENV con la ruta del .env en el VPS}"; }

paso1() {
  necesita_ruta
  if [ ! -f "$KEY" ]; then
    echo "1) Creando una llave ed25519 nueva en $KEY"
    ssh-keygen -t ed25519 -f "$KEY" -C "pasanaku-lectura-$(date +%F)" -N ""
  else
    echo "1) Ya existe la llave $KEY (se reutiliza)"
  fi

  echo "2) Instalando la llave en $ADMIN@$HOST (te pide la contraseña de root UNA vez; no se guarda)"
  ssh-copy-id -i "$KEY.pub" "$ADMIN@$HOST"

  echo "3) Comprobando que se entra por LLAVE (sin contraseña)"
  ssh "${SSH_OPTS[@]}" -o PasswordAuthentication=no "$ADMIN@$HOST" 'echo "entrada por llave: OK"'

  echo "4) Creando el usuario de solo lectura '$LECTOR' y dandole lectura SOLO sobre el .env"
  ssh "${SSH_OPTS[@]}" "$ADMIN@$HOST" "RUTA_ENV='$RUTA_ENV' LECTOR='$LECTOR' bash -s" <<'REMOTO'
set -e
id "$LECTOR" >/dev/null 2>&1 || useradd -m -s /bin/bash "$LECTOR"
install -d -m 700 -o "$LECTOR" -g "$LECTOR" "/home/$LECTOR/.ssh"
cp /root/.ssh/authorized_keys "/home/$LECTOR/.ssh/authorized_keys"
chown "$LECTOR:$LECTOR" "/home/$LECTOR/.ssh/authorized_keys"
chmod 600 "/home/$LECTOR/.ssh/authorized_keys"
command -v setfacl >/dev/null 2>&1 || { apt-get update -qq && apt-get install -y -qq acl; }
test -f "$RUTA_ENV" || { echo "NO EXISTE $RUTA_ENV en el VPS"; exit 1; }
setfacl -m "u:$LECTOR:r" "$RUTA_ENV"
# necesita poder atravesar los directorios padre
d="$(dirname "$RUTA_ENV")"; while [ "$d" != "/" ]; do setfacl -m "u:$LECTOR:x" "$d" 2>/dev/null || true; d="$(dirname "$d")"; done
echo "usuario $LECTOR listo: lee $RUTA_ENV y nada mas"
REMOTO

  echo "5) Nombres de las variables (SIN valores):"
  nombres
  echo
  echo "Siguiente: elige los nombres que necesita el Gmail auth y corre:  bash $0 traer NOMBRE1 NOMBRE2 ..."
  echo "Despues, cuando todo funcione, corre:  bash $0 endurecer"
}

nombres() {
  necesita_ruta
  ssh "${SSH_OPTS[@]}" "$LECTOR@$HOST" "grep -oE '^[A-Za-z_][A-Za-z0-9_]*=' '$RUTA_ENV' | sed 's/=\$//' | sort -u"
}

traer() {
  necesita_ruta
  [ "$#" -ge 1 ] || { echo "indica al menos un NOMBRE de variable"; exit 1; }
  RAIZ="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
  SALIDA="$RAIZ/.env.atlas.local"
  # Asegura que git lo ignora ANTES de escribir ningun valor.
  grep -qxF ".env.atlas.local" "$RAIZ/.gitignore" 2>/dev/null || echo ".env.atlas.local" >> "$RAIZ/.gitignore"
  : > "$SALIDA"; chmod 600 "$SALIDA"
  for nombre in "$@"; do
    [[ "$nombre" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || { echo "nombre invalido: $nombre"; exit 1; }
    ssh "${SSH_OPTS[@]}" "$LECTOR@$HOST" "grep -E '^$nombre=' '$RUTA_ENV' | head -1" >> "$SALIDA" \
      || echo "no se encontro $nombre"
  done
  echo "Escritas $(wc -l < "$SALIDA") variable(s) en $SALIDA (ignorado por git, permisos 600). Valores NO impresos."
}

endurecer() {
  cat <<'TEXTO'
Cuando la entrada por llave y el usuario de lectura funcionen, en el VPS (como root) y EN OTRA SESION ABIERTA:
  1. nano /etc/ssh/sshd_config.d/99-endurecer.conf
       PermitRootLogin prohibit-password
       PasswordAuthentication no
  2. sshd -t                    # valida la configuracion; si da error, NO sigas
  3. systemctl reload ssh       # (o sshd)
  4. SIN cerrar la sesion actual, abre otra terminal y comprueba:  ssh -i ~/.ssh/id_ed25519_contabo root@HOST 'echo ok'
  5. Cambia la contraseña de root (passwd) aunque ya no se use: la actual quedo escrita en un chat.
Si algo falla, la sesion que dejaste abierta te permite revertir el archivo del paso 1.
TEXTO
}

case "${1:-}" in
  paso1) paso1 ;;
  nombres) nombres ;;
  traer) shift; traer "$@" ;;
  endurecer) endurecer ;;
  *) sed -n '2,18p' "$0"; exit 1 ;;
esac
