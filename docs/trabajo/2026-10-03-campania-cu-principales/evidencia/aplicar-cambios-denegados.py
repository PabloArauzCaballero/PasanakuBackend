"""Aplica los tres cambios que el sistema de permisos de Claude Code denegó (B26, alias, B22).

LO EJECUTA EL USUARIO, no el asistente:
    ! python docs/trabajo/2026-10-03-campania-cu-principales/evidencia/aplicar-cambios-denegados.py

Qué hace (decisiones del usuario, 2026-10-03):
  1. B26  — `GET /grupos/sorteos/{id}/paquete` pasa a @Publico: el paquete es publico por diseno de CU-61
            (poder recomputar el sorteo desde afuera es su sentido). Se atiende con rol de sistema.
  2. Alias — la busqueda del alias entre participantes de OTROS corre con rol de sistema, acotada a
            devolver solo un id de usuario (Datos.comoSistema).
  3. B22  — votar resuelve el PARTICIPANTE del usuario en el grupo del acuerdo (la FK del voto apunta a
            `participante`, no a `usuario`).

Es todo o nada: lee y comprueba los anclajes de los cuatro archivos y solo entonces escribe. Si un anclaje
no esta (el codigo cambio), no toca nada y lo dice. Correrlo dos veces tampoco rompe: avisa que ya esta.
"""
import pathlib
import re
import sys

RAIZ = pathlib.Path(__file__).resolve().parents[4]
G = RAIZ / "servicios/grupos/src/main/java/bo/aportaya/grupos"
ARCHIVOS = {
    "controlador": G / "web/GruposController.java",
    "repositorio": G / "infraestructura/ConsultasRepositorio.java",
    "consultas": G / "aplicacion/Consultas.java",
    "acuerdos": G / "web/AcuerdosController.java",
}
texto = {k: p.read_text(encoding="utf-8") for k, p in ARCHIVOS.items()}


def falla(msg):
    print("NO SE ESCRIBIO NADA —", msg)
    sys.exit(1)


def reemplazar(clave, viejo, nuevo, ya_hecho):
    if ya_hecho in texto[clave]:
        print(f"  ya aplicado: {ya_hecho[:60]!r}")
        return
    if viejo not in texto[clave]:
        falla(f"anclaje no encontrado en {ARCHIVOS[clave].name}: {viejo[:70]!r}")
    texto[clave] = texto[clave].replace(viejo, nuevo, 1)


# ---- 1. B26: paquete del sorteo publico ------------------------------------------------------------
reemplazar(
    "controlador",
    '''    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<PaqueteDelSorteo> consultarPaqueteDelSorteo(UUID sorteoId) {
        Traza.marcarCasoDeUso("CU-61", sorteoId.toString());
        return respuestas.consultarPaqueteDelSorteo(sorteoId, sesion.actual());''',
    '''    @Override
    @Publico("CU-61: el paquete del sorteo es publico por diseno; poder recomputarlo desde afuera es su sentido")
    public ResponseEntity<PaqueteDelSorteo> consultarPaqueteDelSorteo(UUID sorteoId) {
        Traza.marcarCasoDeUso("CU-61", sorteoId.toString());
        // Sin sesion: lo atiende el proceso publico, con rol de sistema (lectura de un sorteo).
        var contextoPublico = bo.aportaya.plataforma.dominio.ContextoSesion.deSistema(
                PROCESO_PUBLICO, new bo.aportaya.plataforma.dominio.Traza(UUID.randomUUID().toString()));
        return respuestas.consultarPaqueteDelSorteo(sorteoId, contextoPublico);''',
    "contextoPublico",
)
if "import bo.aportaya.plataforma.web.seguridad.Publico;" not in texto["controlador"]:
    if "import bo.aportaya.plataforma.web.seguridad.Permiso;" not in texto["controlador"]:
        falla("no encuentro el import de Permiso en GruposController para colocar el de Publico")
    texto["controlador"] = texto["controlador"].replace(
        "import bo.aportaya.plataforma.web.seguridad.Permiso;",
        "import bo.aportaya.plataforma.web.seguridad.Permiso;\nimport bo.aportaya.plataforma.web.seguridad.Publico;",
        1,
    )
