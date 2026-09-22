package bo.aportaya.plataforma.web.errores;

import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.Publico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Un controlador que existe solo para que el manejador global tenga algo que manejar.
 *
 * <p>No se prueba el manejador llamandolo a mano: llamarlo a mano saltea el
 * {@code DispatcherServlet}, y entonces lo que se prueba es un metodo, no el
 * comportamiento de la API. La mitad de los casos —el JSON roto, el verbo equivocado,
 * el tipo de contenido, la ruta inexistente— <b>solo existen</b> pasando por la
 * infraestructura MVC.
 *
 * <p>Vive en las fuentes de prueba: no se despliega, no aparece en ningun contrato y
 * ningun servicio lo carga.
 */
@RestController
@org.springframework.validation.annotation.Validated
public class ControladorDeEnsayo {

    /** Lo que devuelve el camino feliz, con dinero adentro para probar la frontera. */
    public record Salida(UUID id, String nombre, Dinero saldo) {}

    /** Entrada con las tres validaciones tipicas del proyecto. */
    public record Entrada(@NotBlank String nombre, @Email String correo, @Size(min = 8) String clave, Dinero monto) {}

    @PostMapping("/ensayo/recursos")
    @Permiso("BILLETERA_OPERAR")
    public ResponseEntity<Salida> crear(@Valid @RequestBody Entrada entrada) {
        return ResponseEntity.status(201)
                .body(new Salida(
                        UUID.fromString("00000000-0000-4000-8000-00000000000a"),
                        entrada.nombre(),
                        Dinero.de("120.50", Moneda.BOB)));
    }

    @GetMapping("/ensayo/recursos/{id}")
    @Permiso("BILLETERA_VER")
    public Salida ver(@PathVariable UUID id) {
        return new Salida(id, "recurso", Dinero.de("0.00", Moneda.BOB));
    }

    @GetMapping("/ensayo/abierto")
    @Publico("es el endpoint con el que se prueba que @Publico realmente abre")
    public Map<String, String> abierto() {
        return Map.of("estado", "abierto");
    }

    @GetMapping("/ensayo/solo-rol")
    @Permiso("ORGANIZADOR")
    public Map<String, String> soloRol() {
        return Map.of("estado", "ok");
    }

    @GetMapping("/ensayo/con-parametro")
    @Permiso("BILLETERA_VER")
    public Map<String, String> conParametro(@RequestParam String desde) {
        return Map.of("desde", desde);
    }

    @GetMapping("/ensayo/con-parametro-restringido")
    @Permiso("BILLETERA_VER")
    public Map<String, String> conParametroRestringido(
            @RequestParam @Pattern(regexp = "^\\d{4}-\\d{2}$") String periodo) {
        return Map.of("periodo", periodo);
    }

    @PostMapping("/ensayo/con-cabecera")
    @Permiso("BILLETERA_OPERAR")
    public Map<String, String> conCabecera(@RequestHeader("Idempotency-Key") UUID clave) {
        return Map.of("clave", clave.toString());
    }

    // ---------------------------------------------------------------- fallos --

    @GetMapping("/ensayo/regla-de-negocio")
    @Permiso("BILLETERA_VER")
    public Salida reglaDeNegocio() {
        throw new ErrorDeNegocio(CodigoError.de(21, 3), "El aporte no cubre la obligacion.", Map.of("faltan", "35.00"));
    }

    @GetMapping("/ensayo/regla-del-dominio")
    @Permiso("BILLETERA_VER")
    public Salida reglaDelDominio() {
        throw new ErrorDeDominio("un monto negativo no es un monto");
    }

    @GetMapping("/ensayo/restriccion-conocida")
    @Permiso("BILLETERA_VER")
    public Salida restriccionConocida() {
        throw new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"uq_envio_idempotencia\"");
    }

    @GetMapping("/ensayo/restriccion-desconocida")
    @Permiso("BILLETERA_VER")
    public Salida restriccionDesconocida() {
        throw new DataIntegrityViolationException(
                "ERROR: null value in column \"columna_que_el_catalogo_no_conoce\" of relation \"tabla_secreta\"");
    }

    @GetMapping("/ensayo/fallo-no-previsto")
    @Permiso("BILLETERA_VER")
    public Salida falloNoPrevisto() {
        throw new IllegalStateException("jdbc:postgresql://interna:5432/pasanaku password=supersecreto");
    }
}
