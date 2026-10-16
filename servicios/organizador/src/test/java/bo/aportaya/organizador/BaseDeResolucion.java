package bo.aportaya.organizador;

import bo.aportaya.organizador.aplicacion.CU90PostularOrganizador.EntradaPostulacion;
import bo.aportaya.organizador.aplicacion.CU90ResolverHabilitacion.Entrada;
import bo.aportaya.organizador.aplicacion.CU90ResolverHabilitacion.Resultado;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/** Armado compartido de las pruebas de resolución de habilitaciones. */
abstract class BaseDeResolucion extends BaseDeOrganizador {

    protected Map<String, BigDecimal> medidos;
    protected UUID backoffice;

    @BeforeEach
    protected void requisitos() {
        fixtura.requisito("REPUTACION-" + corto(), "REPUTACION", "70", true, "APRENDIZ");
        fixtura.requisito("ANTIGUEDAD-" + corto(), "ANTIGUEDAD", "6", true, "APRENDIZ");
        medidos = medidosDe("82", "12");
        backoffice = fixtura.usuario();
    }

    @AfterEach
    protected void limpiar() {
        fixtura.limpiar();
    }

    protected String corto() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected Map<String, BigDecimal> medidosDe(String reputacion, String antiguedad) {
        var salida = new java.util.LinkedHashMap<String, BigDecimal>();
        for (var fila : dsl.fetch("SELECT codigo, tipo FROM organizador.requisito_habilitacion")) {
            String tipo = fila.get("tipo", String.class);
            salida.put(
                    fila.get("codigo", String.class),
                    new BigDecimal("REPUTACION".equals(tipo) ? reputacion : antiguedad));
        }
        return salida;
    }

    protected ContextoSesion comoBackoffice(UUID usuario) {
        return ContextoSesion.de(
                usuario, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    protected UUID postular(UUID usuario) {
        return transaccion
                .execute(t -> postulacionCU.postular(
                        new EntradaPostulacion(
                                "Quiero organizar el pasanaku de mi barrio",
                                "Tres pasanakus como participante",
                                fixtura.kycAprobado(usuario),
                                new BigDecimal("82.00"),
                                medidos),
                        contextoDe(usuario)))
                .solicitudId();
    }

    protected Entrada entrada(UUID solicitud, UUID clave, String decision, String motivo, int revision) {
        return new Entrada(solicitud, clave, decision, motivo, revision, medidos);
    }

    protected Resultado resolver(Entrada entrada, UUID revisor) {
        return transaccion.execute(t -> resolucionCU.resolver(entrada, comoBackoffice(revisor)));
    }

    protected int decisiones(UUID solicitud) {
        return contar("SELECT count(*) FROM organizador.decision_habilitacion WHERE solicitud_id=?", solicitud);
    }

    protected int eventos(String tipo, UUID agregado) {
        return contar("SELECT count(*) FROM organizador.evento_dominio WHERE tipo=? AND agregado_id=?", tipo, agregado);
    }

    protected String estadoSolicitud(UUID solicitud) {
        return dsl.fetchOne("SELECT estado FROM organizador.solicitud_organizador WHERE id=?", solicitud)
                .get(0, String.class);
    }
}
