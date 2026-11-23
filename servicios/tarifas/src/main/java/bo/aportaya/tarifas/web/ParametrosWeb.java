package bo.aportaya.tarifas.web;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.web.seguridad.ActorAutenticado;
import bo.aportaya.plataforma.web.seguridad.CuatroOjos;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import bo.aportaya.plataforma.web.traza.Traza;
import bo.aportaya.tarifas.aplicacion.CU132RegistroDeParametros;
import bo.aportaya.tarifas.web.generado.modelo.EntradaVersionParametro;
import bo.aportaya.tarifas.web.generado.modelo.ParametroFinanciero;
import bo.aportaya.tarifas.web.generado.modelo.VersionDeParametro;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * CU-132 en el borde: {@code /tarifas/parametros}. El {@code @Permiso} vive en {@link TarifasController}.
 *
 * <p>Quien propone y quien aprueba salen de su token; el contrato no declara ningún campo de identidad. La segunda
 * firma la compara {@link CuatroOjos} antes de entrar al caso de uso, que la vuelve a exigir.
 */
@Component
class ParametrosWeb {

    private static final CodigoError MISMA_PERSONA = CodigoError.de(132, 3);

    private final CU132RegistroDeParametros cu132;
    private final SesionDeLaPeticion sesion;
    private final CuatroOjos cuatroOjos;

    ParametrosWeb(CU132RegistroDeParametros cu132, SesionDeLaPeticion sesion, CuatroOjos cuatroOjos) {
        this.cu132 = cu132;
        this.sesion = sesion;
        this.cuatroOjos = cuatroOjos;
    }

    List<ParametroFinanciero> catalogo() {
        Traza.marcarCasoDeUso("CU-132", "catalogo");
        return cu132.catalogo(sesion.actual()).stream()
                .map(MapeoDeParametros::parametro)
                .toList();
    }

    VersionDeParametro proponer(String codigo, UUID clave, EntradaVersionParametro cuerpo) {
        Traza.marcarCasoDeUso("CU-132", codigo);
        // Quien propone tiene que poder ser el primer actor de un cuatro ojos: token con sesión.
        cuatroOjos.actor();
        var procedencia = cuerpo.getProcedencia();
        var propuesta = new CU132RegistroDeParametros.Propuesta(
                Optional.ofNullable(cuerpo.getNivel()),
                new BigDecimal(cuerpo.getValor()),
                Optional.ofNullable(cuerpo.getMoneda()).map(EntradaVersionParametro.MonedaEnum::getValue),
                cuerpo.getVigenteDesde(),
                procedencia.getFuente(),
                procedencia.getReferencia(),
                procedencia.getFecha());
        return MapeoDeParametros.version(cu132.proponer(codigo, propuesta, clave, sesion.actual()));
    }

    VersionDeParametro aprobar(UUID versionId) {
        Traza.marcarCasoDeUso("CU-132", versionId.toString());
        var ctx = sesion.actual();
        UUID propuso = cu132.propuestaPor(versionId, ctx);
        // AP-CU132-03: otra persona, con su propio token (R-TAR-14).
        cuatroOjos.segundaFirma(ActorAutenticado.registrado(propuso, null), MISMA_PERSONA);
        return MapeoDeParametros.version(cu132.aprobar(versionId, ctx));
    }
}
