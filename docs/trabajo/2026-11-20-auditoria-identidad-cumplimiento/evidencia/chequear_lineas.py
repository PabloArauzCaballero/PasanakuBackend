"""Chequea que cada referencia `archivo:línea(s)` del informe exista y no supere el largo del archivo.

Solo lee. Uso: python3 chequear_lineas.py <informe.md>
"""
import os
import re
import subprocess
import sys

RAIZ = '/home/user/PasanakuBackend'
PREFIJOS = [
    '',
    'apps/movil/lib/pantallas/identidad/',
    'apps/movil/',
    'apps/backoffice/src/app/rutas/',
    'servicios/identidad/src/main/java/bo/aportaya/identidad/',
    'servicios/identidad/',
    'servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/',
    'servicios/cumplimiento/',
    'servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/',
]
archivos = subprocess.run(['git', '-C', RAIZ, 'ls-files'], capture_output=True, text=True).stdout.split('\n')
por_nombre = {}
for f in archivos:
    por_nombre.setdefault(os.path.basename(f), []).append(f)

largo_cache = {}


def largo(ruta):
    if ruta not in largo_cache:
        with open(os.path.join(RAIZ, ruta), 'rb') as fh:
            largo_cache[ruta] = fh.read().count(b'\n') + 1
    return largo_cache[ruta]


def resolver(ref):
    ref = ref.lstrip('/')
    plano = ref.replace('.../', '')
    for corto, largo_ in (('identidad/', 'sql/10_tablas/01_identidad_usuarios/'), ('cumplimiento/', 'sql/10_tablas/12_cumplimiento_asfi/')):
        if plano.startswith(corto) and plano.endswith('.sql'):
            plano = largo_ + plano[len(corto):]
    for p in PREFIJOS:
        if os.path.isfile(os.path.join(RAIZ, p + plano)):
            return p + plano
    base = os.path.basename(plano)
    candidatos = [c for c in por_nombre.get(base, []) if c.endswith(plano)]
    if len(candidatos) == 1:
        return candidatos[0]
    trozos = [t for t in re.split(r'/?\.\.\./?', ref) if t]
    candidatos = [c for c in por_nombre.get(base, []) if all(t.strip('/') in c for t in trozos)]
    candidatos = [c for c in candidatos if '/test/' not in c or 'test' in ref] or candidatos
    if len(candidatos) == 1:
        return candidatos[0]
    sql = [c for c in candidatos if c.startswith('sql/10_tablas/')]
    if len(sql) == 1:
        return sql[0]
    return None


texto = open(sys.argv[1]).read()
seccion = texto[texto.index('## 5. Hallazgos'):texto.index('## 6. Matriz de contratos')]
actual = None
malos = []
total = 0
patron = re.compile(r'`([A-Za-z0-9_.\-/]+\.(?:java|dart|ts|sql|yaml|yml|md|kts|xml|json)):([0-9][0-9,\- ]*)')
for linea in seccion.split('\n'):
    m = re.match(r'### (IDCU-[A-Z0-9-]+-\d{3})', linea)
    if m:
        actual = m.group(1)
    for ref, nums in patron.findall(linea):
        numeros = [int(n) for n in re.findall(r'\d+', nums)]
        if not numeros:
            continue
        total += 1
        ruta = resolver(ref)
        if ruta is None:
            malos.append((actual, ref, nums.strip(), 'archivo no resuelto'))
            continue
        tope = largo(ruta)
        if max(numeros) > tope:
            malos.append((actual, ruta, nums.strip(), f'el archivo tiene {tope} líneas'))
print(f'referencias revisadas: {total} · fuera de rango o sin resolver: {len(malos)}')
for m in malos:
    print(' | '.join(str(x) for x in m))
