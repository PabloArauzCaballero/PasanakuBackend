package bo.aportaya.grupos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.aportaya.grupos.aplicacion.CU59CalcularPlazo;
import bo.aportaya.grupos.aplicacion.CU64TraspasarCupo;
import bo.aportaya.grupos.aplicacion.CU65Retirarse;
import bo.aportaya.grupos.dominio.TraspasoAdmisible;
import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.pruebas.web.PruebaWeb;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * CU-64 · F1.L3.M05: el KYC del entrante lo dice {@code identidad}, no el minimo del grupo.
 *
 * <p>Antes, el controlador pasaba {@code politica.kycMinimo()} como KYC del entrante, y el control de debida
 * diligencia aprobaba siempre. El doble de {@code identidad} es el puerto {@link HechosDeOtrosServicios}, en sus tres
 * niveles: correcto (nivel de sobra), limite (justo el minimo del grupo) e invalido (identidad no responde: el puerto
 * devuelve {@code NINGUNO}, su degradacion declarada). Como {@code CU64} es un doble aca, lo que se fija es el valor
 * que recibe, y la regla de dominio {@link TraspasoAdmisible} decide con ese mismo valor.
 */
@PruebaWeb(
        value = GruposController.class,
        properties = {
            "aportaya.tarifas.codigo-tarifario=TAR-2026",
            "aportaya.grupo.afinidad-neutra=0.50",
            "aportaya.grupo.servicio-de-licencia=http://cumplimiento:8080",
            "aportaya.grupo.tope-de-reenvios-de-invitacion=3",
            "aportaya.grupos.admision.humana-obligatoria=false",
        })
@MockitoBean(
        types = {
            bo.aportaya.grupos.aplicacion.CU64TraspasoPorSubasta.class,
            CU59CalcularPlazo.class,
            CU65Retirarse.class,
            InvitacionesWeb.class,
            AdmisionDelGrupo.class,
            RespuestasAOtrosServicios.class,
            SorteoDelGrupo.class,
            IntegrantesDelGrupo.class,
            GruposDeBackoffice.class
        })
class TraspasoKycDelEntranteWebTest extends BaseDeEtapaMinimaWeb {

    private static final String MINIMO_DEL_GRUPO = "BASICO"; // la politica de BaseDeEtapaMinimaWeb

    @BeforeEach
    void entranteDelGrupo() {
        when(afuera.etapaDe(ENTRANTE)).thenReturn(EtapasDePrueba.con("500.00"));
        when(consultas.participanteDe(eq(GRUPO), any(), any())).thenReturn(Optional.of(UUID.randomUUID()));
        when(consultas.participanteDeOtro(eq(GRUPO), eq(ENTRANTE), any())).thenReturn(Optional.of(UUID.randomUUID()));
        when(afuera.estadoDePagos(any()))
                .thenReturn(new HechosDeOtrosServicios.EstadoDePagos(
                        true, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, "BOB"));
        when(cu64.ejecutar(any(), any())).thenReturn(UUID.randomUUID());
    }

    private String kycQueRecibeElCasoDeUso() throws Exception {
        traspasar().andExpect(status().isCreated());
        var entrada = ArgumentCaptor.forClass(CU64TraspasarCupo.EntradaTraspaso.class);
        verify(cu64).ejecutar(entrada.capture(), any());
        assertThat(entrada.getValue().kycMinimoDelGrupo()).isEqualTo(MINIMO_DEL_GRUPO);
        return entrada.getValue().kycDelEntrante();
    }

    private static boolean admitido(String kycDelEntrante) {
        return TraspasoAdmisible.impedimento("OCUPADO", false, true, kycDelEntrante, MINIMO_DEL_GRUPO, 500, 500, true)
                .isEmpty();
    }

    @Test
    @DisplayName("Correcto · el nivel del entrante sale de identidad (INTERMEDIO), no del minimo del grupo")
    void elNivelEsElDeIdentidad() throws Exception {
        when(afuera.nivelDeKyc(ENTRANTE)).thenReturn("INTERMEDIO");

        assertThat(kycQueRecibeElCasoDeUso()).isEqualTo("INTERMEDIO");
        verify(afuera).nivelDeKyc(ENTRANTE);
    }

    @Test
    @DisplayName("Límite · un entrante con justo el nivel minimo del grupo pasa el control")
    void justoElMinimoAlcanza() throws Exception {
        when(afuera.nivelDeKyc(ENTRANTE)).thenReturn(MINIMO_DEL_GRUPO);

        assertThat(admitido(kycQueRecibeElCasoDeUso())).isTrue();
    }

    @Test
    @DisplayName("Inválido · KYC no aprobado: el caso de uso recibe NINGUNO y la regla lo rechaza (ENTRANTE_SIN_KYC)")
    void sinKycSeRechaza() throws Exception {
        when(afuera.nivelDeKyc(ENTRANTE)).thenReturn("NINGUNO");

        String recibido = kycQueRecibeElCasoDeUso();
        assertThat(recibido).isEqualTo("NINGUNO");
        assertThat(TraspasoAdmisible.impedimento("OCUPADO", false, true, recibido, MINIMO_DEL_GRUPO, 500, 500, true))
                .contains(TraspasoAdmisible.Motivo.ENTRANTE_SIN_KYC);
    }

    @Test
    @DisplayName("Inválido · identidad caida: el puerto degrada a NINGUNO y el traspaso falla cerrado")
    void identidadCaidaFallaCerrado() throws Exception {
        // HechosCaidaTest fija que el adaptador devuelve NINGUNO con identidad caida o lenta.
        when(afuera.nivelDeKyc(ENTRANTE)).thenReturn("NINGUNO");

        assertThat(admitido(kycQueRecibeElCasoDeUso())).isFalse();
    }

    @Test
    @DisplayName("Reintento del mismo traspaso: no se consulta el KYC (vuelve el original)")
    void elReintentoNoConsultaElKyc() throws Exception {
        when(cu64.yaTraspasado(eq(CUPO), any(), any(), any())).thenReturn(true);

        traspasar().andExpect(status().isCreated());
        verify(afuera, never()).nivelDeKyc(any());
    }
}
