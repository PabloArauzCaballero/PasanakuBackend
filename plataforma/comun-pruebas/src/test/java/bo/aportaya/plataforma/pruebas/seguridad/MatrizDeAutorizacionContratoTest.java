package bo.aportaya.plataforma.pruebas.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.pruebas.seguridad.PoliticaDeAcceso.Ambito;
import bo.aportaya.plataforma.pruebas.seguridad.PoliticaDeAcceso.Estado;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * H13.S1.M2 · La matriz de autorizacion de los quince servicios, leida de las fuentes.
 *
 * <p>La sabana de cada servicio prueba EN VIVO que sin sesion es 401 y sin permiso es
 * 403. Esto prueba lo que ninguna sabana puede ver: que las <b>doscientas trece</b>
 * operaciones que los contratos prometen tienen una decision de acceso escrita, que el
 * permiso que exigen existe en el catalogo, que cada ruta sin sesion tiene su razon y su
 * control, y que toda operacion que nombra a una persona declara como evita cruzar de una
 * a otra (el «otro grupo, otro ambito» del plan).
 *
 * <p>Lee fuentes, no arranca nada: corre en segundos y en cualquier maquina.
 */
class MatrizDeAutorizacionContratoTest {

    /** Permisos que tiene cualquier participante: ahi el rol correcto sobre lo ajeno sigue siendo acceso indebido. */
    private static final Set<String> DE_PARTICIPANTE =
            Set.of("PARTICIPANTE", "BILLETERA_VER", "BILLETERA_OPERAR", "ORGANIZADOR", "GRUPO_CREAR");

    private static MapaDeSeguridad mapa;
    private static List<Operacion> operaciones;

    @BeforeAll
    static void leer() {
        mapa = MapaDeSeguridad.delRepositorio();
        operaciones = mapa.operaciones();
    }

    @Test
    @DisplayName("la matriz recorre los quince servicios y mas de doscientas operaciones (no una lista vacia)")
    void recorreTodo() {
        assertThat(operaciones.stream().map(Operacion::servicio).distinct().count())
                .as("servicios con contrato")
                .isEqualTo(15);
        assertThat(operaciones).as("operaciones de todos los contratos").hasSizeGreaterThan(200);
    }

    @Test
    @DisplayName("rechaza: toda operacion que el contrato promete tiene una decision de acceso en su controlador")
    void todaOperacionDecideSuAcceso() {
        List<String> sinDecision = new ArrayList<>();
        for (Operacion operacion : operaciones) {
            Decision decision = mapa.decisionDe(operacion);
            if (!decision.decide()) {
                sinDecision.add("%s -> %s %s".formatted(operacion, decision.tipo(), decision.ubicacion(mapa.raiz())));
            }
        }
        assertThat(sinDecision)
                .as("operacion sin @Permiso ni @Publico, o sin controlador: queda abierta o no existe")
                .isEmpty();
    }

    @Test
    @DisplayName("rechaza: todo permiso que un controlador exige existe en el catalogo sembrado")
    void todoPermisoExisteEnElCatalogo() {
        Set<String> catalogo = catalogoDeCodigos();
        List<String> inventados = new ArrayList<>();
        for (Operacion operacion : operaciones) {
            Decision decision = mapa.decisionDe(operacion);
            if (decision.tipo() == Decision.Tipo.PERMISO && !catalogo.contains(decision.valor())) {
                inventados.add("%s exige «%s», que no esta en seeders/minimos/10-roles-y-permisos.json (%s)"
                        .formatted(operacion, decision.valor(), decision.ubicacion(mapa.raiz())));
            }
        }
        assertThat(inventados)
                .as("un permiso que solo vive en una anotacion no se le puede dar a nadie: queda cerrado o abierto")
                .isEmpty();
    }

    @Test
    @DisplayName("rechaza: la lista de rutas sin sesion es exactamente la declarada, ni una mas ni una menos")
    void lasRutasPublicasSonLasDeclaradas() {
        Set<String> declaradas = new TreeSet<>();
        declaradas.addAll(PoliticaDeAcceso.PUBLICAS_DE_LECTURA.keySet());
        declaradas.addAll(PoliticaDeAcceso.PUBLICAS_QUE_ESCRIBEN.keySet());

        Set<String> reales = operaciones.stream()
                .filter(o -> mapa.decisionDe(o).tipo() == Decision.Tipo.PUBLICO)
                .map(Operacion::id)
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(reales)
                .as(
                        "una ruta nueva sin sesion exige una decision escrita en PoliticaDeAcceso; una que ya no lo es, se borra")
                .containsExactlyInAnyOrderElementsOf(declaradas);
    }

