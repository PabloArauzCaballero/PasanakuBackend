package bo.aportaya.identidad;

import static bo.aportaya.identidad.ExpedientesDePrueba.conVencimiento;
import static bo.aportaya.identidad.ExpedientesDePrueba.contexto;
import static bo.aportaya.identidad.ExpedientesDePrueba.expediente;
import static bo.aportaya.identidad.ExpedientesDePrueba.expedienteCompleto;
import static bo.aportaya.identidad.ExpedientesDePrueba.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import bo.aportaya.identidad.aplicacion.CU02RevisarExpediente;
import bo.aportaya.identidad.infraestructura.RevisionRepositorio;
import bo.aportaya.plataforma.archivos.AlmacenDeArchivos;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lo que el SERVIDOR exige para aprobar un expediente (CU-01 flujo 4b): las cinco fotos y
 * un documento vigente a la fecha de la decision, en hora de La Paz. El backoffice apaga
 * el boton con la misma regla, pero esa es ayuda: la barrera es esta (AP-CU01-09/10/11).
 */
class CU02DecisionDelServidorTest {
    private static DSLContext dsl;
    private static TransactionTemplate transaccion;
    private static RevisionRepositorio repositorio;

    @BeforeAll
    static void armar() {
        var contenedor = BaseDePrueba.contenedor();
        DataSource fuente = new DriverManagerDataSource(
                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
        dsl = DSL.using(new TransactionAwareDataSourceProxy(fuente), SQLDialect.POSTGRES);
        transaccion = new TransactionTemplate(new DataSourceTransactionManager(fuente));
        repositorio = new RevisionRepositorio();
        ExpedientesDePrueba.usar(dsl);
    }

    @Test
    void aprobarExigeEnElServidorLasCincoFotosYUnDocumentoVigente() {
        UUID revisor = usuario();
        ContextoSesion contexto = contexto(revisor);
        // Hoy, para el caso de uso, es 2026-09-24 en La Paz.
        var caso = new CU02RevisarExpediente(
                repositorio,
                mock(AlmacenDeArchivos.class),
                new Datos(dsl),
                Reloj.fijo(Instant.parse("2026-09-24T16:00:00Z")));

        UUID sinFotos = expediente(usuario(), "EN_REVISION", true);
        conVencimiento(sinFotos, "2030-01-01");
        assertThat(aprobar(caso, sinFotos, contexto)).isEqualTo("AP-CU01-09");

        UUID sinFecha = expedienteCompleto(usuario(), null);
        assertThat(aprobar(caso, sinFecha, contexto)).isEqualTo("AP-CU01-10");

        UUID vencido = expedienteCompleto(usuario(), "2026-09-23");
        assertThat(aprobar(caso, vencido, contexto)).isEqualTo("AP-CU01-11");

        for (UUID rechazado : new UUID[] {sinFotos, sinFecha, vencido}) {
            assertThat(dsl.fetchValue("SELECT estado FROM identidad.verificacion_kyc WHERE id = ?", rechazado))
                    .as("una aprobacion rechazada no toca el expediente")
                    .isEqualTo("EN_REVISION");
        }

        // El ultimo dia de vigencia todavia vale.
        UUID venceHoy = expedienteCompleto(usuario(), "2026-09-24");
        assertThat(aprobar(caso, venceHoy, contexto)).isEqualTo("APROBADA");
        assertThat(dsl.fetchValue("SELECT revisada_por FROM identidad.verificacion_kyc WHERE id = ?", venceHoy))
                .isEqualTo(revisor);

        // Rechazar no exige fotos ni documento vigente: es justamente como se pide uno nuevo.
        UUID paraRechazar = expedienteCompleto(usuario(), "2026-09-23");
        transaccion.execute(estado -> {
            caso.resolver(paraRechazar, "RECHAZAR", "El carnet esta vencido: subi uno vigente.", contexto);
            return null;
        });
        assertThat(dsl.fetchValue("SELECT estado FROM identidad.verificacion_kyc WHERE id = ?", paraRechazar))
                .isEqualTo("RECHAZADA");
    }

    @Test
    void laColaInformaElVencimientoYSiElDocumentoEstaVigente() {
        UUID vigente = expedienteCompleto(usuario(), "2030-01-01");
        UUID vencido = expedienteCompleto(usuario(), "2026-09-23");
        var caso = new CU02RevisarExpediente(
                repositorio,
                mock(AlmacenDeArchivos.class),
                new Datos(dsl),
                Reloj.fijo(Instant.parse("2026-09-24T16:00:00Z")));
        var cola = repositorio.enEstado(dsl, "EN_REVISION");
        var deVigente = cola.stream()
                .filter(e -> e.verificacionId().equals(vigente))
                .findFirst()
                .orElseThrow();
        var deVencido = cola.stream()
                .filter(e -> e.verificacionId().equals(vencido))
                .findFirst()
                .orElseThrow();

        assertThat(deVigente.fechaExpiracionDocumento()).isEqualTo(java.time.LocalDate.parse("2030-01-01"));
        assertThat(deVigente.completo()).isTrue();
        assertThat(caso.documentoVigente(deVigente)).isTrue();
        assertThat(caso.documentoVigente(deVencido)).isFalse();
    }

    /** Devuelve el codigo del rechazo, o el estado final si la aprobacion paso. */
    private static String aprobar(CU02RevisarExpediente caso, UUID verificacion, ContextoSesion contexto) {
        try {
            transaccion.execute(estado -> {
                caso.resolver(verificacion, "APROBAR", null, contexto);
                return null;
            });
            return dsl.fetchValue("SELECT estado FROM identidad.verificacion_kyc WHERE id = ?", verificacion)
                    .toString();
        } catch (ErrorDeNegocio e) {
            return e.codigo().valor();
        }
    }
}
