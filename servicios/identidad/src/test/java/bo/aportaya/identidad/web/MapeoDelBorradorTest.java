package bo.aportaya.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.dominio.BorradorOnboardingDatos;
import bo.aportaya.identidad.web.generado.modelo.BorradorOnboarding;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test unitario de {@link MapeoDelBorrador}: conversion entre dominio y modelo generado. */
class MapeoDelBorradorTest {

    private static final BorradorOnboardingDatos DATOS_COMPLETOS = new BorradorOnboardingDatos(
            "pablo@example.com",
            "+59171234567",
            "Pablo",
            "Arauz",
            LocalDate.of(1995, 6, 15),
            "CI",
            "1234567",
            "SC",
            LocalDate.of(2030, 1, 1),
            "DEPENDIENTE",
            "Ingeniero",
            "SUELDO",
            "2000",
            "1",
            "documento");

    @Test
    @DisplayName("aModelo · null devuelve null")
    void aModeloNullEsNull() {
        assertThat(MapeoDelBorrador.aModelo(null)).isNull();
    }

    @Test
    @DisplayName("aModelo · mapea todos los campos del dominio al modelo")
    void aModeloMapeaTodosLosCampos() {
        BorradorOnboarding modelo = MapeoDelBorrador.aModelo(DATOS_COMPLETOS);

        assertThat(modelo).isNotNull();
        assertThat(modelo.getCorreo()).isEqualTo("pablo@example.com");
        assertThat(modelo.getTelefonoE164()).isEqualTo("+59171234567");
        assertThat(modelo.getNombres()).isEqualTo("Pablo");
        assertThat(modelo.getApellidos()).isEqualTo("Arauz");
        assertThat(modelo.getFechaNacimiento()).isEqualTo(LocalDate.of(1995, 6, 15));
        assertThat(modelo.getNumeroDocumento()).isEqualTo("1234567");
        assertThat(modelo.getLugarExpedicion()).isEqualTo("SC");
        assertThat(modelo.getActividadEconomica()).isEqualTo("DEPENDIENTE");
        assertThat(modelo.getDetalleActividad()).isEqualTo("Ingeniero");
        assertThat(modelo.getOrigenDeFondos()).isEqualTo("SUELDO");
        assertThat(modelo.getMontoMensualEstimado()).isEqualTo("1");
        assertThat(modelo.getPasoPendiente()).isEqualTo(BorradorOnboarding.PasoPendienteEnum.DOCUMENTO);
    }

    @Test
    @DisplayName("aModelo · tipo de documento valido se mapea al enum correcto")
    void aModeloTipoDocumentoValido() {
        var datos = new BorradorOnboardingDatos(
                "a@b.com", null, null, null, null, "CI", null, null, null, null, null, null, null, null, null);
        assertThat(MapeoDelBorrador.aModelo(datos).getTipoDocumento())
                .isEqualTo(BorradorOnboarding.TipoDocumentoEnum.CI);
    }

    @Test
    @DisplayName("aModelo · tipo de documento invalido no rompe (queda null)")
    void aModeloTipoDocumentoInvalidoNoRompe() {
        var datos = new BorradorOnboardingDatos(
                "a@b.com",
                null,
                null,
                null,
                null,
                "TIPO_INEXISTENTE",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        assertThat(MapeoDelBorrador.aModelo(datos).getTipoDocumento()).isNull();
    }

    @Test
    @DisplayName("aModelo · paso pendiente invalido no rompe (queda null)")
    void aModeloPasoPendienteInvalidoNoRompe() {
        var datos = new BorradorOnboardingDatos(
                "a@b.com",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "PASO_INEXISTENTE");
        assertThat(MapeoDelBorrador.aModelo(datos).getPasoPendiente()).isNull();
    }

    @Test
    @DisplayName("aModelo · campos nulos en dominio quedan nulos en modelo")
    void aModeloCamposNulosQuedanNulos() {
        var datos = new BorradorOnboardingDatos(
                "a@b.com", null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        BorradorOnboarding modelo = MapeoDelBorrador.aModelo(datos);
        assertThat(modelo.getTelefonoE164()).isNull();
        assertThat(modelo.getNombres()).isNull();
        assertThat(modelo.getTipoDocumento()).isNull();
        assertThat(modelo.getPasoPendiente()).isNull();
    }

    @Test
    @DisplayName("aDominio · mapea todos los campos del modelo al dominio")
    void aDominioCamposCompletos() {
        var modelo = new BorradorOnboarding();
        modelo.setCorreo("test@example.com");
        modelo.setTelefonoE164("+59170000000");
        modelo.setNombres("Test");
        modelo.setApellidos("User");
        modelo.setFechaNacimiento(LocalDate.of(2000, 1, 1));
        modelo.setTipoDocumento(BorradorOnboarding.TipoDocumentoEnum.CI);
        modelo.setNumeroDocumento("9999999");
        modelo.setLugarExpedicion("LP");
        modelo.setPasoPendiente(BorradorOnboarding.PasoPendienteEnum.ACTIVIDAD);

        BorradorOnboardingDatos datos = MapeoDelBorrador.aDominio(modelo);

        assertThat(datos.correo()).isEqualTo("test@example.com");
        assertThat(datos.nombres()).isEqualTo("Test");
        assertThat(datos.tipoDocumento()).isEqualTo("CI");
        assertThat(datos.pasoPendiente()).isEqualTo("actividad");
    }

    @Test
    @DisplayName("aDominio · tipo y paso nulos quedan nulos en el dominio")
    void aDominioNulosQuedanNulos() {
        var modelo = new BorradorOnboarding();
        modelo.setCorreo("x@x.com");

        BorradorOnboardingDatos datos = MapeoDelBorrador.aDominio(modelo);

        assertThat(datos.tipoDocumento()).isNull();
        assertThat(datos.pasoPendiente()).isNull();
    }
}
