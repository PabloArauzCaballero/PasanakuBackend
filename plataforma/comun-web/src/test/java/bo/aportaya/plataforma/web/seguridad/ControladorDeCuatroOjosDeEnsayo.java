package bo.aportaya.plataforma.web.seguridad;

import bo.aportaya.plataforma.dominio.CodigoError;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Un acto de dos firmas de ensayo, con el uso documentado de {@link CuatroOjos}: el primer
 * acto guarda su actor del token; el segundo lo rehidrata y pide la segunda firma. La
 * "tabla" es un mapa en memoria porque lo que se prueba es la capa web, no la persistencia.
 *
 * <p>Las entradas no declaran ningun aprobador: un cuerpo que lo traiga es un campo no
 * declarado y cae en {@code 400 AP-VAL-02}.
 */
@RestController
class ControladorDeCuatroOjosDeEnsayo {

    static final CodigoError CODIGO = CodigoError.de(99, 1);

    record EntradaPropuesta(String motivo) {}

    record EntradaAprobacion(String comentario) {}

    private final CuatroOjos cuatroOjos;
    private final Map<UUID, ActorAutenticado> propuestas = new ConcurrentHashMap<>();

    ControladorDeCuatroOjosDeEnsayo(CuatroOjos cuatroOjos) {
        this.cuatroOjos = cuatroOjos;
    }

    @PostMapping("/ensayo/propuestas")
    @Permiso("REVERSO_AUTORIZAR")
    ResponseEntity<Map<String, String>> proponer(@RequestBody EntradaPropuesta entrada) {
        UUID id = UUID.randomUUID();
        propuestas.put(id, cuatroOjos.actor());
        return ResponseEntity.status(201).body(Map.of("id", id.toString()));
    }

    @PostMapping("/ensayo/propuestas/{id}/aprobacion")
    @Permiso("REVERSO_AUTORIZAR")
    Map<String, String> aprobar(@PathVariable UUID id, @RequestBody EntradaAprobacion entrada) {
        ActorAutenticado primero = propuestas.get(id);
        SegundaFirma firma = cuatroOjos.segundaFirma(primero, CODIGO);
        return Map.of(
                "propusoUsuario", firma.primero().usuarioId().toString(),
                "aproboUsuario", firma.segundo().usuarioId().toString(),
                "aproboSesion", firma.segundo().sesionId().toString());
    }
}
