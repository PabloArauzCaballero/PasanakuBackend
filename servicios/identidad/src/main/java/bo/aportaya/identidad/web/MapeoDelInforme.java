package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU08InformeDeAccesos;
import bo.aportaya.identidad.dominio.InformeDeAccesos;
import bo.aportaya.identidad.web.generado.modelo.FilaDeAcceso;
import bo.aportaya.identidad.web.generado.modelo.PaginaDeAccesos;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;

/**
 * Del informe de accesos de CU-08 al contrato {@code PaginaDeAccesos}. Sin reglas: traduce y rechaza con 400
 * (AP-VAL-02) lo que el contrato declara fuera de dominio, en vez de dejar que llegue como 500 o como un
 * {@code OFFSET} negativo a la base.
 */
final class MapeoDelInforme {

    /** El valor por omision que declara el contrato ({@code limite: default 50}). */
    static final int FILAS_POR_OMISION = 50;

    private static final int PAGINA_MAS_ALTA = 1000;
    private static final int FILAS_COMO_MAXIMO = 100;

    private MapeoDelInforme() {}

    static InformeDeAccesos.Seccion seccion(String valor) {
        for (InformeDeAccesos.Seccion s : InformeDeAccesos.Seccion.values()) {
            if (s.name().equals(valor)) {
                return s;
            }
        }
        throw fueraDeDominio("seccion");
    }

    static int pagina(Integer pedida) {
        int pagina = pedida == null ? 1 : pedida;
        if (pagina < 1 || pagina > PAGINA_MAS_ALTA) {
            throw fueraDeDominio("pagina");
        }
        return pagina;
    }

    static int limite(Integer pedido) {
        int limite = pedido == null ? FILAS_POR_OMISION : pedido;
        if (limite < 1 || limite > FILAS_COMO_MAXIMO) {
            throw fueraDeDominio("limite");
        }
        return limite;
    }

    static PaginaDeAccesos aPagina(CU08InformeDeAccesos.Pagina p) {
        PaginaDeAccesos pagina = new PaginaDeAccesos();
        pagina.setSeccion(PaginaDeAccesos.SeccionEnum.fromValue(p.seccion().name()));
        pagina.setGeneradoEn(p.generadoEn());
        pagina.setItems(p.filas().stream().map(MapeoDelInforme::aFila).toList());
        pagina.setPagina(p.pagina());
        pagina.setLimite(p.limite());
        pagina.setTotal(p.total());
        pagina.setTotalPaginas(p.totalPaginas());
        return pagina;
    }

    private static ConstraintViolationException fueraDeDominio(String parametro) {
        return new ConstraintViolationException(parametro + " fuera del dominio del contrato", Set.of());
    }

    private static FilaDeAcceso aFila(InformeDeAccesos.Fila f) {
        FilaDeAcceso fila = new FilaDeAcceso();
        fila.setAsignacionId(f.asignacionId());
        fila.setUsuarioId(f.usuarioId());
        fila.setNombre(f.nombre());
        fila.setEstadoCuenta(f.estadoCuenta());
        fila.setRolCodigo(f.rolCodigo());
        fila.setAmbito(FilaDeAcceso.AmbitoEnum.fromValue(f.ambito()));
        fila.setAmbitoId(f.ambitoId().orElse(null));
        fila.setOtorgadaPor(f.otorgadaPor());
        fila.setOtorgadaEn(f.otorgadaEn());
        fila.setVigenteHasta(f.vigenteHasta().orElse(null));
        fila.setTotpConfirmado(f.totpConfirmado());
        return fila;
    }
}
