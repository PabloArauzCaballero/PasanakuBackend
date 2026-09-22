package bo.aportaya.aportes.dominio;

import java.util.Optional;

/**
 * El secreto compartido con el que la pasarela firma sus webhooks (Q-02).
 *
 * <p>{@code proveedor_pago.referencia_credenciales} guarda solo una REFERENCIA al
 * almacen de secretos ({@code vault://...}), nunca el secreto — es una de las
 * dieciocho prohibiciones de {@code docs/Seguridad.md}. Este puerto es lo que
 * resolveria esa referencia contra un HSM/vault real; ese adaptador no existe en el
 * repo (no hay cliente de vault en ningun servicio) y construirlo es
 * {@code DECISION_REQUIRED} — no se improvisa en un carril de seguridad transversal.
 *
 * <p>Mientras tanto: {@code local}/{@code test} usan un secreto FIJO de desarrollo
 * (doble, Q-02); cualquier otro perfil no tiene bean de un secreto real y cae en
 * {@link bo.aportaya.aportes.infraestructura.SecretoWebhookSinConfigurar}, que
 * siempre devuelve {@link Optional#empty()} — fail closed: sin adaptador real, cada
 * webhook se trata como firma invalida, nunca como firma valida.
 */
public interface ProveedorDeSecretoWebhook {

    Optional<String> secretoDe(String proveedorCodigo);
}
