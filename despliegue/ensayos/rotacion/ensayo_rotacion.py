#!/usr/bin/env python3
"""Ensayo de ROTACION DE SECRETOS (H13.S1.M3). Local, sin red, sin ningun secreto real.

    python despliegue/ensayos/rotacion/ensayo_rotacion.py [--salida registro.json]

Ensaya el procedimiento de docs/operacion/rotacion-de-secretos-ensayada.md con material
criptografico SINTETICO generado al vuelo (`secrets`, `cryptography`): no toca los servicios ni
sus variables. Lo que demuestra es la mecanica de cada clase de secreto, para saber ANTES del
incidente cuanto cuesta rotarlo y que se rompe si se rota mal:

  1. INVENTARIO   cada secreto que el despliegue (scripts/generar_compose.py, DE_ENTORNO) inyecta
                  tiene una fila en el documento de rotacion. Un secreto sin procedimiento no se
                  puede rotar a las tres de la manana.
  2. HIGIENE      ninguno de los valores de desarrollo aparece en el compose DESPLEGADO: rotar es
                  cambiar una variable de entorno, no editar el repositorio.
  3. JWT RS256    (a) con una sola clave, rotar invalida TODA sesion viva: se mide cuantas;
                  (b) con ventana de solapamiento (JWKS con dos claves) no se invalida ninguna, y
                  recien al retirar la vieja deja de valer lo firmado con ella.
  4. HMAC         (INVITACIONES_CLAVE, WEBHOOK_SECRETO, CLAVES DE FIRMA) verificacion con dos claves
                  durante la ventana y firma solo con la nueva.
  5. PIMIENTA     el hash con pimienta no se puede "re-firmar": se verifica con las dos y se
                  re-hashea con la nueva al primer uso (migracion perezosa); se cuenta cuantos
                  faltan antes de poder retirar la vieja.

Sale con 0 si todas las comprobaciones se cumplen. Cada comprobacion que dice «DEFECTO ENCONTRADO»
es un hallazgo del repositorio, no un fallo del ensayo: se informa y se cuenta aparte.
"""
import argparse
import base64
import hashlib
import hmac
import json
import pathlib
import re
import secrets
import sys
import time

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

RAIZ = pathlib.Path(__file__).resolve().parent.parent.parent.parent
COMPOSE = RAIZ / "scripts" / "generar_compose.py"
DOC = RAIZ / "docs" / "operacion" / "rotacion-de-secretos-ensayada.md"
DESPLEGADO = RAIZ / "docker-compose.coolify.yml"
ES_SECRETO = re.compile(r"(CLAVE|SECRET|PIMIENTA|TOKEN|API_KEY|FIRMA|PASSWORD|CREDENCIAL)", re.I)

resultados = []
defectos = []


def comprobar(nombre, ok, detalle=""):
    resultados.append({"comprobacion": nombre, "ok": bool(ok), "detalle": detalle})
    print(f"  {'OK    ' if ok else 'FALLA '}· {nombre}" + (f" — {detalle}" if detalle else ""))


def defecto(texto):
    defectos.append(texto)
    print(f"  DEFECTO ENCONTRADO · {texto}")


def b64(datos):
    return base64.urlsafe_b64encode(datos).rstrip(b"=").decode()


def desb64(texto):
    return base64.urlsafe_b64decode(texto + "=" * (-len(texto) % 4))


# ------------------------------------------------------------------ 1 y 2 · inventario e higiene
def inventario():
    print("== 1 · INVENTARIO de secretos del despliegue contra el documento de rotacion")
    fuente = COMPOSE.read_text(encoding="utf-8")
    # COMUNES_DESPLEGADO (BD_CLAVE) + DE_ENTORNO: todo lo que el compose desplegado deja al almacen de secretos.
    desde = fuente.index("COMUNES_DESPLEGADO = {")
    bloque = fuente[desde:]
    entorno = bloque.index("DE_ENTORNO = {")
    bloque = bloque[: bloque.index("\n}")] + bloque[entorno:][: bloque[entorno:].index("\n}")]
    inyectados = sorted({m for m in re.findall(r"\$\{([A-Z0-9_]+)\}", bloque) if ES_SECRETO.search(m)})
    comprobar("el despliegue inyecta secretos que inventariar", len(inyectados) >= 8, f"{len(inyectados)} variables")
    comprobar("existe el documento de rotacion", DOC.is_file(), str(DOC.relative_to(RAIZ)))
    texto = DOC.read_text(encoding="utf-8") if DOC.is_file() else ""
    sin_fila = [v for v in inyectados if f"`{v}`" not in texto]
    comprobar("todo secreto inyectado tiene fila en el inventario del documento", not sin_fila,
              "sin fila: " + ", ".join(sin_fila) if sin_fila else f"{len(inyectados)} con fila")

    print("== 2 · HIGIENE: los valores de desarrollo no estan en el compose desplegado")
    valores_dev = re.findall(r'"(SEGURIDAD_PIMIENTA|WEBHOOK_SECRETO|CERTIFICADOS_CLAVE_FIRMA|INVITACIONES_CLAVE|QR_INTERNO_CLAVE)":\s*"([^"]+)"',
                             fuente[: fuente.index("DE_ENTORNO = {")])
    if DESPLEGADO.is_file():
        despleg = DESPLEGADO.read_text(encoding="utf-8")
        fugas = [n for n, v in valores_dev if v and v in despleg]
        comprobar("ningun valor de desarrollo aparece en docker-compose.coolify.yml", not fugas,
                  "aparece: " + ", ".join(fugas) if fugas else f"{len(valores_dev)} valores revisados")
    else:
        comprobar("existe docker-compose.coolify.yml para revisar", False, "no existe")


