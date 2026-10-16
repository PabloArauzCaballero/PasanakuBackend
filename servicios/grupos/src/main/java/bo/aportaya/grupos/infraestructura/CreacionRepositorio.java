package bo.aportaya.grupos.infraestructura;

import static bo.aportaya.grupos.generado.Tables.CONFIGURACION_GRUPO;
import static bo.aportaya.grupos.generado.Tables.CUPO;
import static bo.aportaya.grupos.generado.Tables.GRUPO;
import static bo.aportaya.grupos.generado.Tables.REGLAMENTO_GRUPO;

import bo.aportaya.grupos.dominio.GrupoNuevo;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/** Alta del grupo, su configuracion, su reglamento y sus cupos. */
@Component
public class CreacionRepositorio {

    public Optional<Alta> altaAnterior(DSLContext dsl, UUID creador, UUID clave) {
        dsl.fetch("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", "alta-grupo:" + creador);
        return Optional.ofNullable(dsl.fetchOne(
                        "SELECT grupo_id, creador_id, huella_solicitud, fondo_por_periodo FROM grupos.alta_grupo WHERE clave_idempotencia=?",
                        clave))
                .map(f -> new Alta(
                        f.get("grupo_id", UUID.class),
                        f.get("creador_id", UUID.class),
                        f.get("huella_solicitud", String.class),
                        f.get("fondo_por_periodo", String.class)));
    }

    public void guardarAlta(DSLContext dsl, UUID grupo, UUID creador, UUID clave, String huella, String fondo) {
        dsl.execute(
                "INSERT INTO grupos.alta_grupo (grupo_id, creador_id, clave_idempotencia, huella_solicitud, fondo_por_periodo, creada_en) VALUES (?,?,?,?,?,now())",
                grupo,
                creador,
                clave,
                huella,
                fondo);
        // La politica de fila reserva `participante` al sistema: el creador no escribe su propia membresia.
        // El permiso de crear grupos ya se autorizo en la ruta, y la fila es solo del grupo recien creado.
        bo.aportaya.plataforma.datos.Datos.comoSistema(
                dsl,
                () -> dsl.execute(
                        "INSERT INTO grupos.participante (grupo_id, usuario_id, estado, es_organizador, fecha_ingreso, reputacion_al_ingresar, aportes_realizados, aportes_en_mora) VALUES (?,?,'ACEPTADO_PENDIENTE_FIRMA',true,now(),0,0,0)",
                        grupo,
                        creador));
    }

    public record Alta(UUID grupoId, UUID creadorId, String huella, String fondo) {}

    public UUID crear(
            DSLContext dsl,
            GrupoNuevo datos,
            String codigoPublico,
            Optional<UUID> organizador,
            BigDecimal quorum,
            OffsetDateTime ahora) {
        return dsl.insertInto(GRUPO)
                .set(GRUPO.CODIGO_PUBLICO, codigoPublico)
                .set(GRUPO.NOMBRE, datos.nombre())
                .set(GRUPO.MONTO_APORTE, datos.montoDelAporte().monto())
                .set(GRUPO.MONEDA, datos.montoDelAporte().moneda().name())
                .set(GRUPO.PERIODICIDAD, datos.periodicidad())
                .set(GRUPO.DIA_COBRO, (short) datos.diaDeCobro())
                // Tantos periodos como cupos: cada uno cobra una vez, y esa es la
                // definicion entera de un pasanaku.
                .set(GRUPO.NUM_PERIODOS, (short) datos.cupos())
                .set(GRUPO.CUPOS_TOTALES, (short) datos.cupos())
                .set(GRUPO.CUPOS_OCUPADOS, (short) 0)
                .set(GRUPO.FECHA_INICIO, datos.fechaDeInicio())
                .set(GRUPO.FECHA_FIN_ESTIMADA, datos.fechaDeInicio().plusMonths(datos.cupos()))
                .set(GRUPO.ESTADO, "BORRADOR")
                .set(GRUPO.TIPO_CONFORMACION, "MANUAL_POR_INVITACION")
                .set(GRUPO.MODALIDAD_TURNOS, "SORTEO_ALEATORIO")
                .set(GRUPO.VISIBILIDAD, "PRIVADO")
                .set(GRUPO.ORGANIZADOR_ID, organizador.orElse(null))
                .set(GRUPO.ES_AUTOGESTIONADO, organizador.isEmpty())
                .set(GRUPO.REQUIERE_KYC_MINIMO, "BASICO")
                .set(GRUPO.REPUTACION_MINIMA, BigDecimal.ZERO)
                .set(GRUPO.DIAS_GRACIA, (short) 3)
                .set(GRUPO.APLICA_RECARGO_MORA, true)
                .set(GRUPO.USA_FONDO_GARANTIA, false)
                .set(GRUPO.PORCENTAJE_FONDO_GARANTIA, BigDecimal.ZERO)
                .set(GRUPO.QUORUM_DECISIONES, quorum)
                .returning(GRUPO.ID)
                .fetchOne(GRUPO.ID);
    }

