package bo.aportaya.entregas;

import bo.aportaya.entregas.dominio.puertos.FondosDelComprador;
import bo.aportaya.entregas.dominio.puertos.HechosDeGrupos;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DOBLES de {@code grupos} y {@code nucleo-financiero} para el mercado (regla 65).
 *
 * <p>Ninguno de los dos contratos existe todavia en el servicio real: aca se escribe el contrato
 * propuesto y se ejercita en tres niveles (correcto, limite, invalido, incluida la caida). Lo
 * que queda verificado es la saga de ESTE servicio; nada de lo que pasa aca prueba a los vecinos.
 */
final class DoblesDelMercado {

    private DoblesDelMercado() {}

    static final class Grupos implements HechosDeGrupos {

        private final Map<UUID, Turno> turnos = new HashMap<>();
        private final Map<String, Miembro> miembros = new HashMap<>();
        private boolean caido;

        void seCae(boolean caido) {
            this.caido = caido;
        }

        void turno(Turno turno) {
            turnos.put(turno.turnoId(), turno);
        }

        void miembro(UUID grupoId, UUID usuarioId, UUID participanteId, boolean activo) {
            miembros.put(grupoId + ":" + usuarioId, new Miembro(participanteId, activo));
        }

        @Override
        public synchronized Optional<Turno> turno(UUID turnoId) {
            return caido ? Optional.empty() : Optional.ofNullable(turnos.get(turnoId));
        }

        @Override
        public synchronized Optional<Miembro> miembro(UUID grupoId, UUID usuarioId) {
            return caido ? Optional.empty() : Optional.ofNullable(miembros.get(grupoId + ":" + usuarioId));
        }

        @Override
        public synchronized Optional<Set<UUID>> gruposActivosDe(UUID usuarioId) {
            if (caido) {
                return Optional.empty();
            }
            Set<UUID> grupos = new HashSet<>();
            miembros.forEach((clave, m) -> {
                if (m.activo() && clave.endsWith(":" + usuarioId)) {
                    grupos.add(UUID.fromString(clave.substring(0, clave.indexOf(':'))));
                }
            });
            return Optional.of(grupos);
        }
    }

    /**
     * nucleo: saldos, retenciones y pagos, todo IDEMPOTENTE por clave como el real. Conserva el
     * total del sistema (disponible + retenido + plataforma): ninguna operacion crea ni destruye plata.
     */
    static final class Fondos implements FondosDelComprador {

        private enum Estado {
            RETENIDA,
            EJECUTADA,
            LIBERADA
        }

        private record Retencion(UUID usuario, Dinero monto, Estado estado) {}

        private final Map<UUID, Dinero> disponible = new HashMap<>();
        private final Map<String, Referencia> porClave = new HashMap<>();
        private final Map<UUID, Retencion> retenciones = new HashMap<>();
        private Dinero plataforma = Dinero.cero(Moneda.BOB);
        private boolean caido;
        private int fallasDeRetener;
        private int fallasDePagar;
        final AtomicInteger retencionesNuevas = new AtomicInteger();
        final AtomicInteger pagosNuevos = new AtomicInteger();
        final AtomicInteger liberacionesNuevas = new AtomicInteger();

        void reiniciar() {
            disponible.clear();
            porClave.clear();
            retenciones.clear();
            plataforma = Dinero.cero(Moneda.BOB);
            caido = false;
            fallasDeRetener = 0;
            fallasDePagar = 0;
            retencionesNuevas.set(0);
            pagosNuevos.set(0);
            liberacionesNuevas.set(0);
        }

        synchronized void saldo(UUID usuario, Dinero monto) {
            disponible.put(usuario, monto);
        }

        synchronized void seCae(boolean caido) {
            this.caido = caido;
        }

        /** Las proximas `veces` retenciones devuelven NO_DISPONIBLE (nucleo cae y vuelve). */
        synchronized void fallaAlRetener(int veces) {
            this.fallasDeRetener = veces;
        }

