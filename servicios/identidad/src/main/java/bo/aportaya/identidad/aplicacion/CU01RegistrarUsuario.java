package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.AperturaDeCuenta;
import bo.aportaya.identidad.dominio.CanalDeVerificacion;
import bo.aportaya.identidad.dominio.DocumentoDeIdentidad;
import bo.aportaya.identidad.dominio.PoliticaDeClave;
import bo.aportaya.identidad.dominio.puertos.HasheadorDeCredencial;
import bo.aportaya.identidad.infraestructura.RegistroRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Ids;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-01 · Registro y apertura de billetera — <b>la parte que es de {@code identidad}</b>.
 *
 * <h2>Por que no es una saga</h2>
 *
 * El caso de uso toca cuatro esquemas: {@code identidad} (usuario, documento,
 * verificacion), {@code cumplimiento} (debida diligencia, calificacion de riesgo,
 * expediente), {@code nucleo_financiero} (cuenta de billetera) y {@code auditoria}
 * (listas restrictivas). Ninguno es dueño de todo, y {@code svc_identidad} no puede
 * escribir en los otros tres — invariante 11.
 *
 * <p>La tentacion es llamarlo saga. <b>No lo es, y el propio proyecto lo dice:</b>
 * {@code estado_saga} existe solo en {@code aportes}, {@code entregas},
 * {@code garantia} y {@code tarifas}, y el recetario §8b acota la saga a «cuando hay
 * dinero en vuelo». En una apertura de cuenta no hay dinero en vuelo: hay una cuenta
 * que se abre en cero.
 *
 * <p>Lo que si hay es <b>coreografia por eventos</b>, que es para lo que existe el
 * outbox: {@code identidad} crea al usuario en una transaccion local y emite
 * {@code identidad.usuario_registrado}; {@code cumplimiento} y
 * {@code nucleo-financiero} lo consumen y hacen lo suyo. Cada uno escribe en su
 * esquema, cada uno con su transaccion, y ninguno necesita permiso sobre el ajeno.
 *
 * <p>El usuario nace {@code PENDIENTE_VERIFICACION} y <b>no opera</b> hasta que esos
 * dos respondan. Esa espera no es una limitacion del diseño: es lo que el caso de uso
 * pide cuando dice que queda habilitado «solo dentro de los limites que le
 * corresponden por ese nivel de conocimiento». Marcarlo activo antes seria prometer
 * una habilitacion que todavia nadie evaluo.
 */
@Service
public class CU01RegistrarUsuario {

    /**
     * Las finalidades se consienten POR SEPARADO: aceptar los terminos no arrastra el
     * tratamiento de datos, y ninguno de los dos arrastra {@code MARKETING} — que a
     * proposito no esta en esta lista. Un consentimiento agrupado no es
     * consentimiento: es una casilla que nadie leyo.
     */
    private static final List<String> FINALIDADES = List.of("TERMINOS", "PRIVACIDAD", "TRATAMIENTO_DATOS");

    private final Datos datos;
    private final RegistroRepositorio registros;
    private final Outbox outbox;
    private final Reloj reloj;
    private final Ids ids;
    private final HasheadorDeCredencial hasheador;
    private final PoliticaDeClave politica;

    public CU01RegistrarUsuario(
            Datos datos,
            RegistroRepositorio registros,
            Outbox outbox,
            Reloj reloj,
            Ids ids,
            HasheadorDeCredencial hasheador,
            // De configuracion y no de una constante: el largo minimo es politica, y la
            // politica se cambia sin recompilar (invariante 10). `clavesQueNoSeRepiten`
            // no tiene efecto en el alta —no hay historial todavia— pero la politica es
            // una sola para toda la vida de la cuenta y se construye igual.
            @Value("${identidad.clave.largo-minimo:8}") int largoMinimoDeClave,
            @Value("${identidad.clave.no-se-repiten:5}") int clavesQueNoSeRepiten) {
        this.datos = datos;
        this.registros = registros;
        this.outbox = outbox;
        this.reloj = reloj;
        this.ids = ids;
        this.hasheador = hasheador;
        this.politica = new PoliticaDeClave(largoMinimoDeClave, clavesQueNoSeRepiten);
    }