    public void configurar(DSLContext dsl, UUID grupoId, boolean permitePermuta) {
        dsl.insertInto(CONFIGURACION_GRUPO)
                .set(CONFIGURACION_GRUPO.GRUPO_ID, grupoId)
                .set(CONFIGURACION_GRUPO.PERMITE_CUPOS_MULTIPLES, false)
                .set(CONFIGURACION_GRUPO.MAX_CUPOS_POR_PERSONA, (short) 1)
                .set(CONFIGURACION_GRUPO.PERMITE_PERMUTA_TURNOS, permitePermuta)
                .set(CONFIGURACION_GRUPO.REQUIERE_AVALISTA, false)
                .set(CONFIGURACION_GRUPO.PERMITE_INGRESO_TARDIO, false)
                .set(CONFIGURACION_GRUPO.HORA_LIMITE_PAGO, java.time.LocalTime.of(23, 59))
                .set(CONFIGURACION_GRUPO.TOLERANCIA_MONTO_PARCIAL, BigDecimal.ZERO)
                .execute();
    }

    /** El reglamento se guarda con su hash: lo que se firma es un texto exacto. */
    public UUID redactarReglamento(
            DSLContext dsl, UUID grupoId, String contenido, String hash, UUID redactadoPor, OffsetDateTime ahora) {
        return dsl.insertInto(REGLAMENTO_GRUPO)
                .set(REGLAMENTO_GRUPO.GRUPO_ID, grupoId)
                .set(REGLAMENTO_GRUPO.VERSION, (short) 1)
                .set(REGLAMENTO_GRUPO.CONTENIDO, contenido)
                .set(REGLAMENTO_GRUPO.HASH_CONTENIDO, hash)
                .set(REGLAMENTO_GRUPO.CLAUSULAS_MORA, "Recargo segun la politica de mora del grupo.")
                .set(REGLAMENTO_GRUPO.CLAUSULAS_ABANDONO, "El retiro se rige por CU-65.")
                .set(REGLAMENTO_GRUPO.VIGENTE_DESDE, ahora)
                .set(REGLAMENTO_GRUPO.REDACTADO_POR, redactadoPor)
                .returning(REGLAMENTO_GRUPO.ID)
                .fetchOne(REGLAMENTO_GRUPO.ID);
    }

    public void abrirCupos(DSLContext dsl, UUID grupoId, int cuantos, OffsetDateTime ahora) {
        for (int numero = 1; numero <= cuantos; numero++) {
            dsl.insertInto(CUPO)
                    .set(CUPO.GRUPO_ID, grupoId)
                    .set(CUPO.NUMERO, (short) numero)
                    .set(CUPO.ESTADO, "LIBRE")
                    .set(CUPO.FRACCION, BigDecimal.ONE)
                    .execute();
        }
    }
}
