package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.AperturaDeBilleteras.Resultado;
import bo.aportaya.nucleofinanciero.aplicacion.AperturaDeBilleteras.UsuarioRegistrado;
import bo.aportaya.nucleofinanciero.infraestructura.CuentaBilleteraRepositorio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Carril Q · quien abre las billeteras {@code USUARIO} y {@code GRUPO} (CU-01 paso 9-10, CU-20 paso 3; S4/S5 del plan 20).
 * El productor es un doble de contrato (el evento trae los campos que {@code CU01RegistrarUsuario}/{@code CU20CrearGrupo}
 * emiten). El recorrido con Kafka y los servicios reales NO esta cubierto aca.
 */
class CUAperturaDeBilleterasTest extends BaseDeApertura {

    private Optional<UUID> operativa(UUID usuario) {
        return dsl.transactionResult(
                c -> new CuentaBilleteraRepositorio().operativaDe(DSL.using(c), usuario, Moneda.BOB));
    }

    @Test
    @DisplayName("Dado un usuario_registrado · Cuando nucleo lo consume · Entonces existe UNA billetera BOB ACTIVA"
            + " SIMPLIFICADA en cero, con politica, operativa para el resto del servicio y con su aviso en el outbox")
    void abreLaBilleteraDeLaPersona() {
        UUID usuario = fixtura.usuario();
        var e = registrado(usuario);

        var salida = abrir(apertura, e);

        assertThat(salida.resultado()).isEqualTo(Resultado.ABIERTA);
        var fila = dsl.fetchOne(
                """
                SELECT tipo, usuario_id, grupo_id, moneda, estado, nivel_debida_diligencia, saldo_disponible,
                       permite_saldo_negativo, politica_billetera_id, numero_cuenta
                  FROM nucleo_financiero.cuenta_billetera WHERE id = ?
                """,
                salida.cuentaId().orElseThrow());
        assertThat(fila.get("tipo")).isEqualTo("USUARIO");
        assertThat(fila.get("usuario_id")).isEqualTo(usuario);
        assertThat(fila.get("grupo_id")).isNull();
        assertThat(fila.get("moneda")).isEqualTo("BOB");
        assertThat(fila.get("estado")).isEqualTo("ACTIVA");
        assertThat(fila.get("nivel_debida_diligencia")).isEqualTo("SIMPLIFICADA");
        assertThat((BigDecimal) fila.get("saldo_disponible")).isEqualByComparingTo("0");
        assertThat(fila.get("permite_saldo_negativo")).isEqualTo(false);
        assertThat(fila.get("politica_billetera_id")).isNotNull();
        assertThat(fila.get("numero_cuenta").toString()).startsWith("BOB-").hasSizeLessThanOrEqualTo(20);
        // Lo que GET /billetera/cuentas/propia exige (antes: 422 AP-CU13-01 a toda persona).
        assertThat(operativa(usuario)).contains(salida.cuentaId().orElseThrow());
        assertThat(marcas(e.eventoId())).isEqualTo(1);
        assertThat(avisosDeApertura(salida.cuentaId().orElseThrow())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un grupo_creado · Cuando nucleo lo consume · Entonces existe UNA billetera BOB del GRUPO (titular"
                    + " el grupo, nunca una persona: R-GRP-04), ACTIVA, ESTANDAR, sin politica P2P")
    void abreLaBilleteraDelGrupo() {
        UUID grupo = grupoReal();
        var e = creado(grupo);

        var salida = abrir(apertura, e);

        assertThat(salida.resultado()).isEqualTo(Resultado.ABIERTA);
        var fila = dsl.fetchOne(
                """
                SELECT tipo, usuario_id, grupo_id, moneda, estado, nivel_debida_diligencia, politica_billetera_id,
                       numero_cuenta FROM nucleo_financiero.cuenta_billetera WHERE id = ?
                """,
                salida.cuentaId().orElseThrow());
        assertThat(fila.get("tipo")).isEqualTo("GRUPO");
        assertThat(fila.get("grupo_id")).isEqualTo(grupo);
        assertThat(fila.get("usuario_id")).isNull();
        assertThat(fila.get("estado")).isEqualTo("ACTIVA");
        assertThat(fila.get("nivel_debida_diligencia")).isEqualTo("ESTANDAR");
        assertThat(fila.get("politica_billetera_id")).isNull();
        assertThat(fila.get("numero_cuenta").toString()).startsWith("GRP-");
        assertThat(avisosDeApertura(salida.cuentaId().orElseThrow())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "evento duplicado: el mismo usuario_registrado / grupo_creado dos veces deja UNA cuenta, UNA marca y UN"
                    + " aviso")
    void eventoDuplicado() {
        UUID usuario = fixtura.usuario();
        var eu = registrado(usuario);
        var primera = abrir(apertura, eu);
        assertThat(abrir(apertura, eu).resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(cuentasDeUsuario(usuario)).isEqualTo(1);
        assertThat(marcas(eu.eventoId())).isEqualTo(1);
        assertThat(avisosDeApertura(primera.cuentaId().orElseThrow())).isEqualTo(1);

        UUID grupo = grupoReal();
        var eg = creado(grupo);
        var abierta = abrir(apertura, eg);
        assertThat(abrir(apertura, eg).resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(cuentasDeGrupo(grupo)).isEqualTo(1);
        assertThat(avisosDeApertura(abierta.cuentaId().orElseThrow())).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "reintento: el productor republica el alta con OTRO id de evento · Entonces no se abre una segunda cuenta"
                    + " ni se repite el aviso")
    void reintentoConOtroEvento() {
        UUID usuario = fixtura.usuario();
        var primera = abrir(apertura, registrado(usuario));
        var otra = abrir(apertura, registrado(usuario));

        assertThat(otra.resultado()).isEqualTo(Resultado.YA_EXISTIA);
        assertThat(otra.cuentaId()).isEqualTo(primera.cuentaId());
        assertThat(cuentasDeUsuario(usuario)).isEqualTo(1);
        assertThat(avisosDeApertura(primera.cuentaId().orElseThrow())).isEqualTo(1);

        UUID grupo = grupoReal();
        var g1 = abrir(apertura, creado(grupo));
        var g2 = abrir(apertura, creado(grupo));
        assertThat(g2.resultado()).isEqualTo(Resultado.YA_EXISTIA);
        assertThat(g2.cuentaId()).isEqualTo(g1.cuentaId());
        assertThat(cuentasDeGrupo(grupo)).isEqualTo(1);
    }

    @Test
    @DisplayName("fuera de orden: el evento llega ANTES de que exista el usuario o el grupo · Entonces la clave foranea"
            + " lo rechaza, no queda cuenta ni marca, y el reproceso del MISMO evento la abre cuando llegue")
    void fueraDeOrden() {
        UUID usuario = UUID.randomUUID();
        var eu = registrado(usuario);
        assertThatThrownBy(() -> abrir(apertura, eu))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("fk_cuenta_billetera_usuario_id");
        assertThat(cuentasDeUsuario(usuario)).isZero();
        assertThat(marcas(eu.eventoId())).isZero();

        UUID grupo = UUID.randomUUID();
        var eg = creado(grupo);
        assertThatThrownBy(() -> abrir(apertura, eg))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("fk_cuenta_billetera_grupo_id");
        assertThat(cuentasDeGrupo(grupo)).isZero();
        assertThat(marcas(eg.eventoId())).isZero();

        // Llega el usuario real: el mismo evento, ahora si.
        UUID real = fixtura.usuario();
        var reproceso = new UsuarioRegistrado(eu.eventoId(), eu.correlationId(), real);
        assertThat(abrir(apertura, reproceso).resultado()).isEqualTo(Resultado.ABIERTA);
        assertThat(cuentasDeUsuario(real)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "rechaza abrir sin politica de billetera vigente: no se crea una cuenta que niega todo; no queda marca y"
                    + " el reproceso la abre cuando la politica exista")
    void rechazaSinPolitica() {
        UUID usuario = fixtura.usuario();
        var e = registrado(usuario);
        var sinPolitica = aperturaCon("ACTIVA", new CuentaBilleteraRepositorio() {
            @Override
            public Optional<UUID> politicaVigente(DSLContext d, Moneda moneda) {
                return Optional.empty();
            }
        });

        assertThatThrownBy(() -> abrir(sinPolitica, e))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("politica de billetera");

        assertThat(cuentasDeUsuario(usuario)).isZero();
        assertThat(marcas(e.eventoId())).isZero();
        assertThat(abrir(apertura, e).resultado()).isEqualTo(Resultado.ABIERTA);
    }

    @Test
    @DisplayName("rechaza reabrir lo cerrado: si la persona cerro su billetera, un reproceso del alta NO abre otra")
    void noReabreLaCerrada() {
        UUID usuario = fixtura.usuario();
        UUID cerrada = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        dslFixtura.execute("UPDATE nucleo_financiero.cuenta_billetera SET estado = 'CERRADA' WHERE id = ?", cerrada);

        var salida = abrir(apertura, registrado(usuario));

        assertThat(salida.resultado()).isEqualTo(Resultado.YA_EXISTIA);
        assertThat(cuentasDeUsuario(usuario)).isEqualTo(1);
    }

    @Test
    @DisplayName("estado inicial: con EN_APERTURA la cuenta nace cerrada a toda operacion (no es operativa); un valor"
            + " desconocido impide arrancar")
    void estadoInicialConfigurable() {
        UUID usuario = fixtura.usuario();
        var enApertura = aperturaCon("EN_APERTURA", new CuentaBilleteraRepositorio());

        var salida = abrir(enApertura, registrado(usuario));

        assertThat(dsl.fetchOne(
                                "SELECT estado FROM nucleo_financiero.cuenta_billetera WHERE id = ?",
                                salida.cuentaId().orElseThrow())
                        .get(0))
                .isEqualTo("EN_APERTURA");
        assertThat(operativa(usuario)).isEmpty();
        assertThatThrownBy(() -> aperturaCon("ABIERTA_DE_PAR_EN_PAR", new CuentaBilleteraRepositorio()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("usuarios con prefijos comunes en UUID abren billetera sin colisionar en numero_cuenta")
    void usuariosConPrefijoComunNoColisionan() {
        UUID u1 = fixtura.usuario();
        UUID u2 = fixtura.usuario();
        var salida1 = abrir(apertura, registrado(u1));
        var salida2 = abrir(apertura, registrado(u2));
        assertThat(salida1.resultado()).isEqualTo(Resultado.ABIERTA);
        assertThat(salida2.resultado()).isEqualTo(Resultado.ABIERTA);
        assertThat(salida1.cuentaId()).isNotEqualTo(salida2.cuentaId());
    }

    @Test
    @DisplayName(
            "concurrencia: dos eventos DISTINTOS del mismo usuario (y del mismo grupo) al mismo tiempo dejan UNA sola"
                    + " cuenta: el indice unico R-BIL-04 decide")
    void concurrencia() throws Exception {
        UUID usuario = fixtura.usuario();
        UUID grupo = grupoReal();
        var u = enParalelo(
                () -> abrir(apertura, registrado(usuario)).resultado(),
                () -> abrir(apertura, registrado(usuario)).resultado());
        var g = enParalelo(() -> abrir(apertura, creado(grupo)).resultado(), () -> abrir(apertura, creado(grupo))
                .resultado());

        assertThat(u).containsExactlyInAnyOrder(Resultado.ABIERTA, Resultado.YA_EXISTIA);
        assertThat(g).containsExactlyInAnyOrder(Resultado.ABIERTA, Resultado.YA_EXISTIA);
        assertThat(cuentasDeUsuario(usuario)).isEqualTo(1);
        assertThat(cuentasDeGrupo(grupo)).isEqualTo(1);
    }

    @SafeVarargs
    private <T> List<T> enParalelo(Callable<T>... tareas) throws Exception {
        var hilos = Executors.newFixedThreadPool(tareas.length);
        try {
            var largada = new CountDownLatch(1);
            var futuros = new ArrayList<Future<T>>();
            for (var tarea : tareas) {
                futuros.add(hilos.submit(() -> {
                    largada.await();
                    return tarea.call();
                }));
            }
            largada.countDown();
            var resultados = new ArrayList<T>();
            for (var f : futuros) {
                resultados.add(f.get(60, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            hilos.shutdownNow();
        }
    }
}
