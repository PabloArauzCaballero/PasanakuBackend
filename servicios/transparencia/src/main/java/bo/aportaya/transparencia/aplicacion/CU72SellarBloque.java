package bo.aportaya.transparencia.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import bo.aportaya.transparencia.dominio.CadenaDeBloques;
import bo.aportaya.transparencia.dominio.ContenidoCanonico;
import bo.aportaya.transparencia.infraestructura.CadenaRepositorio;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-72 · Sellar el bloque de transparencia.
 *
 * <p>Que el grupo pueda demostrar **que su historia no fue reescrita**. Alterar un
 * aporte viejo obliga a rehacer todos los bloques posteriores, y eso se nota.
 *
 * <p>Dos negativas que no se negocian:
 *
 * <ul>
 *   <li>**Con excepciones de conciliacion abiertas no se sella** (R-BIL-12). Un bloque
 *       con datos provisorios miente con firma, y firmar una mentira es peor que no
 *       firmar nada.
 *   <li>**Un hecho ya sellado no se reescribe.** La correccion entra al bloque
 *       siguiente como movimiento compensatorio y ambos quedan visibles. Reescribir el
 *       bloque seria exactamente lo que la cadena existe para impedir.
 * </ul>
 */
@Service
public class CU72SellarBloque {

    private final Datos datos;
    private final CadenaRepositorio cadenas;
    private final Outbox outbox;
    private final Reloj reloj;

    public CU72SellarBloque(Datos datos, CadenaRepositorio cadenas, Outbox outbox, Reloj reloj) {
        this.datos = datos;
        this.cadenas = cadenas;
        this.outbox = outbox;
        this.reloj = reloj;
    }

