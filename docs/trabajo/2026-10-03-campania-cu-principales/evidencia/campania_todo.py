"""Ejecutor único de la campaña: corre, en orden, todos los pasos de API sobre una base recién sembrada.

Uso (base recién reseteada y claves puestas con scripts/clave_dev.py --huella):
    CLAVE_DEV='...' python campania_todo.py | tee h3-campania-final.txt

Cada script de paso es independiente e imprime estado HTTP y código de error del contrato; ninguno se detiene
ante un fallo: la campaña clasifica cada resultado, no lo oculta. No usa newman (no está instalado y no se
suma una dependencia): es HTTP con la librería estándar.
"""
import subprocess
import sys

PASOS = [
    ("H2.S1.M6 · sondas de autorización", "h2_s1_m6_sondas.py"),
    ("H3.S2 · CU-20 crear grupo", "h3_s2_cu20.py"),
    ("H3.S5 · CU-10 recargar y CU-12 transferir (usuario sin grupo)", "h3_dinero.py"),
    ("H3.S6 · CU-12 transferir por alias (usuario con grupo)", "h3_cu12.py"),
    ("H3 · CU-90, CU-69, CU-68, CU-60, CU-21, CU-64, CU-74", "h3_grupos.py"),
    ("H3 · CU-90 (cuerpo corregido), CU-61, CU-62, CU-22, CU-11", "h3_resto.py"),
    ("H3.S10 · CU-62 permuta entre turnos futuros", "h3_permuta.py"),
    ("H3.S9 · CU-64 traspaso de cupo en GRP-DEMO-02", "h3_traspaso_g2.py"),
]

for titulo, script in PASOS:
    print(f"\n{'#' * 78}\n# {titulo}  ({script})\n{'#' * 78}", flush=True)
    r = subprocess.run([sys.executable, script], capture_output=True, text=True, encoding="utf-8")
    print(r.stdout, end="")
    if r.returncode != 0:
        print(f"[el script terminó con código {r.returncode}]\n{r.stderr[-600:]}")
