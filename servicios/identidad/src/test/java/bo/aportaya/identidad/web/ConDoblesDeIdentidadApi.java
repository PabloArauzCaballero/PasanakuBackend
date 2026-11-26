package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU134ConsultarParticipante;
import bo.aportaya.identidad.aplicacion.CU134GestionarMedida;
import bo.aportaya.identidad.aplicacion.CU134LiberarEntrega;
import bo.aportaya.identidad.aplicacion.ConsultarContactoEnGrupo;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Los dobles que {@link VerificacionesController} necesita para levantar y que una prueba de verificaciones
 * no ejercita: todo {@code /identidad/*} es una sola {@code IdentidadApi}, asi que el controlador carga
 * tambien el contacto en grupo y las rutas de CU-134 ({@link RutasDeParticipantes}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(
        types = {
            ConsultarContactoEnGrupo.class,
            CU134ConsultarParticipante.class,
            CU134GestionarMedida.class,
            CU134LiberarEntrega.class
        })
@interface ConDoblesDeIdentidadApi {}
