#!/usr/bin/env python3
"""Diagnostico de sincronizacion entre esquemas y salud de Outbox / Kafka.

Verifica la paridad de datos entre los 15 microservicios de AportaYa:
  1. Usuarios en identidad vs Billeteras de usuario en nucleo_financiero
  2. Grupos en grupos vs Billeteras de grupo en nucleo_financiero
  3. Aportes pagados en aportes vs Transacciones/Asientos en nucleo_financiero
  4. Eventos atascados en Outbox (PENDIENTES viejos, TOMADOS huerfanos, FALLIDOS)
  5. Conteo de eventos consumidos por consumidor

Uso:
    python3 scripts/diagnostico_sincronizacion.py [--host HOST] [--port PORT]
"""

import argparse
import os
import subprocess
import sys

ESQUEMAS = [
    "aportes",
    "auditoria",
    "cumplimiento",
    "entregas",
    "erp",
    "garantia",
    "grupos",
    "identidad",
    "inversiones",
    "notificaciones",
    "nucleo_financiero",
    "organizador",
    "publicidad",
    "tarifas",
    "transparencia",
]


class DbRunner:
    def __init__(self, host, port, db, user, password):
        self.host = host
        self.port = str(port)
        self.db = db
        self.user = user
        self.password = password

    def query(self, sql):
        env = os.environ.copy()
        env["PGPASSWORD"] = self.password
        cmd = [
            "psql",
            "-h", self.host,
            "-p", self.port,
            "-U", self.user,
            "-d", self.db,
            "-t",
            "-A",
            "-F", "|",
            "-c", sql,
        ]
        res = subprocess.run(cmd, env=env, capture_output=True, text=True)
        if res.returncode != 0:
            raise RuntimeError(res.stderr.strip() or "Error ejecutando psql")
        lineas = [l.strip() for l in res.stdout.strip().split("\n") if l.strip()]
        return lineas


def verificar_outbox(db: DbRunner):
    print("\n" + "=" * 75)
    print(" 1. SALUD DEL OUTBOX TRANSACCIONAL (TODOS LOS ESQUEMAS)")
    print("=" * 75)
    print(f"{'Esquema':<20} | {'Pendientes':<10} | {'Tomados':<10} | {'Publicados':<10} | {'Fallidos':<10}")
    print("-" * 75)

    total_p = 0
    total_f = 0
    total_h = 0

    for esquema in ESQUEMAS:
        try:
            sql = f"""
                SELECT 
                    coalesce(count(*) FILTER (WHERE estado = 'PENDIENTE'), 0),
                    coalesce(count(*) FILTER (WHERE estado = 'TOMADO'), 0),
                    coalesce(count(*) FILTER (WHERE estado = 'PUBLICADO'), 0),
                    coalesce(count(*) FILTER (WHERE estado = 'FALLIDO'), 0),
                    coalesce(count(*) FILTER (WHERE estado = 'TOMADO' AND tomado_en < now() - interval '5 minutes'), 0)
                FROM {esquema}.evento_dominio;
            """
            out = db.query(sql)
            if out:
                partes = out[0].split("|")
                p, t, pub, f, h = map(int, partes)
                total_p += p
                total_f += f
                total_h += h

                aviso = ""
                if f > 0:
                    aviso = f" <- [!] {f} FALLIDOS"
                elif h > 0:
                    aviso = f" <- [!] {h} HUERFANOS"

                print(f"{esquema:<20} | {p:<10} | {t:<10} | {pub:<10} | {f:<10}{aviso}")
        except Exception as e:
            print(f"{esquema:<20} | (Error: {e})")

    print("-" * 75)
    print(f"Resumen Outbox: Pendientes={total_p}, Fallidos={total_f}, Huerfanos={total_h}")
    if total_f > 0:
        print(" [ALERTA] Hay eventos FALLIDOS. El nuevo mecanismo rescatarFallidos() los reintentara automaticamente.")


