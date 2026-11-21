import csv, sys, collections
sys.path.insert(0, '.')
from propuestas import P, E, DEF
filas = list(csv.DictReader(open('inventario-textos.tsv', encoding='utf-8'), delimiter='\t'))
orden = ['landing','web','faq','jsonld','tour','plantilla-sms','plantilla-push','plantilla-correo','plantilla-otro','app-texto','error-movil','error-web','error-api','error-backoffice','backoffice']
filas.sort(key=lambda f: (orden.index(f['superficie']) if f['superficie'] in orden else 99, f['id']))
def esc(s): return s.replace('|', '\\|').replace('\n', ' ').strip()
out, tipo, defaults = [], collections.Counter(), []
for f in filas:
    i = f['id']
    if i in P: prop, t = P[i], 'REESCRIBIR'
    elif i in E: prop, t = E[i], 'ELIMINAR'
    else:
        prop, t = DEF[f['categoria']], 'ELIMINAR'
        if f['categoria'] in ('REVISION_HUMANA','RESPALDO_EMPRESA','OTRO') or (f['categoria']=='WHATSAPP' and f['superficie']!='plantilla-otro'):
            defaults.append(i)
    if prop.startswith(('Sin cambio',)): t = 'SIN CAMBIO'
    elif prop.startswith(('Quitar','Reemplazar')): t = 'AJUSTE PARCIAL'
    elif prop.startswith('Eliminar') and i in P: t = 'ELIMINAR'
    tipo[t] += 1
    out.append(f"| {i} | {f['superficie']} | `{f['ruta:linea']}` | {esc(f['texto_actual'])} | **{t}** — {esc(prop)} | {f['categoria']} |")
open('tabla.md','w').write('\n'.join(out)+'\n')
print('filas', len(filas), dict(tipo)); print('defaults sensibles:', defaults)
print('ids en P/E que no existen:', [k for k in list(P)+list(E) if k not in {f['id'] for f in filas}])
