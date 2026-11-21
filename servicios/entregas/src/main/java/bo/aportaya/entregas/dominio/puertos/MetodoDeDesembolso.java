package bo.aportaya.entregas.dominio.puertos;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * F3.L2.M01 · con que metodo se le paga al beneficiario una entrega que nace sola, sin que nadie lo elija.
 *
 * <p><b>DECISION_REQUIRED</b> (Pablo): ninguna fuente lo fija. La propuesta del diseño es derivarlo de la cuenta
 * VERIFICADA del beneficiario ({@code BILLETERA} → {@code BILLETERA_MOVIL}; {@code AHORRO}/{@code CORRIENTE} →
 * {@code TRANSFERENCIA_BANCARIA}). Eso pide resolver participante → usuario, que es de {@code grupos}: por eso el
 * contrato vive aca y las pruebas lo cumplen con un doble. Mientras tanto el adaptador de produccion es un metodo unico
 * por configuracion.
 *
 * <p>Contrato: devuelve uno de {@link #METODOS}, o vacio si todavia no se puede saber (sin cuenta verificada). Vacio NO
 * fondea: la entrega espera, porque {@code entrega_fondo.metodo_desembolso} es {@code NOT NULL} y la nulabilidad no se
 * cambia hasta la decision.
 */
public interface MetodoDeDesembolso {

    /** Los del CHECK de {@code entrega_fondo.metodo_desembolso}, salvo la compensacion interna. */
    Set<String> METODOS = Set.of("BILLETERA_MOVIL", "QR_ENVIO", "TRANSFERENCIA_BANCARIA");

    Optional<String> para(UUID beneficiarioParticipanteId);
}
