package bo.aportaya.grupos;

import bo.aportaya.grupos.aplicacion.CU69IniciarGrupo;
import bo.aportaya.grupos.infraestructura.AdmisionRepositorio;
import bo.aportaya.grupos.infraestructura.CalendarioDelGrupoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;

/** El armado comun de las pruebas del inicio al 75 % (CU-69, 2026-10-30): el caso de uso y un grupo abierto a medida. */
abstract class BaseDeInicio extends BaseDeCU60 {

    private static final int PORCENTAJE = 75;

    protected CU69IniciarGrupo inicio;

    @BeforeEach
    void prepararInicio() {
        inicio = new CU69IniciarGrupo(
                new Datos(dsl),
                new AdmisionRepositorio(),
                new CalendarioDelGrupoRepositorio(),
                Reloj.delSistema(),
                new Outbox("grupos"),
                PORCENTAJE);
    }

    protected static ContextoSesion de(UUID usuario) {
        return ContextoSesion.de(
                usuario, "ORGANIZADOR", new Traza(UUID.randomUUID().toString()));
    }

    protected CU69IniciarGrupo.Iniciado iniciar(UUID grupo, UUID clave, UUID quien) {
        return transaccion.execute(t -> inicio.iniciar(grupo, clave, de(quien)));
    }

    /**
     * Un grupo ABIERTO_A_INSCRIPCION de {@code totales} cupos. El organizador (cupo 1) y los demas integrantes que
     * firmaron ocupan los numeros de {@code ocupados}; los de {@code reservados} quedan RESERVADO; el resto, LIBRE.
     */
    protected UUID grupoAbierto(int totales, Set<Integer> ocupados, Set<Integer> reservados) {
        UUID grupo = fixtura.grupoConformado(totales);
        dsl.execute(
                "UPDATE grupos.grupo SET estado = 'ABIERTO_A_INSCRIPCION', cupos_ocupados = ? WHERE id = ?",
                (short) (ocupados.size() + reservados.size()),
                grupo);
        for (int numero = 1; numero <= totales; numero++) {
            boolean ocupado = ocupados.contains(numero);
            boolean reservado = reservados.contains(numero);
            UUID participante = null;
            if (ocupado || reservado) {
                participante = UUID.randomUUID();
                dsl.execute(
                        """
                        INSERT INTO grupos.participante
                          (id, grupo_id, usuario_id, estado, es_organizador, fecha_ingreso,
                           reputacion_al_ingresar, aportes_realizados, aportes_en_mora)
                        VALUES (?, ?, ?, ?, ?, now(), 0, 0, 0)
                        """,
                        participante,
                        grupo,
                        numero == 1 ? organizador : fixtura.usuario(),
                        ocupado ? "ACTIVO" : "ACEPTADO_PENDIENTE_FIRMA",
                        numero == 1);
            }
            dsl.execute(
                    """
                    INSERT INTO grupos.cupo (id, grupo_id, numero, participante_id, estado, fraccion, asignado_en)
                    VALUES (gen_random_uuid(), ?, ?, ?, ?, 1.00, now())
                    """,
                    grupo,
                    (short) numero,
                    participante,
                    ocupado ? "OCUPADO" : reservado ? "RESERVADO" : "LIBRE");
        }
        return grupo;
    }

    protected static Set<Integer> del1Al(int n) {
        Set<Integer> numeros = new java.util.TreeSet<>();
        for (int i = 1; i <= n; i++) numeros.add(i);
        return numeros;
    }

    protected Map<String, Object> grupo(UUID id) {
        return dsl.fetchOne(
                        "SELECT estado, cupos_totales, num_periodos, cupos_ocupados FROM grupos.grupo WHERE id = ?", id)
                .intoMap();
    }

    protected static String codigo(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    protected int contar(String consulta, Object... parametros) {
        return ((Number) dsl.fetchOne(consulta, parametros).get(0)).intValue();
    }
}
