package bo.aportaya.erp.web;

/**
 * Los permisos que abren la seccion Contabilidad del backoffice ({@code secciones.ts}, "ver:contabilidad").
 *
 * <p>Las lecturas que toda pantalla de la seccion necesita (elegir ejercicio y periodo) aceptan
 * cualquiera de ellos: pedir solo el de reportes dejaria a quien paga o cobra sin poder elegir mes.
 * Los codigos son los sembrados en {@code sql/60_semillas/10-roles-y-permisos.sql}; no se inventa uno.
 */
final class PermisosDeContabilidad {

    static final String CERRAR = "CONTABILIDAD_ERP_CERRAR";
    static final String PRESUPUESTO = "CONTABILIDAD_ERP_PRESUPUESTO";
    static final String COMPRAS = "CONTABILIDAD_ERP_COMPRAS";
    static final String CUENTAS_POR_PAGAR = "CONTABILIDAD_ERP_CUENTAS_POR_PAGAR";
    static final String PAGAR = "CONTABILIDAD_ERP_PAGAR";
    static final String COBRAR = "CONTABILIDAD_ERP_COBRAR";
    static final String ACTIVOS_FIJOS = "CONTABILIDAD_ERP_ACTIVOS_FIJOS";
    static final String REPORTES = "CONTABILIDAD_ERP_REPORTES";

    private PermisosDeContabilidad() {}
}
