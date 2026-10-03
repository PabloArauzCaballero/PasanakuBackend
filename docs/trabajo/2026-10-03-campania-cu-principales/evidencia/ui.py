"""Asistente minimo para manejar la app en el emulador por adb (sin dependencias).

    python ui.py ver                      -> lista los textos visibles con su centro
    python ui.py tocar "Texto"            -> toca el primer elemento cuyo texto o descripcion contiene «Texto»
    python ui.py escribir "texto"         -> escribe en el campo con foco
    python ui.py foto nombre.png          -> captura de pantalla
    python ui.py volver                   -> tecla atras
Los datos que se escriben son sinteticos de la campania; nunca se captura ni se imprime una clave.
"""
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

ADB = "C:/Users/DELL/tools/android-sdk/platform-tools/adb.exe"


def adb(*a, binario=False):
    r = subprocess.run([ADB, *a], capture_output=True)
    return r.stdout if binario else r.stdout.decode("utf-8", "replace")


def nodos():
    adb("shell", "rm", "-f", "/sdcard/ui.xml")
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("exec-out", "cat", "/sdcard/ui.xml")
    out = []
    for n in ET.fromstring(xml).iter("node"):
        t = (n.get("text") or n.get("content-desc") or "").strip()
        m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds") or "")
        if t and m:
            x1, y1, x2, y2 = map(int, m.groups())
            out.append((t, (x1 + x2) // 2, (y1 + y2) // 2, n.get("clickable") == "true"))
    return out


def main():
    cmd, *args = sys.argv[1:]
    if cmd == "ver":
        for t, x, y, c in nodos():
            print(f"{'*' if c else ' '} ({x:4},{y:4}) {t[:90]}")
    elif cmd == "tocar":
        buscado = args[0].lower()
        for t, x, y, c in nodos():
            if buscado in t.lower():
                adb("shell", "input", "tap", str(x), str(y))
                print(f"toque en «{t[:60]}» ({x},{y})")
                return
        print(f"NO ENCONTRADO: {args[0]}")
        sys.exit(1)
    elif cmd == "escribir":
        adb("shell", "input", "text", args[0].replace(" ", "%s"))
    elif cmd == "escribir_env":
        import os
        adb("shell", "input", "text", os.environ[args[0]].replace(" ", "%s"))
        print("escrito desde la variable", args[0], "(sin imprimir)")
    elif cmd == "pos":
        adb("shell", "input", "tap", args[0], args[1])
    elif cmd == "foto":
        open(args[0], "wb").write(adb("exec-out", "screencap", "-p", binario=True))
        print("captura:", args[0])
    elif cmd == "volver":
        adb("shell", "input", "keyevent", "4")
    elif cmd == "tecla":
        adb("shell", "input", "keyevent", args[0])


main()