    @Transactional
    public SalidaRegistro ejecutar(EntradaRegistro entrada, ContextoSesion ctx) {
        OffsetDateTime ahora = reloj.ahora().atOffset(ZoneOffset.UTC);

        return datos.conContexto(ctx, dsl -> {
            if (registros.telefonoYaRegistrado(dsl, entrada.telefonoE164())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(1, 3), "Ya hay una cuenta con ese telefono. Podes recuperar el acceso.");
            }
            if (registros.documentoYaRegistrado(
                    dsl, entrada.documento().hashNumero(), entrada.documento().lugarExpedicion())) {
                throw new ErrorDeNegocio(
                        CodigoError.de(1, 3), "Ya hay una cuenta con ese documento. Podes recuperar el acceso.");
            }
            if (entrada.aceptaContratos().isEmpty()) {
                throw new ErrorDeNegocio(CodigoError.de(1, 4), "Hace falta aceptar el contrato para abrir la cuenta.");
            }
            // La clave se evalua ANTES de crear a la persona: rechazarla despues dejaria
            // un usuario sin credencial, que es exactamente el estado que este caso de
            // uso existe para no producir.
            var rechazo = politica.evaluar(
                    entrada.contrasena(),
                    List.of(),
                    hasheador::coincide,
                    entrada.telefonoE164(),
                    entrada.documento().hashNumero());
            if (rechazo.isPresent()) {
                throw new ErrorDeNegocio(CodigoError.de(1, 6), mensajeDe(rechazo.get()));
            }
            if (!entrada.licenciaHabilitaBilletera()) {
                // Denegar por omision: sin licencia vigente que habilite el servicio,
                // no se abre nada. R-LIC-01 lo hace cumplir del otro lado tambien.
                throw new ErrorDeNegocio(CodigoError.de(1, 5), "El servicio no esta habilitado en este momento.");
            }

            // Elegir el correo sin dar un correo dejaria a la persona esperando algo
            // que nunca se envio.
            entrada.canalVerificacion().exigirDestino(entrada.correo());

            UUID usuario = registros.crearUsuario(
                    dsl,
                    codigoPublico(),
                    entrada.nombres(),
                    entrada.apellidos(),
                    entrada.telefonoE164(),
                    entrada.correo(),
                    entrada.fechaNacimiento(),
                    AperturaDeCuenta.PENDIENTE_VERIFICACION.name(),
                    ahora);

            // En la MISMA transaccion que el usuario. Si quedara afuera habria un
            // instante —o un fallo— con la persona creada y sin con que entrar, y
            // recuperarse de eso exige intervencion manual sobre la cuenta de alguien.
            var kdf = hasheador.parametros();
            registros.guardarCredencial(
                    dsl, usuario, hasheador.hashear(entrada.contrasena()), kdf.algoritmo(), kdf.comoJson(), ahora);
            // La clave en claro no sobrevive a la transaccion: el arreglo se borra en
            // cuanto dejo de hacer falta. Un `char[]` existe justamente para esto.
            Arrays.fill(entrada.contrasena(), '\0');

            UUID documento = registros.guardarDocumento(
                    dsl, usuario, entrada.documento(), entrada.numeroCifrado());
            registros.iniciarVerificacion(dsl, usuario, documento, "BASICO", ahora);
            registros.registrarConsentimientos(dsl, usuario, FINALIDADES, entrada.ip(), entrada.agente(), ahora);

            // Lo que sigue —diligencia, calificacion, expediente y billetera— lo hacen
            // `cumplimiento` y `nucleo-financiero` al consumir este evento. Cada uno en
            // su esquema y con su transaccion.
            outbox.emitir(
                    dsl,
                    new EventoDominio(
                            "identidad.usuario_registrado",
                            "usuario",
                            usuario,
                            Map.of("usuarioId", usuario.toString(), "nivelSolicitado", "BASICO"),
                            UUID.fromString(ctx.traza().id())));

            return new SalidaRegistro(usuario, AperturaDeCuenta.PENDIENTE_VERIFICACION);
        });
    }

    private String mensajeDe(PoliticaDeClave.MotivoDeRechazo motivo) {
        return switch (motivo) {
            case DEMASIADO_CORTA -> "Esa contrasena es demasiado corta.";
            case DERIVADA_DE_DATOS_PERSONALES -> "No uses tu telefono ni tu documento dentro de la contrasena.";
            // Inalcanzable en el alta —no hay historial—, pero el switch es exhaustivo
            // a proposito: si maniana la politica crece, el compilador avisa aca.
            case REUTILIZADA -> "Elegi una contrasena distinta.";
        };
    }

    private String codigoPublico() {
        return "AY-" + ids.nuevo().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }

    public record EntradaRegistro(
            String telefonoE164,
            String nombres,
            String apellidos,
            LocalDate fechaNacimiento,
            String correo,
            CanalDeVerificacion canalVerificacion,
            DocumentoDeIdentidad documento,
            String numeroCifrado,
            /** En claro y como {@code char[]}: se borra en cuanto se hashea. */
            char[] contrasena,
            List<UUID> aceptaContratos,
            boolean licenciaHabilitaBilletera,
            String ip,
            String agente) {}

    public record SalidaRegistro(UUID usuarioId, AperturaDeCuenta estado) {}

    /** Para que la traza del alta se pueda seguir desde el primer registro. */
    public static Traza trazaNueva(Ids ids) {
        return Traza.nueva(ids);
    }
}
