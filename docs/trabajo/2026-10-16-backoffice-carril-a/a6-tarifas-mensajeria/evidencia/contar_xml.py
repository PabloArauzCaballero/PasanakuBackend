"""Cuenta tests/failures/errors/skipped por task desde los XML de JUnit de un servicio.

Uso: python3 contar_xml.py <ruta servicios/<s>/build/test-results>
"""
import pathlib
import sys
import xml.etree.ElementTree as E

raiz = pathlib.Path(sys.argv[1])
for task in sorted(p for p in raiz.iterdir() if p.is_dir()):
    t = f = e = s = 0
    for x in task.glob("*.xml"):
        r = E.parse(x).getroot()
        t += int(r.get("tests"))
        f += int(r.get("failures"))
        e += int(r.get("errors"))
        s += int(r.get("skipped"))
    print(f"{task.name}: tests={t} failures={f} errors={e} skipped={s}")