if "PROCESO_PUBLICO =" not in texto["controlador"]:
    m = re.search(r"public class GruposController[^{]*\{\n", texto["controlador"])
    if not m:
        falla("no encuentro la declaracion de GruposController")
    texto["controlador"] = (
        texto["controlador"][: m.end()]
        + "\n    /** El proceso que atiende lo publico: fijo, para poder leerlo en la bitacora. */\n"
        + '    private static final UUID PROCESO_PUBLICO = UUID.fromString("00000000-0000-0000-0000-0000000000f1");\n'
        + texto["controlador"][m.end():]
    )

# ---- 2. Alias con rol de sistema -------------------------------------------------------------------
reemplazar(
    "repositorio",
    '''    public Optional<UUID> usuarioDelAlias(DSLContext dsl, String alias) {
        var fila = dsl.fetchOne(
                """
                SELECT p.usuario_id AS usuario
                  FROM grupos.participante p
                 WHERE p.alias = ? AND p.estado = 'ACTIVO'
                 LIMIT 1
                """,
                alias);''',
    '''    public Optional<UUID> usuarioDelAlias(DSLContext dsl, String alias) {
        // Busca entre los participantes de OTROS: la politica de fila deja al titular ver solo
        // lo suyo. La consulta devuelve unicamente un id de usuario, no la fila, y por eso es
        // el unico lugar donde se lee con rol de sistema.
        var fila = bo.aportaya.plataforma.datos.Datos.comoSistema(dsl, () -> dsl.fetchOne(
                """
                SELECT p.usuario_id AS usuario
                  FROM grupos.participante p
                 WHERE p.alias = ? AND p.estado = 'ACTIVO'
                 LIMIT 1
                """,
                alias));''',
    "Datos.comoSistema(dsl, () -> dsl.fetchOne(",
)

# ---- 3. B22: el voto lo emite un PARTICIPANTE -------------------------------------------------------
reemplazar(
    "repositorio",
    "    /** El grupo al que pertenece un turno. */",
    '''    /** El grupo de un acuerdo. */
    public Optional<UUID> grupoDelAcuerdo(DSLContext dsl, UUID acuerdoId) {
        var fila = dsl.fetchOne("SELECT grupo_id FROM grupos.acuerdo WHERE id = ?", acuerdoId);
        return fila == null ? Optional.empty() : Optional.ofNullable(fila.get("grupo_id", UUID.class));
    }

    /** El grupo al que pertenece un turno. */''',
    "grupoDelAcuerdo(DSLContext dsl",
)
reemplazar(
    "consultas",
    "    @Transactional(readOnly = true)\n    public Optional<UUID> grupoDelTurno(UUID turnoId, ContextoSesion ctx) {",
    '''    @Transactional(readOnly = true)
    public Optional<UUID> grupoDelAcuerdo(UUID acuerdoId, ContextoSesion ctx) {
        return datos.conContexto(ctx, dsl -> consultas.grupoDelAcuerdo(dsl, acuerdoId));
    }

    @Transactional(readOnly = true)
    public Optional<UUID> grupoDelTurno(UUID turnoId, ContextoSesion ctx) {''',
    "grupoDelAcuerdo(UUID acuerdoId",
)
reemplazar(
    "acuerdos",
    "        cu63.votar(acuerdoId, ctx.usuarioId(), cuerpo.getVoto().getValue(), ctx);",
    '''        // Vota un PARTICIPANTE del grupo del acuerdo, no un usuario: la FK del voto apunta a
        // `participante`. Quien no participa de ese grupo no vota.
        UUID grupoId = consultas
                .grupoDelAcuerdo(acuerdoId, ctx)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(63, 1), "Ese acuerdo no existe."));
        UUID participanteId = consultas
                .participanteDe(grupoId, ctx.usuarioId(), ctx)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(63, 1), "No participas de ese grupo."));

        cu63.votar(acuerdoId, participanteId, cuerpo.getVoto().getValue(), ctx);''',
    "participanteId, cuerpo.getVoto()",
)

for clave, ruta in ARCHIVOS.items():
    ruta.write_text(texto[clave], encoding="utf-8")
print("Listo: B26 (paquete publico), alias (rol de sistema acotado) y B22 (voto por participante) aplicados.")
print("Siguiente: ./gradlew :servicios:grupos:spotlessApply :servicios:grupos:test :servicios:grupos:webTest")
