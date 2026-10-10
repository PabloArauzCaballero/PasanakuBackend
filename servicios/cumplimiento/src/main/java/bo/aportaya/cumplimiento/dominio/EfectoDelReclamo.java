package bo.aportaya.cumplimiento.dominio;

import java.time.OffsetDateTime;

/**
 * Que le impide un reclamo a OTRO proceso: ejecutar una deuda o dejar firme una sancion.
 *
 * <p>H13.S1.M6. Un reclamo en revision no equivale a una sancion firme ni a una deuda
 * ejecutable (regla 91.4): mientras el cliente discute algo con la plataforma, la plataforma
 * no cobra ni castiga por ese motivo. Esta clase responde SOLO eso —«este caso, hoy, frena o
 * no»— y lo hace desde el estado del caso, sin guardar nada: el efecto no se escribe en una
 * columna que alguien pueda dejar vieja, se deriva cada vez.
 *
 * <p>Cuando no se sabe, frena. El estado desconocido, el cierre sin resultado o una fecha
 * ausente se leen como «todavia no» (denegar por omision, invariante 9): dejar pasar una
 * ejecucion porque el reclamo estaba mal cargado es el error caro.
 *
 * <p><b>Decision de negocio pendiente</b> (a confirmar con Cumplimiento y Legal): aca el
 * alcance es el del caso —los reclamos de la misma persona y del mismo producto—; si el
 * bloqueo debe cubrir TODA la deuda de la persona sin importar el producto, es otra regla.
 */
public record EfectoDelReclamo(boolean bloqueaSancionFirme, boolean bloqueaDeudaEjecutable, Motivo motivo) {

    public enum Motivo {
        /** La persona no tiene reclamos: nada frena. */
        SIN_RECLAMOS,
        /** Ingresado, en analisis o respondido sin cierre formal: se esta revisando. */
        EN_REVISION,
        /** Hay una instancia superior presentada o en tramite. */
        INSTANCIA_SIN_RESOLVER,
        /** Una instancia superior resolvio con resarcimiento: la plataforma debe, no cobra. */
        RESARCIMIENTO_DISPUESTO,
        /** Cerrado a favor del cliente (total o parcial): hay reparacion de por medio. */
        FAVORABLE_AL_CLIENTE,
        /** Cerrado en contra, pero el cliente todavia puede elevarlo dentro del plazo de la norma. */
        PLAZO_DE_ELEVACION_ABIERTO,
        /** Terminado sin que frene nada: el cliente desistio, o se resolvio y el plazo para elevar paso. */
        TERMINADO_SIN_BLOQUEO,
        /** No se pudo interpretar el caso: se frena. */
        ESTADO_NO_RECONOCIDO
    }

    /** Lo que importa de un reclamo para saber si frena. */
    public record Caso(
            String estado,
            String resultado,
            OffsetDateTime fechaRespuesta,
            boolean instanciaSinResolver,
            boolean instanciaConResarcimiento) {}

    private static final EfectoDelReclamo LIBRE = new EfectoDelReclamo(false, false, Motivo.TERMINADO_SIN_BLOQUEO);

    public static EfectoDelReclamo sinReclamos() {
        return new EfectoDelReclamo(false, false, Motivo.SIN_RECLAMOS);
    }

    private static EfectoDelReclamo frena(Motivo motivo) {
        return new EfectoDelReclamo(true, true, motivo);
    }

    /**
     * @param ahora el instante contra el que se mide el plazo de elevacion
     * @param diasParaElevar los dias que la norma da al cliente tras la respuesta (config, no literal)
     */
    public static EfectoDelReclamo de(Caso caso, OffsetDateTime ahora, int diasParaElevar) {
        if (caso.estado() == null) {
            return frena(Motivo.ESTADO_NO_RECONOCIDO);
        }
        if (caso.instanciaSinResolver()) {
            return frena(Motivo.INSTANCIA_SIN_RESOLVER);
        }
        if (caso.instanciaConResarcimiento()) {
            return frena(Motivo.RESARCIMIENTO_DISPUESTO);
        }
        return switch (caso.estado()) {
            case "INGRESADO", "EN_ANALISIS", "RESPONDIDO" -> frena(Motivo.EN_REVISION);
            case "ELEVADO" -> LIBRE;
            case "CERRADO" -> deUnCasoCerrado(caso, ahora, diasParaElevar);
            default -> frena(Motivo.ESTADO_NO_RECONOCIDO);
        };
    }

    private static EfectoDelReclamo deUnCasoCerrado(Caso caso, OffsetDateTime ahora, int diasParaElevar) {
        if (caso.resultado() == null) {
            return frena(Motivo.ESTADO_NO_RECONOCIDO);
        }
        return switch (caso.resultado()) {
            case "FAVORABLE", "PARCIAL" -> frena(Motivo.FAVORABLE_AL_CLIENTE);
            case "DESISTIDO" -> LIBRE;
            case "DESFAVORABLE" -> {
                if (caso.fechaRespuesta() == null) {
                    yield frena(Motivo.ESTADO_NO_RECONOCIDO);
                }
                boolean sigueElevable = !ahora.isAfter(caso.fechaRespuesta().plusDays(diasParaElevar));
                yield sigueElevable ? frena(Motivo.PLAZO_DE_ELEVACION_ABIERTO) : LIBRE;
            }
            default -> frena(Motivo.ESTADO_NO_RECONOCIDO);
        };
    }
}
