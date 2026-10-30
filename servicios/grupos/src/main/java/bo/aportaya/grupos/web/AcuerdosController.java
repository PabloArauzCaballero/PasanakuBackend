package bo.aportaya.grupos.web;

import bo.aportaya.grupos.aplicacion.CU63Acordar;
import bo.aportaya.grupos.aplicacion.Consultas;
import bo.aportaya.grupos.dominio.TipoDeAcuerdo;
import bo.aportaya.grupos.web.generado.AcuerdosApi;
import bo.aportaya.grupos.web.generado.modelo.AcuerdoDelGrupo;
import bo.aportaya.grupos.web.generado.modelo.EntradaAcuerdo;
import bo.aportaya.grupos.web.generado.modelo.EntradaVoto;
import bo.aportaya.grupos.web.generado.modelo.SalidaAcuerdo;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las paginas de {@code /acuerdos}: listar, proponer y votar.
 *
 * <p>El quorum no lo elige quien propone: sale del reglamento del grupo. Si lo pusiera
 * el proponente, cualquiera podria abrir una votacion con el quorum que le convenga y
 * el acuerdo colectivo dejaria de serlo.
 */
@RestController
public class AcuerdosController implements AcuerdosApi {

    private final CU63Acordar cu63;
    private final Consultas consultas;
    private final SesionDeLaPeticion sesion;
    private final Reloj reloj;

    public AcuerdosController(CU63Acordar cu63, Consultas consultas, SesionDeLaPeticion sesion, Reloj reloj) {
        this.cu63 = cu63;
        this.consultas = consultas;
        this.sesion = sesion;
        this.reloj = reloj;
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaAcuerdo> proponerAcuerdo(UUID idempotencyKey, EntradaAcuerdo cuerpo) {
        var ctx = sesion.actual();
        Traza.marcarCasoDeUso("CU-63", cuerpo.getTipo().getValue());

        var politica = consultas
                .politicaDelGrupo(cuerpo.getGrupoId(), ctx)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(63, 1), "Ese grupo no existe."));

        UUID acuerdoId = cu63.proponer(
                new CU63Acordar.EntradaPropuesta(
                        cuerpo.getGrupoId(),
                        cuerpo.getTipo().getValue(),
                        cuerpo.getDescripcion(),
                        ctx.usuarioId(),
                        politica.quorum(),
                        Optional.ofNullable(cuerpo.getReferenciaAfectadaId()),
                        reloj.ahora().atOffset(ZoneOffset.UTC).plusDays(cuerpo.getDiasVotacion()),
                        entranteDelTraspaso(cuerpo, ctx)),
                ctx);

        var respuesta = new SalidaAcuerdo();
        respuesta.setAcuerdoId(acuerdoId);
        respuesta.setEstado("EN_VOTACION");
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    /**
     * Un traspaso se vota con nombre y apellido: el cupo y la persona que entra. Sin eso, una
     * votacion aprobada le serviria al titular para meter a cualquiera (AP-CU63-06).
     */
    private Optional<UUID> entranteDelTraspaso(EntradaAcuerdo cuerpo, ContextoSesion ctx) {
        if (cuerpo.getTipo() != EntradaAcuerdo.TipoEnum.TRASPASO_CUPO) {
            return Optional.empty();
        }
        // Primero, que quien propone sea del grupo: si no, la respuesta sobre quien entra le diria a un
        // extraño si esa persona participa de un grupo ajeno.
        consultas
                .participanteDe(cuerpo.getGrupoId(), ctx.usuarioId(), ctx)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(63, 1), "No participas de ese grupo."));
        if (cuerpo.getReferenciaAfectadaId() == null || cuerpo.getUsuarioEntranteId() == null) {
            throw sinEntrante();
        }
        return Optional.of(consultas
                .participanteDeOtro(cuerpo.getGrupoId(), cuerpo.getUsuarioEntranteId(), ctx)
                .orElseThrow(AcuerdosController::sinEntrante));
    }

    private static ErrorDeNegocio sinEntrante() {
        return new ErrorDeNegocio(
                CodigoError.de(63, 6), "El traspaso se vota sobre un cupo y una persona del grupo que entra.");
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<SalidaAcuerdo> votarAcuerdo(UUID acuerdoId, UUID idempotencyKey, EntradaVoto cuerpo) {
        var ctx = sesion.actual();
        Traza.marcarCasoDeUso("CU-63", acuerdoId.toString());

        // Vota un PARTICIPANTE del grupo del acuerdo, no un usuario: la FK del voto apunta a
        // `participante`. Quien no participa de ese grupo no vota.
        UUID grupoId = consultas
                .grupoDelAcuerdo(acuerdoId, ctx)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(63, 1), "Ese acuerdo no existe."));
        UUID participanteId = consultas
                .participanteDe(grupoId, ctx.usuarioId(), ctx)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(63, 1), "No participas de ese grupo."));

        cu63.votar(acuerdoId, participanteId, cuerpo.getVoto().getValue(), ctx);

        var respuesta = new SalidaAcuerdo();
        respuesta.setAcuerdoId(acuerdoId);
        respuesta.setEstado(cu63.resolver(acuerdoId, ctx));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Override
    @Permiso("PARTICIPANTE")
    public ResponseEntity<List<AcuerdoDelGrupo>> listarAcuerdos(
            UUID grupoId, String tipo, String estado, UUID referenciaAfectadaId) {
        var ctx = sesion.actual();
        Traza.marcarCasoDeUso("CU-63", grupoId.toString());
        // Las votaciones de un grupo son de sus participantes: nadie de afuera las lista.
        consultas
                .participanteDe(grupoId, ctx.usuarioId(), ctx)
                .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(63, 1), "No participas de ese grupo."));
        var filas = cu63.delGrupo(
                grupoId,
                Optional.ofNullable(tipo),
                Optional.ofNullable(estado),
                Optional.ofNullable(referenciaAfectadaId),
                ctx);
        return ResponseEntity.ok(filas.stream()
                .map(a -> {
                    var salida = new AcuerdoDelGrupo();
                    salida.setAcuerdoId(a.id());
                    salida.setTipo(TipoDeAcuerdo.valueOf(a.tipo()).aContrato());
                    salida.setEstado(AcuerdoDelGrupo.EstadoEnum.fromValue(a.estado()));
                    salida.setDescripcion(a.descripcion());
                    salida.setReferenciaAfectadaId(a.afectado().orElse(null));
                    salida.setUsuarioEntranteId(a.usuarioEntrante().orElse(null));
                    salida.setCierraEn(a.cierraEn());
                    return salida;
                })
                .toList());
    }
}