# ------------------------------------------------------------------ 3 · JWT RS256 con JWKS
def firmar_jwt(clave_privada, kid, sub, vence_en):
    cabecera = b64(json.dumps({"alg": "RS256", "typ": "JWT", "kid": kid}).encode())
    carga = b64(json.dumps({"sub": sub, "exp": vence_en}).encode())
    firma = clave_privada.sign(f"{cabecera}.{carga}".encode(), padding.PKCS1v15(), hashes.SHA256())
    return f"{cabecera}.{carga}.{b64(firma)}"


def verificar_jwt(token, jwks, ahora):
    """Como un servidor de recurso: elige la clave por kid dentro del JWKS publicado."""
    cabecera, carga, firma = token.split(".")
    kid = json.loads(desb64(cabecera))["kid"]
    publica = jwks.get(kid)
    if publica is None:
        return False
    try:
        publica.verify(desb64(firma), f"{cabecera}.{carga}".encode(), padding.PKCS1v15(), hashes.SHA256())
    except Exception:
        return False
    return json.loads(desb64(carga))["exp"] > ahora


def jwt():
    print("== 3 · JWT (JWT_CLAVE_FIRMA / JWKS de identidad): rotar sin y con ventana de solapamiento")
    nueva = lambda: rsa.generate_private_key(public_exponent=65537, key_size=2048)  # noqa: E731
    ahora, hora = int(time.time()), 3600
    sesiones = 200

    # (a) diseno de hoy: UNA clave. Rotar = cambiar la unica clave publicada.
    a = nueva()
    vivas = [firmar_jwt(a, "k-a", f"s2-usuario-{i}", ahora + hora) for i in range(sesiones)]
    jwks = {"k-a": a.public_key()}
    previas = sum(verificar_jwt(t, jwks, ahora) for t in vivas)
    b = nueva()
    jwks = {"k-b": b.public_key()}
    invalidadas = sum(not verificar_jwt(t, jwks, ahora) for t in vivas)
    comprobar("antes de rotar las sesiones vivas validan", previas == sesiones, f"{previas}/{sesiones}")
    comprobar("SIN la ventana (una sola clave publicada): rotar invalidaria TODA sesion viva (logout global)",
              invalidadas == sesiones, f"{invalidadas}/{sesiones} sesiones invalidadas")
    # El codigo ya soporta la ventana de dos claves (H13.S1.M2 hallazgo 2): el ensayo comprueba que el
    # emisor la publica y que la configuracion la acepta; la mecanica real la ejerce EmisorDeAcceso con
    # `RotacionDeClavesDeFirmaTest`.
    emisor = (RAIZ / "servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/EmisorDeAcceso.java")
    config = (RAIZ / "servicios/identidad/src/main/resources/application.yml")
    comprobar("identidad publica en su JWKS la clave vigente Y la anterior (ventana de rotacion)",
              emisor.is_file() and "clave-firma-anterior" in emisor.read_text(encoding="utf-8")
              and "JWT_CLAVE_FIRMA_ANTERIOR" in config.read_text(encoding="utf-8"),
              "aportaya.jwt.clave-firma-anterior / JWT_CLAVE_FIRMA_ANTERIOR")

    # (b) con solapamiento: JWKS = {vieja, nueva}; se firma con la nueva; se retira la vieja al vencer la ultima sesion.
    a = nueva()
    vivas_a = [firmar_jwt(a, "k-a", f"s2-usuario-{i}", ahora + hora) for i in range(sesiones)]
    b = nueva()
    jwks = {"k-a": a.public_key(), "k-b": b.public_key()}
    nuevas_b = [firmar_jwt(b, "k-b", f"s2-usuario-{i}", ahora + hora) for i in range(sesiones)]
    comprobar("CON SOLAPAMIENTO: las sesiones firmadas con la clave vieja siguen validando",
              all(verificar_jwt(t, jwks, ahora) for t in vivas_a), f"{sesiones}/{sesiones}")
    comprobar("CON SOLAPAMIENTO: lo firmado con la clave nueva valida desde el primer segundo",
              all(verificar_jwt(t, jwks, ahora) for t in nuevas_b), f"{sesiones}/{sesiones}")
    jwks_sin_vieja = {"k-b": b.public_key()}
    comprobar("al retirar la clave vieja, lo firmado con ella deja de validar (la clave filtrada no sirve)",
              not any(verificar_jwt(t, jwks_sin_vieja, ahora) for t in vivas_a), "0 aceptados")
    comprobar("y lo firmado con la nueva sigue valido",
              all(verificar_jwt(t, jwks_sin_vieja, ahora) for t in nuevas_b), f"{sesiones}/{sesiones}")
    comprobar("un token vencido se rechaza aunque la clave este publicada",
              not verificar_jwt(firmar_jwt(b, "k-b", "s2", ahora - 1), jwks_sin_vieja, ahora))
    comprobar("un kid desconocido se rechaza (nadie elige su propia clave)",
              not verificar_jwt(firmar_jwt(nueva(), "k-z", "s2", ahora + hora), jwks_sin_vieja, ahora))
    _ = serialization  # la clave privada nunca se serializa ni se escribe a disco en este ensayo