    @Test
    @DisplayName(
            "rechaza: toda ruta sin sesion dice por que, y la que escribe tiene un control que existe en el codigo")
    void todaRutaPublicaTieneRazonYControl() {
        List<String> faltas = new ArrayList<>();
        for (Operacion operacion : operaciones) {
            Decision decision = mapa.decisionDe(operacion);
            if (decision.tipo() != Decision.Tipo.PUBLICO) {
                continue;
            }
            if (decision.valor().strip().length() < 30) {
                faltas.add("%s: @Publico sin justificacion real (%s)"
                        .formatted(operacion, decision.ubicacion(mapa.raiz())));
            }
            if (PoliticaDeAcceso.PUBLICAS_DE_LECTURA.containsKey(operacion.id()) && operacion.cambiaEstado()) {
                faltas.add("%s: declarada «de lectura» pero cambia estado".formatted(operacion));
            }
            var control = PoliticaDeAcceso.PUBLICAS_QUE_ESCRIBEN.get(operacion.id());
            if (control != null && !contiene(control.archivo(), control.patron())) {
                faltas.add("%s: el control «%s» ya no esta en %s"
                        .formatted(operacion, control.patron(), control.archivo()));
            }
        }
        assertThat(faltas).isEmpty();
    }

    @Test
    @DisplayName(
            "rechaza: toda ruta publica que CAMBIA estado declara un control compensatorio (ninguna queda sin dueno)")
    void ningunaRutaPublicaEscribeSinControl() {
        List<String> sinControl = new ArrayList<>();
        for (Operacion operacion : operaciones) {
            if (mapa.decisionDe(operacion).tipo() == Decision.Tipo.PUBLICO
                    && operacion.cambiaEstado()
                    && !PoliticaDeAcceso.PUBLICAS_QUE_ESCRIBEN.containsKey(operacion.id())) {
                sinControl.add(operacion.toString());
            }
        }
        assertThat(sinControl)
                .as("una ruta sin sesion que escribe sin control compensatorio es un IDOR esperando a que alguien"
                        + " conozca un UUID: no hay lista de excepciones")
                .isEmpty();
    }

    @Test
    @DisplayName("rechaza: toda operacion de participante que nombra a una persona declara como no cruza a otra")
    void todaOperacionConPersonaDeclaraSuAmbito() {
        List<String> sinDeclarar = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        for (Operacion operacion : operaciones) {
            Decision decision = mapa.decisionDe(operacion);
            boolean deParticipante =
                    decision.tipo() == Decision.Tipo.PERMISO && DE_PARTICIPANTE.contains(decision.valor());
            if (operacion.nombraAUnaPersona() && deParticipante) {
                vistas.add(operacion.id());
                if (!PoliticaDeAcceso.AMBITO.containsKey(operacion.id())) {
                    sinDeclarar.add("%s (%s) exige %s y nombra a una persona; sin ambito declarado"
                            .formatted(operacion, decision.ubicacion(mapa.raiz()), decision.valor()));
                }
            }
        }
        assertThat(sinDeclarar)
                .as("el rol correcto sobre el recurso ajeno sigue siendo acceso indebido (regla 90.1.2-3)")
                .isEmpty();
        assertThat(PoliticaDeAcceso.AMBITO.keySet())
                .as("entradas de PoliticaDeAcceso.AMBITO que ya no corresponden a ninguna operacion")
                .isSubsetOf(vistas);
    }

    @Test
    @DisplayName(
            "rechaza: el control de ambito declarado esta de verdad en el codigo, o la politica de fila que lo sostiene")
    void elControlDeAmbitoExiste() {
        List<String> ausentes = new ArrayList<>();
        for (Map.Entry<String, Ambito> e : PoliticaDeAcceso.AMBITO.entrySet()) {
            Ambito ambito = e.getValue();
            if (ambito.estado() != Estado.ABIERTO && !contiene(ambito.archivo(), ambito.patron())) {
                ausentes.add("%s declara %s pero «%s» no esta en %s"
                        .formatted(e.getKey(), ambito.estado(), ambito.patron(), ambito.archivo()));
            }
        }
        assertThat(ausentes).isEmpty();
    }

    private boolean contiene(String archivoRelativo, String patron) {
        Path archivo = mapa.raiz().resolve(archivoRelativo);
        try {
            return Files.isRegularFile(archivo)
                    && Pattern.compile(patron)
                            .matcher(Files.readString(archivo))
                            .find();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Roles y permisos sembrados: el guardia deja pasar si el token trae el codigo como permiso o como rol. */
    @SuppressWarnings("unchecked")
    private Set<String> catalogoDeCodigos() {
        Path semilla = mapa.raiz().resolve("seeders/minimos/10-roles-y-permisos.json");
        try {
            Map<String, Object> documento = new Yaml().load(Files.readString(semilla));
            Set<String> codigos = new HashSet<>();
            for (Object bloque : (List<Object>) documento.get("bloques")) {
                Map<String, Object> b = (Map<String, Object>) bloque;
                if ("rol".equals(b.get("tabla")) || "permiso".equals(b.get("tabla"))) {
                    for (Object fila : (List<Object>) b.get("filas")) {
                        codigos.add(String.valueOf(((Map<String, Object>) fila).get("codigo")));
                    }
                }
            }
            assertThat(codigos).as("el catalogo sembrado se leyo").hasSizeGreaterThan(30);
            return codigos;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
