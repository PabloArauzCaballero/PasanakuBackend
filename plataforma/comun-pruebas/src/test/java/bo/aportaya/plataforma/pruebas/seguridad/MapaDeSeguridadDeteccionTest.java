package bo.aportaya.plataforma.pruebas.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * La matriz tiene que CAER cuando hay un agujero. Aca se le inyecta uno de cada tipo a un
 * servicio sintetico y se comprueba que lo ve: un endpoint sin decision, un {@code @Publico}
 * con punto y coma y parentesis dentro de su razon (que parte mal un lector ingenuo), un
 * Javadoc que menciona la anotacion sin tenerla, y una operacion que ningun controlador
 * implementa. Sin esto, «la matriz esta verde» podria significar «la matriz no ve nada».
 */
class MapaDeSeguridadDeteccionTest {

    @TempDir
    Path raiz;

    private MapaDeSeguridad mapa;

    @BeforeEach
    void armarServicioSintetico() throws IOException {
        Path openapi = raiz.resolve("servicios/demo/src/main/resources/openapi");
        Path web = raiz.resolve("servicios/demo/src/main/java/bo/demo/web");
        Files.createDirectories(openapi);
        Files.createDirectories(web);
        Files.writeString(
                openapi.resolve("demo.yaml"),
                """
                openapi: 3.1.0
                info: { title: demo, version: 1.0.0 }
                components:
                  parameters:
                    Usuario: { name: usuarioId, in: path, required: true, schema: { type: string } }
                  schemas:
                    Entrada:
                      type: object
                      properties:
                        usuarioId: { type: string }
                paths:
                  /demo/protegida:
                    get: { operationId: protegida, responses: { '200': { description: ok } } }
                  /demo/abierta-sin-decidir:
                    post:
                      operationId: abiertaSinDecidir
                      requestBody:
                        content: { application/json: { schema: { $ref: '#/components/schemas/Entrada' } } }
                      responses: { '200': { description: ok } }
                  /demo/publica/{usuarioId}:
                    parameters: [ { $ref: '#/components/parameters/Usuario' } ]
                    get: { operationId: publicaConPuntoYComa, responses: { '200': { description: ok } } }
                  /demo/fantasma:
                    get: { operationId: nadieLaImplementa, responses: { '200': { description: ok } } }
                  /demo/con-alternativas:
                    get: { operationId: conAlternativas, responses: { '200': { description: ok } } }
                """);
        Files.writeString(
                web.resolve("DemoController.java"),
                """
                package bo.demo.web;

                /**
                 * Mencionar {@code @Permiso} aca no protege nada: es un comentario.
                 */
                public class DemoController {

                    @Override
                    @Permiso("DEMO_LEER")
                    public String protegida() {
                        return "ok";
                    }

                    @Override
                    public String abiertaSinDecidir() {
                        return "queda abierta";
                    }

                    @Override
                    @Publico("el alta es publica (CU-01); se pide antes de la sesion, y no hay nada que ver")
                    public String publicaConPuntoYComa() {
                        return "x";
                    }

                    @Override
                    @Permiso("DEMO_LEER")
                    @PermisoAlternativo({"DEMO_VER", "DEMO_ADMINISTRAR"})
                    public String conAlternativas() {
                        return "x";
                    }
                }
                """);
        mapa = MapaDeSeguridad.de(raiz);
    }

    @Test
    @DisplayName(
            "rechaza: un endpoint sin @Permiso ni @Publico se ve como NINGUNA, aunque el Javadoc nombre la anotacion")
    void veLoQueNoTieneDecision() {
        Operacion sinDecidir = operacion("abiertaSinDecidir");

        assertThat(mapa.decisionDe(sinDecidir).tipo()).isEqualTo(Decision.Tipo.NINGUNA);
        assertThat(mapa.decisionDe(sinDecidir).decide()).isFalse();
    }

    @Test
    @DisplayName("el permiso se lee tal cual esta escrito y la linea apunta a la declaracion")
    void leeElPermiso() {
        Decision decision = mapa.decisionDe(operacion("protegida"));

        assertThat(decision.tipo()).isEqualTo(Decision.Tipo.PERMISO);
        assertThat(decision.valor()).isEqualTo("DEMO_LEER");
        assertThat(decision.linea()).isEqualTo(10);
    }

    @Test
    @DisplayName("una anotacion con arreglo entre llaves no esconde el @Override ni el permiso del metodo")
    void leeUnMetodoConAlternativasEnArreglo() {
        Decision decision = mapa.decisionDe(operacion("conAlternativas"));

        assertThat(decision.tipo()).isEqualTo(Decision.Tipo.PERMISO);
        assertThat(decision.valor()).isEqualTo("DEMO_LEER");
    }

    @Test
    @DisplayName("una justificacion de @Publico con punto y coma y parentesis se lee entera")
    void leeLaJustificacionEntera() {
        Decision decision = mapa.decisionDe(operacion("publicaConPuntoYComa"));

        assertThat(decision.tipo()).isEqualTo(Decision.Tipo.PUBLICO);
        assertThat(decision.valor()).startsWith("el alta es publica (CU-01); se pide antes de la sesion");
    }

    @Test
    @DisplayName("rechaza: una operacion que el contrato promete y ningun controlador implementa se ve SIN_METODO")
    void veLaOperacionFantasma() {
        assertThat(mapa.decisionDe(operacion("nadieLaImplementa")).tipo()).isEqualTo(Decision.Tipo.SIN_METODO);
    }

    @Test
    @DisplayName("una persona en la ruta o en el cuerpo se detecta, venga del parametro compartido o de la propiedad")
    void detectaALaPersona() {
        assertThat(operacion("publicaConPuntoYComa").nombraAUnaPersona()).isTrue();
        assertThat(operacion("abiertaSinDecidir").nombraAUnaPersona()).isTrue();
        assertThat(operacion("protegida").nombraAUnaPersona()).isFalse();
    }

    @Test
    @DisplayName("la mascara deja el comentario y el contenido de los textos en blanco sin mover ninguna posicion")
    void laMascaraConservaLasPosiciones() {
        String original = "a; // b; c\n\"x;}\" /* y; */ z;";
        String mascara = FuenteJava.enmascarar(original);

        assertThat(mascara).hasSameSizeAs(original);
        assertThat(mascara.indexOf(';')).isEqualTo(original.indexOf(';'));
        assertThat(mascara).doesNotContain("b;").doesNotContain("x;}").doesNotContain("y;");
        assertThat(mascara.lines().count()).isEqualTo(original.lines().count());
    }

    private Operacion operacion(String id) {
        List<Operacion> todas = mapa.operaciones();
        return todas.stream().filter(o -> o.id().equals(id)).findFirst().orElseThrow();
    }
}