# ------------------------------------------------------------------ 4 · HMAC con dos claves
def hmac_con_ventana():
    print("== 4 · HMAC (INVITACIONES_CLAVE, WEBHOOK_SECRETO, ...): verificar con dos, firmar con una")
    k1, k2 = secrets.token_bytes(32), secrets.token_bytes(32)
    firma = lambda k, m: hmac.new(k, m, hashlib.sha256).hexdigest()  # noqa: E731
    mensajes = [f"invitacion-{i}".encode() for i in range(100)]
    emitidos = [(m, firma(k1, m)) for m in mensajes]

    def valida(claves, m, f):
        return any(hmac.compare_digest(firma(k, m), f) for k in claves)

    comprobar("sin ventana (solo la nueva) lo emitido con la vieja se rechaza",
              not any(valida([k2], m, f) for m, f in emitidos), "0/100 aceptados")
    comprobar("con ventana [nueva, vieja] todo lo emitido con la vieja sigue valido",
              all(valida([k2, k1], m, f) for m, f in emitidos), "100/100 aceptados")
    comprobar("lo nuevo se firma SOLO con la nueva y no valida con la vieja",
              not valida([k1], b"nuevo", firma(k2, b"nuevo")) and valida([k2, k1], b"nuevo", firma(k2, b"nuevo")))
    comprobar("retirada la vieja, la ventana se cierra y la filtrada no firma nada valido",
              not valida([k2], b"forjado", firma(k1, b"forjado")))


# ------------------------------------------------------------------ 5 · pimienta
def pimienta():
    print("== 5 · PIMIENTA (SEGURIDAD_PIMIENTA): migracion perezosa, no re-firma")
    p1, p2 = secrets.token_bytes(32), secrets.token_bytes(32)
    huella = lambda p, v: hashlib.sha256(p + v.encode()).hexdigest()  # noqa: E731
    almacen = {f"u{i}": huella(p1, f"codigo-{i}") for i in range(500)}
    comprobar("la huella guardada NO se puede convertir a la pimienta nueva sin el valor original",
              all(almacen[u] != huella(p2, f"codigo-{i}") for i, u in enumerate(almacen)),
              "hace falta que el usuario presente el valor")

    migrados = 0
    def entrar(u, valor):
        nonlocal migrados
        if hmac.compare_digest(almacen[u], huella(p2, valor)):
            return True
        if hmac.compare_digest(almacen[u], huella(p1, valor)):
            almacen[u] = huella(p2, valor)  # re-hash con la nueva al primer uso
            migrados += 1
            return True
        return False

    for i in range(0, 500, 5):
        entrar(f"u{i}", f"codigo-{i}")
    pendientes = sum(1 for u in almacen if hmac.compare_digest(almacen[u], huella(p1, f"codigo-{u[1:]}")))
    comprobar("durante la ventana entran con la vieja y se migran con la nueva", migrados == 100, f"{migrados} migrados")
    comprobar("se sabe cuantos faltan: la vieja NO se retira hasta que sea cero", pendientes == 400, f"{pendientes} pendientes")
    comprobar("un valor equivocado no entra con ninguna de las dos", not entrar("u1", "equivocado"))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--salida", help="escribe el registro del ensayo (JSON) en este archivo")
    args = ap.parse_args()
    t0 = time.time()
    inventario()
    jwt()
    hmac_con_ventana()
    pimienta()
    falla = [r for r in resultados if not r["ok"]]
    print(f"\ncomprobaciones: {len(resultados)} · fallas: {len(falla)} · defectos del repositorio encontrados: {len(defectos)}"
          f" · {round(time.time() - t0, 2)} s")
    if args.salida:
        pathlib.Path(args.salida).write_text(
            json.dumps({"resultados": resultados, "defectos": defectos}, ensure_ascii=False, indent=2), encoding="utf-8")
    print("ENSAYO DE ROTACION: " + ("FAIL" if falla else "PASS"))
    return 1 if falla else 0


if __name__ == "__main__":
    sys.exit(main())
