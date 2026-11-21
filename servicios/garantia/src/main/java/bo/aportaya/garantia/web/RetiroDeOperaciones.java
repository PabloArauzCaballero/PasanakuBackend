package bo.aportaya.garantia.web;

import bo.aportaya.plataforma.web.errores.ErrorApi;
import bo.aportaya.plataforma.web.traza.Traza;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce {@link OperacionRetirada} a {@code 410} con el cuerpo de error del contrato. Va antes que el manejador
 * comun porque aquel no conoce el {@code 410} y lo dejaria salir como {@code 500}.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class RetiroDeOperaciones {

    @ExceptionHandler(OperacionRetirada.class)
    ResponseEntity<ErrorApi> retirada(OperacionRetirada e) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ErrorApi.de(e.codigo().valor(), e.getMessage(), Traza.actual()));
    }
}