        synchronized void fallaAlPagar(int veces) {
            this.fallasDePagar = veces;
        }

        synchronized Dinero disponible(UUID usuario) {
            return disponible.getOrDefault(usuario, Dinero.cero(Moneda.BOB));
        }

        synchronized Dinero plataforma() {
            return plataforma;
        }

        synchronized Dinero retenidoVivo() {
            return retenciones.values().stream()
                    .filter(r -> r.estado() == Estado.RETENIDA)
                    .map(Retencion::monto)
                    .reduce(Dinero.cero(Moneda.BOB), Dinero::mas);
        }

        synchronized Dinero totalDelSistema() {
            return disponible.values().stream()
                    .reduce(Dinero.cero(Moneda.BOB), Dinero::mas)
                    .mas(retenidoVivo())
                    .mas(plataforma);
        }

        @Override
        public synchronized Referencia retener(UUID compradorUsuarioId, Dinero monto, String clave) {
            var previa = porClave.get(clave);
            if (previa != null) {
                return previa;
            }
            if (caido || fallasDeRetener > 0) {
                if (fallasDeRetener > 0) {
                    fallasDeRetener--;
                }
                return new Referencia(Resultado.NO_DISPONIBLE, null);
            }
            if (disponible(compradorUsuarioId).esMenorQue(monto)) {
                return new Referencia(Resultado.SALDO_INSUFICIENTE, null);
            }
            disponible.put(compradorUsuarioId, disponible(compradorUsuarioId).menos(monto));
            UUID ref = UUID.randomUUID();
            retenciones.put(ref, new Retencion(compradorUsuarioId, monto, Estado.RETENIDA));
            retencionesNuevas.incrementAndGet();
            var r = new Referencia(Resultado.OK, ref);
            porClave.put(clave, r);
            return r;
        }

        @Override
        public synchronized Referencia pagarAlVendedor(
                UUID retencionRef, UUID vendedorUsuarioId, Dinero paraElVendedor, Dinero cargos, String clave) {
            var previa = porClave.get(clave);
            if (previa != null) {
                return previa;
            }
            if (caido || fallasDePagar > 0) {
                if (fallasDePagar > 0) {
                    fallasDePagar--;
                }
                return new Referencia(Resultado.NO_DISPONIBLE, null);
            }
            var r = retenciones.get(retencionRef);
            if (r == null || r.estado() != Estado.RETENIDA || !r.monto().equals(paraElVendedor.mas(cargos))) {
                return new Referencia(Resultado.NO_DISPONIBLE, null);
            }
            retenciones.put(retencionRef, new Retencion(r.usuario(), r.monto(), Estado.EJECUTADA));
            disponible.put(vendedorUsuarioId, disponible(vendedorUsuarioId).mas(paraElVendedor));
            plataforma = plataforma.mas(cargos);
            pagosNuevos.incrementAndGet();
            var ok = new Referencia(Resultado.OK, UUID.randomUUID());
            porClave.put(clave, ok);
            return ok;
        }

        @Override
        public synchronized Resultado liberar(UUID retencionRef, String clave) {
            if (porClave.containsKey(clave)) {
                return Resultado.OK;
            }
            if (caido) {
                return Resultado.NO_DISPONIBLE;
            }
            var r = retenciones.get(retencionRef);
            if (r == null || r.estado() == Estado.EJECUTADA) {
                return Resultado.NO_DISPONIBLE;
            }
            if (r.estado() == Estado.RETENIDA) {
                retenciones.put(retencionRef, new Retencion(r.usuario(), r.monto(), Estado.LIBERADA));
                disponible.put(r.usuario(), disponible(r.usuario()).mas(r.monto()));
                liberacionesNuevas.incrementAndGet();
            }
            porClave.put(clave, new Referencia(Resultado.OK, retencionRef));
            return Resultado.OK;
        }
    }
}
