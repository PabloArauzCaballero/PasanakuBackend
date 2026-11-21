# Instrucciones comunes para las sesiones que ejecutan el plan de correcciones

Las lee cada sesión antes de tocar nada. Las decide Pablo, que es el dueño del producto. Hay que trabajar
**con muchísimo cuidado**: es preferible un cambio chico, probado y fácil de revisar a uno grande.

## Fuentes de verdad (leerlas en este orden)
1. `CLAUDE.md`, `AGENTS.md` y `.claude/rules/` completos. Mandan sobre todo lo demás.
2. `docs/trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md`, con todas las decisiones de Pablo.
   No se inventa ninguna regla de negocio.
3. `docs/trabajo/2026-11-21-plan-correcciones/PLAN.md` y `PLAN-PROVEEDORES.md`.
4. El anexo de tu lote en `docs/trabajo/2026-11-21-plan-correcciones/anexos/`. Ahí está, para cada
   hallazgo, la ficha del informe de auditoría de origen, en la rama `origin/auditoria/<modulo>-2026-11-20`.

## Base y entrega
- Trabajá sobre `origin/dev` actualizada, **no** sobre `test`. La auditoría se hizo sobre `test` (07fc6971)
  y `dev` tiene 29 commits más: antes de corregir, **volvé a ubicar cada hallazgo en `dev`**. Si en `dev`
  ya está corregido, la microtarea se cierra con esa evidencia, sin cambiar código.
- **Un PR por microtarea o por grupo chico de microtareas que comparten causa**, hacia `dev`. El título
  lleva el ID (por ejemplo, `F1.S1.M1`). La descripción tiene el CA, el DoD con la salida pegada, lo que
  no quedó cubierto y la línea `Largo plazo: <respuesta>`. **No mergees**: revisa y mergea Pablo.
- Solo tocás los archivos de tu alcance. Si encontrás algo roto fuera de él, lo anotás en tu
  `REPORTE.md` y no lo arreglás.
- El esquema se genera: `sql/` sale de `docs/entidades/*.puml` y de `docs/Restricciones.md` mediante
  `python3 scripts/generar_ddl.py` (ver `sql/README.md`). **No edites `sql/` a mano.** Cambiá la fuente
  y regenerá.

## Entorno de verificación en la nube (probado el 2026-11-21)
El contenedor trae Java 21, Gradle por `./gradlew`, Node y Yarn, y PostgreSQL 16 instalado pero apagado.
Docker **no** tiene demonio.
```bash
D=/var/lib/postgresql/pasanaku-local; B=/usr/lib/postgresql/16/bin
mkdir -p $D && chown postgres $D
runuser -u postgres -- $B/initdb -D $D/data -U postgres -A trust
runuser -u postgres -- $B/pg_ctl -D $D/data -o "-p 5433 -c listen_addresses=127.0.0.1 -k /tmp" -l $D/log.txt start
psql -h 127.0.0.1 -p 5433 -U postgres -c "CREATE ROLE pasanaku LOGIN SUPERUSER PASSWORD 'pasanaku';" -c "CREATE DATABASE pasanaku OWNER pasanaku;"
cd sql && PGPASSWORD=pasanaku psql -h 127.0.0.1 -p 5433 -U pasanaku -d pasanaku -v ON_ERROR_STOP=1 -q -f aplicar.sql
```
- Con eso funcionan la generación de jOOQ, `compileJava` y `./gradlew :servicios:<s>:test` (en `aportes`
  pasaron 53 pruebas). Si Maven Central responde 429, reintentá una vez.
- Las invariantes de la base (triggers, `CHECK`, `REVOKE`) se prueban **directamente con `psql`**
  contra esa base, y la salida se pega en el PR.
- Las pruebas con Testcontainers (`BaseDePrueba`) necesitan Docker: **no corren acá**. Declaralas
  `NO COMPROBADO — requiere Docker (máquina de Pablo)` y dejá listo el comando exacto para correrlas.
- Al terminar, apagá la base:
  `runuser -u postgres -- $B/pg_ctl -D $D/data stop`.
- Recursos: como máximo un build de Gradle a la vez dentro de tu sesión.

## Cuidado especial
- Dinero: regla 91 completa. Los cinco puntos de 91.6 en el PR cuando toques importes.
- Cada cambio de autorización lleva su matriz negativa: otro usuario, otro grupo, rol insuficiente y
  sin token.
- Está prohibido debilitar o saltear pruebas. Si una prueba existente valida el flujo viejo (respaldo,
  aprobación), se reemplaza con justificación en el PR, nunca se borra en silencio.
- Nada de datos personales ni secretos en código, PR o reportes.
- Escribí tu `PLAN.md` y tu `REPORTE.md` en `docs/trabajo/2026-11-21-<tu-carril>/` (reglas 20 y 40).
