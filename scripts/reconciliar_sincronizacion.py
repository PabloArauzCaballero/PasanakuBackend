#!/usr/bin/env python3
"""Reconciliacion y auto-recuperacion de datos entre microservicios.

Corrige discrepancias de sincronizacion entre esquemas:
  1. Abre billeteras en nucleo_financiero para usuarios de identidad que no tengan cuenta.
  2. Abre billeteras en nucleo_financiero para grupos de grupos que no tengan cuenta.
  3. Desbloquea eventos en estado FALLIDO o TOMADOS huerfanos en el Outbox.

Uso:
    python3 scripts/reconciliar_sincronizacion.py [--aplicar]
"""

import argparse
import os
import subprocess
import sys


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

    def execute(self, sql):
        env = os.environ.copy()
        env["PGPASSWORD"] = self.password
        cmd = [
            "psql",
            "-h", self.host,
            "-p", self.port,
            "-U", self.user,
            "-d", self.db,
            "-c", sql,
        ]
        res = subprocess.run(cmd, env=env, capture_output=True, text=True)
        if res.returncode != 0:
            raise RuntimeError(res.stderr.strip() or "Error ejecutando psql")
        return res.stdout.strip()


def reconciliar_usuarios(db: DbRunner, aplicar: bool):
    print("\n--- 1. Reconciliacion: Usuarios sin Billetera ---")
    sql_buscar = """
        SELECT u.id, u.codigo_publico
        FROM identidad.usuario u
        LEFT JOIN nucleo_financiero.cuenta_billetera cb 
               ON cb.usuario_id = u.id AND cb.tipo = 'USUARIO'
        WHERE cb.id IS NULL;
    """
    filas = db.query(sql_buscar)
    if not filas:
        print("[OK] Todos los usuarios tienen billetera activa.")
        return

    print(f"Detectados {len(filas)} usuarios sin billetera:")
    for f in filas:
        uid, cod = f.split("|")
        print(f"  * Usuario {cod} ({uid})")

    if not aplicar:
        print("  -> Ejecuta con --aplicar para crear las billeteras faltantes.")
        return

    # Obtener politica vigente
    politicas = db.query("SELECT id FROM nucleo_financiero.politica_billetera WHERE moneda = 'BOB' LIMIT 1;")
    politica_id = politicas[0] if politicas else None
    if not politica_id:
        print("[ERROR] No hay politica de billetera en BOB en nucleo_financiero.politica_billetera")
        return

    insert_sql = f"""
        INSERT INTO nucleo_financiero.cuenta_billetera (
            id, numero_cuenta, tipo, usuario_id, moneda, estado, 
            nivel_debida_diligencia, politica_billetera_id, saldo_disponible, 
            saldo_retenido, permite_saldo_negativo, fecha_apertura, version
        )
        SELECT 
            gen_random_uuid(),
            'BOB-' || substring(replace(u.id::text, '-', '') from 17 for 16),
            'USUARIO',
            u.id,
            'BOB',
            'ACTIVA',
            'SIMPLIFICADA',
            '{politica_id}'::uuid,
            0.00,
            0.00,
            false,
            now(),
            0
        FROM identidad.usuario u
        LEFT JOIN nucleo_financiero.cuenta_billetera cb 
               ON cb.usuario_id = u.id AND cb.tipo = 'USUARIO'
        WHERE cb.id IS NULL;
    """
    res = db.execute(insert_sql)
    print(f"  [EXITO] Billeteras creadas para usuarios: {res}")