def verificar_usuarios_billeteras(db: DbRunner):
    print("\n" + "=" * 75)
    print(" 2. PARIDAD: IDENTIDAD (USUARIOS) vs NUCLEO FINANCIERO (BILLETERAS)")
    print("=" * 75)
    try:
        u_out = db.query("SELECT count(*) FROM identidad.usuario;")
        total_usuarios = int(u_out[0]) if u_out else 0

        b_out = db.query("SELECT count(DISTINCT usuario_id) FROM nucleo_financiero.cuenta_billetera WHERE tipo = 'USUARIO';")
        total_billeteras = int(b_out[0]) if b_out else 0

        print(f"Usuarios registrados en identidad:        {total_usuarios}")
        print(f"Billeteras creadas en nucleo_financiero: {total_billeteras}")

        huerfanos = db.query("""
            SELECT u.id || ' | ' || u.fecha_registro
            FROM identidad.usuario u
            LEFT JOIN nucleo_financiero.cuenta_billetera cb 
                   ON cb.usuario_id = u.id AND cb.tipo = 'USUARIO'
            WHERE cb.id IS NULL
            LIMIT 5;
        """)
        if huerfanos:
            print(f" [DESINCRONIZACION] {len(huerfanos)} usuarios sin billetera:")
            for h in huerfanos:
                print(f"   - {h}")
        else:
            print(" [OK] Paridad completa: todos los usuarios tienen billetera.")
    except Exception as e:
        print(f"Error verificando usuarios vs billeteras: {e}")


def verificar_grupos_billeteras(db: DbRunner):
    print("\n" + "=" * 75)
    print(" 3. PARIDAD: GRUPOS vs NUCLEO FINANCIERO (BILLETERAS DE GRUPO)")
    print("=" * 75)
    try:
        g_out = db.query("SELECT count(*) FROM grupos.grupo;")
        total_grupos = int(g_out[0]) if g_out else 0

        bg_out = db.query("SELECT count(DISTINCT grupo_id) FROM nucleo_financiero.cuenta_billetera WHERE tipo = 'GRUPO';")
        total_billeteras_grupo = int(bg_out[0]) if bg_out else 0

        print(f"Grupos creados en grupos:                {total_grupos}")
        print(f"Billeteras creadas en nucleo_financiero: {total_billeteras_grupo}")

        huerfanos = db.query("""
            SELECT g.id || ' (' || g.nombre || ') | ' || coalesce(g.fecha_inicio::text, 'sin fecha')
            FROM grupos.grupo g
            LEFT JOIN nucleo_financiero.cuenta_billetera cb 
                   ON cb.grupo_id = g.id AND cb.tipo = 'GRUPO'
            WHERE cb.id IS NULL
            LIMIT 5;
        """)
        if huerfanos:
            print(f" [DESINCRONIZACION] {len(huerfanos)} grupos sin billetera:")
            for h in huerfanos:
                print(f"   - {h}")
        else:
            print(" [OK] Paridad completa: todos los grupos tienen su billetera.")
    except Exception as e:
        print(f"Error verificando grupos vs billeteras: {e}")


def verificar_aportes_contabilidad(db: DbRunner):
    print("\n" + "=" * 75)
    print(" 4. PARIDAD: APORTES PAGADOS vs MOVIMIENTOS EN BILLETERA")
    print("=" * 75)
    try:
        p_out = db.query("SELECT count(*) FROM aportes.pago WHERE estado IN ('ACREDITADO', 'PAGADO');")
        pagos = int(p_out[0]) if p_out else 0

        tx_out = db.query("SELECT count(*) FROM nucleo_financiero.transaccion_billetera WHERE tipo = 'APORTE_A_GRUPO';")
        txs = int(tx_out[0]) if tx_out else 0

        print(f"Pagos acreditados/pagados en aportes:    {pagos}")
        print(f"Transacciones registradas en billetera:  {txs}")

        if pagos != txs:
            print(f" [DESINCRONIZACION] Hay {abs(pagos - txs)} pagos sin transaccion contable.")
        else:
            print(" [OK] Paridad completa: pagos acreditados coinciden con transacciones.")
    except Exception as e:
        print(f"Error verificando aportes vs contabilidad: {e}")


def main():
    parser = argparse.ArgumentParser(description="Diagnostico de sincronizacion entre esquemas")
    parser.add_argument("--host", default=os.getenv("PGHOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("APORTAYA_PG_PORT", "5433")))
    parser.add_argument("--db", default=os.getenv("PGDATABASE", "pasanaku"))
    parser.add_argument("--user", default=os.getenv("PGUSER", "pasanaku"))
    parser.add_argument("--password", default=os.getenv("PGPASSWORD", "pasanaku"))
    args = parser.parse_args()

    db = DbRunner(args.host, args.port, args.db, args.user, args.password)
    print(f"Conectando a PostgreSQL en {args.host}:{args.port}/{args.db}...")

    verificar_outbox(db)
    verificar_usuarios_billeteras(db)
    verificar_grupos_billeteras(db)
    verificar_aportes_contabilidad(db)

    print("\n" + "=" * 75)
    print(" DIAGNOSTICO DE SINCRONIZACION COMPLETADO")
    print("=" * 75 + "\n")


if __name__ == "__main__":
    main()
