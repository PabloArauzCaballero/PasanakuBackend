package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU68AceptarIngreso;
import bo.aportaya.grupos.aplicacion.CU69EnlaceDeGrupo;
import bo.aportaya.grupos.aplicacion.CU69Invitar;
import bo.aportaya.grupos.aplicacion.CanjearInvitacion;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.grupos.web.generado.modelo.ActividadEnGrupos;
import bo.aportaya.grupos.web.generado.modelo.AliasResuelto;
import bo.aportaya.grupos.web.generado.modelo.PaqueteDelSorteo;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.Publico;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

/**
 * Lo que este servicio le contesta a los demas para que ellos tampoco tengan que leer su esquema: no son casos de uso.
 * Salio de {@link GruposController} (limite de 300 lineas) sin cambiar ninguna ruta.
 */
abstract class RutasAOtrosServicios extends RutasDeSubastaDelCupo {

    /** El proceso que atiende lo publico: fijo, para poder leerlo en la bitacora. */
    private static final UUID PROCESO_PUBLICO = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

    private final SesionDeLaPeticion sesion;
    private final RespuestasAOtrosServicios respuestas;

    @SuppressWarnings("checkstyle:ParameterNumber")
    protected RutasAOtrosServicios(
            AdmisionDelGrupo admision,
            SesionDeLaPeticion sesion,
            CU68AceptarIngreso cu68Decision,
            InvitacionesWeb enlaces,
            CU69Invitar invitaciones,
            CanjearInvitacion canjes,
            Consultas consultas,
            HechosDeOtrosServicios afuera,
            BigDecimal afinidad,
            CU69EnlaceDeGrupo enlacesDeGrupo,
            RespuestasAOtrosServicios respuestas) {
        super(
                admision,
                sesion,
                cu68Decision,
                enlaces,
                invitaciones,
                canjes,
                consultas,
                afuera,
                afinidad,
                enlacesDeGrupo);
        this.sesion = sesion;
        this.respuestas = respuestas;
    }

    @Override
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<AliasResuelto> resolverAlias(String alias) {
        Traza.marcarCasoDeUso("CU-12", alias);
        return respuestas.resolverAlias(alias, sesion.actual());
    }

    @Override
    @Permiso("BILLETERA_VER")
    public ResponseEntity<ActividadEnGrupos> consultarActividad(UUID usuarioId) {
        Traza.marcarCasoDeUso("CU-16", usuarioId.toString());
        return respuestas.consultarActividad(usuarioId, sesion.actual());
    }

    @Override
    @Publico("CU-61: el paquete del sorteo es publico por diseno; poder recomputarlo desde afuera es su sentido")
    public ResponseEntity<PaqueteDelSorteo> consultarPaqueteDelSorteo(UUID sorteoId) {
        Traza.marcarCasoDeUso("CU-61", sorteoId.toString());
        // Sin sesion: lo atiende el proceso publico, con rol de sistema (lectura de un sorteo).
        var contextoPublico = bo.aportaya.plataforma.dominio.ContextoSesion.deSistema(
                PROCESO_PUBLICO,
                new bo.aportaya.plataforma.dominio.Traza(UUID.randomUUID().toString()));
        return respuestas.consultarPaqueteDelSorteo(sorteoId, contextoPublico);
    }
}
