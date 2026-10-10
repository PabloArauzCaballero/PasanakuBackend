package bo.aportaya.cumplimiento.aplicacion;

import bo.aportaya.cumplimiento.dominio.EfectoDelReclamo;
import bo.aportaya.cumplimiento.infraestructura.ReclamoRepositorio;
import bo.aportaya.cumplimiento.infraestructura.ReclamoRepositorio.CasoDelReclamo;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * H13.S1.M6 · ¿Frena algun reclamo la ejecucion de una deuda o la firmeza de una sancion?
 *
 * <p>Es la consulta que garantia (cobranza, restricciones) y organizador (sanciones) tienen que
 * hacer ANTES de volver ejecutable una deuda o firme una sancion. Devuelve el rastro entero —
 * cada caso con su estado, resultado, fecha y el motivo por el que frena o no— y no solo un
 * booleano: la trazabilidad de reclamo, resolucion y efecto permitido es el entregable.
 *
 * <p>No escribe nada y no devuelve el texto del reclamo ni de la respuesta. Lo que decide si el
 * efecto procede sigue siendo el debido proceso de quien lo aplica (causal, notificacion
 * probada, descargo, apelacion): que ningun reclamo frene es condicion NECESARIA, no suficiente.
 */
@Service
public class ConsultarEfectoDelReclamo {

    private final Datos datos;
    private final ReclamoRepositorio reclamos;
    private final Reloj reloj;
    private final int diasParaElevar;

    public ConsultarEfectoDelReclamo(
            Datos datos,
            ReclamoRepositorio reclamos,
            Reloj reloj,
            @Value("${aportaya.reclamos.dias-para-elevar}") int diasParaElevar) {
        this.datos = datos;
        this.reclamos = reclamos;
        this.reloj = reloj;
        this.diasParaElevar = diasParaElevar;
    }

    /**
     * @param producto si viene, solo cuentan los reclamos de ese producto; si no, todos los de la persona
     */
    @Transactional(readOnly = true)
    public Resultado delUsuario(UUID usuarioId, Optional<String> producto, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);
        List<CasoDelReclamo> casos = datos.conContexto(ctx, dsl -> reclamos.casosDelUsuario(dsl, usuarioId)).stream()
                .filter(c -> producto.map(c.producto()::equals).orElse(true))
                .toList();

        List<Caso> evaluados = casos.stream()
                .map(c -> new Caso(
                        c.id(),
                        c.codigo(),
                        c.categoria(),
                        c.producto(),
                        c.estado(),
                        c.resultado(),
                        c.fechaIngreso(),
                        c.fechaRespuesta(),
                        EfectoDelReclamo.de(
                                new EfectoDelReclamo.Caso(
                                        c.estado(),
                                        c.resultado(),
                                        c.fechaRespuesta(),
                                        c.instanciaSinResolver(),
                                        c.instanciaConResarcimiento()),
                                ahora,
                                diasParaElevar)))
                .toList();

        boolean sancion = evaluados.stream().anyMatch(c -> c.efecto().bloqueaSancionFirme());
        boolean deuda = evaluados.stream().anyMatch(c -> c.efecto().bloqueaDeudaEjecutable());
        return new Resultado(
                usuarioId,
                sancion,
                deuda,
                evaluados.isEmpty() ? EfectoDelReclamo.sinReclamos().motivo() : null,
                evaluados);
    }

    public record Caso(
            UUID reclamoId,
            String codigo,
            String categoria,
            String producto,
            String estado,
            String resultado,
            OffsetDateTime fechaIngreso,
            OffsetDateTime fechaRespuesta,
            EfectoDelReclamo efecto) {}

    /**
     * @param motivoGlobal solo cuando no hay reclamos que mirar ({@code SIN_RECLAMOS}); si hay, el motivo
     *     de cada uno esta en su caso
     */
    public record Resultado(
            UUID usuarioId,
            boolean bloqueaSancionFirme,
            boolean bloqueaDeudaEjecutable,
            EfectoDelReclamo.Motivo motivoGlobal,
            List<Caso> casos) {}
}