    @Transactional
    public SalidaBloque sellar(EntradaBloque entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        // AP-CU72-01 · R-BIL-12. El estado de la conciliacion vive en el nucleo
        // financiero y llega resuelto: no se lee su esquema (invariante 11) ni se lo
        // consulta por red dentro de la transaccion (invariante 6).
        if (entrada.excepcionesDeConciliacionAbiertas() > 0) {
            throw new ErrorDeNegocio(
                    CodigoError.de(72, 1),
                    "El periodo tiene " + entrada.excepcionesDeConciliacionAbiertas()
                            + " excepciones de conciliacion abiertas: un bloque con datos provisorios miente con firma.");
        }

        // Los bordes del periodo se normalizan ANTES de firmarlos y de guardarlos, y con
        // eso queda dicho todo: lo que se firma es exactamente lo que la base va a tener.
        //
        // Sin esto el bloque nacia invalido. `TIMESTAMPTZ` guarda microsegundos y
        // PostgreSQL REDONDEA para llegar ahi, mientras que Java trunca: un instante
        // .123456789 se firmaba como .123456 y se guardaba como .123457. Verificar la
        // cadena —recomputar el hash desde lo guardado, que es lo que promete CU-73—
        // devolvia HASH_BLOQUE en todos los bloques. Medido en PostgreSQL 16:
        //
        //   enviado .123456789  ->  guardado .123457   (redondea)
        //   Java truncatedTo(MICROS) ->      .123456   (trunca)
        //
        // Y pasaba solo en Linux, donde el reloj da nanosegundos; en macOS ya da
        // microsegundos y no habia nada que redondear.
        OffsetDateTime desde = entrada.desde().truncatedTo(ChronoUnit.MICROS);
        OffsetDateTime hasta = entrada.hasta().truncatedTo(ChronoUnit.MICROS);

        return datos.conContexto(ctx, dsl -> {
            var ultimo = cadenas.ultimoBloque(dsl, entrada.grupoId());
            long numero = ultimo.map(u -> u.numero() + 1).orElse(CadenaDeBloques.PRIMER_NUMERO);
            String hashAnterior = ultimo.map(CadenaRepositorio.Ultimo::hash).orElse(CadenaDeBloques.GENESIS);

            // Las hojas se calculan sobre la forma canonica de cada hecho, no sobre lo
            // que el JSON del momento produzca: un hash solo sirve si dos
            // implementaciones producen el mismo.
            var hojas = new ArrayList<String>(entrada.hechos().size());
            var resumenes = new ArrayList<String>(entrada.hechos().size());
            for (var hecho : entrada.hechos()) {
                String canonico = ContenidoCanonico.serializar(hecho.campos());
                hojas.add(CadenaDeBloques.sha256(canonico));
                resumenes.add(canonico);
            }
            String raizMerkle = CadenaDeBloques.raizMerkle(hojas);
            String hashBloque = CadenaDeBloques.hashDelBloque(
                    numero,
                    hashAnterior,
                    raizMerkle,
                    ContenidoCanonico.instante(desde),
                    ContenidoCanonico.instante(hasta));

            UUID bloqueId;
            try {
                bloqueId = cadenas.sellarBloque(
                        dsl,
                        entrada.grupoId(),
                        numero,
                        hashAnterior,
                        raizMerkle,
                        hashBloque,
                        entrada.hechos().size(),
                        desde,
                        hasta,
                        ahora);
            } catch (org.jooq.exception.IntegrityConstraintViolationException
                    | org.springframework.dao.DataIntegrityViolationException e) {
                // AP-CU72-03 · uq_bloque_grupo_numero. Dos sellados a la vez leen el
                // mismo predecesor y quieren el mismo numero; gana uno. El otro no
                // reintenta a ciegas: el llamador vuelve a leer la punta de la cadena.
                // El detalle lleva la restriccion que la base nombro, no solo la que este
                // catch supone. Sin eso, `ck_bloque_genesis` o cualquier otra se reportaban
                // como «ya existe el bloque N» y mandaban a buscar un duplicado que no
                // existia.
                throw new ErrorDeNegocio(
                        CodigoError.de(72, 3),
                        "Ya existe el bloque " + numero + " para ese grupo (R-REP-04).",
                        java.util.Map.of(
                                "numeroBloque",
                                numero,
                                "grupoId",
                                entrada.grupoId().toString(),
                                "restriccion",
                                raizDe(e)));
            } catch (org.jooq.exception.DataAccessException | org.springframework.dao.DataAccessException e) {
                // AP-CU72-02 · tg_bloque_encadenado. Si la base dice que el eslabon no
                // encaja, la cadena esta rota y **el sellado se detiene**. Insistir
                // escribiria historia sobre una cadena que ya no prueba nada.
                throw new ErrorDeNegocio(
                        CodigoError.de(72, 2),
                        "La cadena del grupo esta rota: " + raizDe(e),
                        java.util.Map.of("numeroBloque", numero, "hashAnterior", hashAnterior));
            }

            for (int i = 0; i < entrada.hechos().size(); i++) {
                var hecho = entrada.hechos().get(i);
                cadenas.sellarRegistro(
                        dsl,
                        bloqueId,
                        hecho.tipoEntidad(),
                        hecho.entidadId(),
                        hojas.get(i),
                        resumenes.get(i),
                        hecho.ocurridoEn().truncatedTo(ChronoUnit.MICROS));
            }

            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "transparencia.bloque_sellado",
                            "bloque_transparencia",
                            bloqueId,
                            Map.of(
                                    "grupoId", entrada.grupoId().toString(),
                                    "numeroBloque", Long.toString(numero),
                                    "hashBloque", hashBloque,
                                    "hashAnterior", hashAnterior,
                                    "entidadesSelladas",
                                            Integer.toString(entrada.hechos().size()),
                                    "motivo", entrada.motivo()),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaBloque(
                    bloqueId,
                    numero,
                    hashAnterior,
                    raizMerkle,
                    hashBloque,
                    entrada.hechos().size());
        });
    }

    /** El mensaje del fondo del pozo: es donde la base dice que regla se rompio. */
    private static String raizDe(Throwable e) {
        Throwable raiz = e;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        return String.valueOf(raiz.getMessage());
    }

    /**
     * Un hecho a sellar. Vive en el esquema de otro servicio; llega resuelto por su
     * contrato, ya en la forma que va a entrar al hash.
     */
    public record Hecho(String tipoEntidad, UUID entidadId, Map<String, String> campos, OffsetDateTime ocurridoEn) {}

    public record EntradaBloque(
            UUID grupoId,
            String motivo,
            List<Hecho> hechos,
            OffsetDateTime desde,
            OffsetDateTime hasta,
            int excepcionesDeConciliacionAbiertas) {}

    public record SalidaBloque(
            UUID bloqueId,
            long numeroBloque,
            String hashAnterior,
            String raizMerkle,
            String hashBloque,
            int entidadesSelladas) {}
}