def reconciliar_grupos(db: DbRunner, aplicar: bool):
    print("\n--- 2. Reconciliacion: Grupos sin Billetera ---")
    sql_buscar = """
        SELECT g.id, g.codigo_publico, g.nombre
        FROM grupos.grupo g
        LEFT JOIN nucleo_financiero.cuenta_billetera cb 
               ON cb.grupo_id = g.id AND cb.tipo = 'GRUPO'
        WHERE cb.id IS NULL;
    """
    filas = db.query(sql_buscar)
    if not filas:
        print("[OK] Todos los grupos tienen billetera activa.")
        return

    print(f"Detectados {len(filas)} grupos sin billetera:")
    for f in filas:
        gid, cod, nom = f.split("|")
        print(f"  * Grupo {cod} ({nom}) [{gid}]")

    if not aplicar:
        print("  -> Ejecuta con --aplicar para crear las billeteras faltantes.")
        return

    insert_sql = """
        INSERT INTO nucleo_financiero.cuenta_billetera (
            id, numero_cuenta, tipo, grupo_id, moneda, estado, 
            nivel_debida_diligencia, saldo_disponible, 
            saldo_retenido, permite_saldo_negativo, fecha_apertura, version
        )
        SELECT 
            gen_random_uuid(),
            'GRP-' || substring(replace(g.id::text, '-', '') from 17 for 16),
            'GRUPO',
            g.id,
            'BOB',
            'ACTIVA',
            'ESTANDAR',
            0.00,
            0.00,
            false,
            now(),
            0
        FROM grupos.grupo g
        LEFT JOIN nucleo_financiero.cuenta_billetera cb 
               ON cb.grupo_id = g.id AND cb.tipo = 'GRUPO'
        WHERE cb.id IS NULL;
    """
    res = db.execute(insert_sql)
    print(f"  [EXITO] Billeteras creadas para grupos: {res}")


def reconciliar_outbox(db: DbRunner, aplicar: bool):
    print("\n--- 3. Reconciliacion: Eventos de Outbox Fallidos o Huerfanos ---")
    esquemas = [
        "aportes", "auditoria", "cumplimiento", "entregas", "erp",
        "garantia", "grupos", "identidad", "inversiones", "notificaciones",
        "nucleo_financiero", "organizador", "publicidad", "tarifas", "transparencia"
    ]
    total_rescatables = 0
    for esq in esquemas:
        try:
            sql = f"""
                SELECT count(*) 
                FROM {esq}.evento_dominio 
                WHERE estado = 'FALLIDO' 
                   OR (estado = 'TOMADO' AND tomado_en < now() - interval '5 minutes');
            """
            out = db.query(sql)
            c = int(out[0]) if out else 0
            if c > 0:
                total_rescatables += c
                print(f"  * Esquema {esq}: {c} eventos atascados.")
                if aplicar:
                    db.execute(f"""
                        UPDATE {esq}.evento_dominio 
                        SET estado = 'PENDIENTE', intentos = 0, proximo_intento_en = now()
                        WHERE estado = 'FALLIDO' 
                           OR (estado = 'TOMADO' AND tomado_en < now() - interval '5 minutes');
                    """)
                    print(f"    -> {c} eventos reactivados a PENDIENTE.")
        except Exception as e:
            pass

    if total_rescatables == 0:
        print("[OK] No hay eventos atascados en Outbox.")
    elif not aplicar:
        print("  -> Ejecuta con --aplicar para reactivar los eventos a PENDIENTE.")


def main():
    parser = argparse.ArgumentParser(description="Reconciliacion de datos entre esquemas")
    parser.add_argument("--aplicar", action="store_true", help="Aplica las correcciones en la base de datos")
    parser.add_argument("--host", default=os.getenv("PGHOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.getenv("APORTAYA_PG_PORT", "5433")))
    parser.add_argument("--db", default=os.getenv("PGDATABASE", "pasanaku"))
    parser.add_argument("--user", default=os.getenv("PGUSER", "pasanaku"))
    parser.add_argument("--password", default=os.getenv("PGPASSWORD", "pasanaku"))
    args = parser.parse_args()

    db = DbRunner(args.host, args.port, args.db, args.user, args.password)
    print("=" * 60)
    print(f" RECONCILIADOR DE SINCRONIZACION APORTAYA")
    print(f" Base de datos: {args.host}:{args.port}/{args.db}")
    print(f" Modo: {'APLICAR CAMBIOS' if args.aplicar else 'SOLO LECTURA (DRY-RUN)'}")
    print("=" * 60)

    reconciliar_usuarios(db, args.aplicar)
    reconciliar_grupos(db, args.aplicar)
    reconciliar_outbox(db, args.aplicar)

    print("\n" + "=" * 60)
    print(" RECONCILIACION FINALIZADA")
    print("=" * 60 + "\n")


if __name__ == "__main__":
    main()
