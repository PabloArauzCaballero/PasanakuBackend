package bo.aportaya.plataforma.pruebas.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * El corte MVC de este proyecto, en una anotacion.
 *
 * <p>Lo que prueba es el <b>contrato HTTP</b>: ruta, verbo, estado, JSON, validacion
 * del contrato, manejador de errores y guardia. El caso de uso va doblado con
 * {@code @MockitoBean}, porque lo que hace el caso de uso ya se prueba contra
 * PostgreSQL real en {@code CU<NN>Test} — repetirlo aca con un doble seria probar el
 * doble (ADR-026, ADR-041).
 *
 * <p>Al reves tambien: lo que se prueba aca <b>no se puede probar alla</b>. Una prueba
 * de integracion llama al caso de uso directamente y nunca pasa por la guardia, por el
 * deserializador ni por el manejador de errores. El {@code 403} del autenticado sin
 * permiso solo existe en este nivel.
 *
 * <p>{@code addFilters = true} a proposito: apagar los filtros es la forma mas comun de
 * escribir una suite de seguridad que pasa siempre, porque apaga justo la cadena que
 * se queria probar.
 *
 * <p>Uso:
 *
 * <pre>{@code
 * @PruebaWeb(EntregasController.class)
 * class EntregasControllerWebTest {
 *     @Autowired MockMvc mvc;
 *     @MockitoBean CU22LiquidarEntrega cu22;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@WebMvcTest
@AutoConfigureMockMvc(addFilters = true)
@Import(ContextoWebDePrueba.class)
@TestPropertySource(
        properties = {
            // El esquema y el JWKS existen porque los pide el cableado, no porque la
            // prueba los use: aca no hay base ni se descarga ninguna clave.
            "aportaya.esquema=prueba",
            "aportaya.jwt.jwks-uri=http://no-se-consulta.invalido/jwks.json",
            "aportaya.zona-horaria=America/La_Paz",
            // Un aviso de Spring Security por peticion tapa el fallo que importa.
            "logging.level.org.springframework.security=WARN",
        })
public @interface PruebaWeb {

    /** Los controladores del corte. Vacio carga todos los del servicio. */
    @org.springframework.core.annotation.AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};

    /**
     * Las claves que el controlador de ESTE servicio pide por {@code @Value}.
     *
     * <p>Van aca y no en las de arriba porque son del servicio: meter la pimienta de
     * identidad o el tarifario de grupos en la anotacion compartida obligaria a los
     * catorce a arrastrar la configuracion de los otros trece.
     */
    @org.springframework.core.annotation.AliasFor(annotation = WebMvcTest.class, attribute = "properties")
    String[] properties() default {};
}
