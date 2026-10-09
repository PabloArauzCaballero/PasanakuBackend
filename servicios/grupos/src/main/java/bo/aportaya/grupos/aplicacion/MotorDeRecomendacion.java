package bo.aportaya.grupos.aplicacion;

import bo.aportaya.grupos.aplicacion.CU68Postular.EntradaPostulacion;
import bo.aportaya.grupos.dominio.CriterioDeEmparejamiento;
import bo.aportaya.grupos.dominio.MotivoDelPuntaje;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * La lectura explicable del motor de admision: motivos legibles y una recomendacion, nunca una decision.
 *
 * <p>Sin dato NO es un valor malo: no se compara contra el minimo ni se rechaza. Se declara SIN_DATOS y una
 * persona del backoffice decide.
 */
final class MotorDeRecomendacion {

    private MotorDeRecomendacion() {}

    static List<String> motivos(CriterioDeEmparejamiento criterio, EntradaPostulacion entrada) {
        var motivos = new ArrayList<>(MotivoDelPuntaje.de(
                criterio,
                entrada.afinidadReputacion(),
                entrada.afinidadMonto(),
                entrada.afinidadGeografia(),
                entrada.afinidadHistorial()));
        if (entrada.reputacionSinDatos()) {
            motivos.add("SIN_DATOS_REPUTACION: sin historial verificable; requiere valoración humana del backoffice.");
        } else if (!criterio.alcanzaLaReputacion(entrada.reputacion())) {
            motivos.add("REVISAR_REPUTACION: requiere valoración humana del backoffice.");
        }
        if (entrada.concentracionSinDatos()) {
            motivos.add(
                    "SIN_DATOS_CONCENTRACION: no se pudo medir la mora del grupo; requiere valoración humana del backoffice.");
        } else if (!criterio.admiteOtroMoroso(entrada.morososDelGrupo())) {
            motivos.add("REVISAR_CONCENTRACION: requiere valoración humana del backoffice.");
        }
        return motivos;
    }

    static String recomendacion(List<String> motivos) {
        boolean hayAlertas = motivos.stream().anyMatch(m -> m.startsWith("REVISAR_") || m.startsWith("SIN_DATOS_"));
        return hayAlertas ? "REVISION_HUMANA" : "ACEPTAR";
    }

    static Map<String, String> entradas(EntradaPostulacion e) {
        return Map.of(
                "reputacion", Integer.toString(e.reputacion()),
                "morososDelGrupo", e.concentracionSinDatos() ? "SIN_DATO" : Integer.toString(e.morososDelGrupo()),
                "afinidadReputacion", e.afinidadReputacion().toPlainString(),
                "afinidadMonto", e.afinidadMonto().toPlainString(),
                "afinidadGeografia", e.afinidadGeografia().toPlainString(),
                "afinidadHistorial", e.afinidadHistorial().toPlainString());
    }
}
