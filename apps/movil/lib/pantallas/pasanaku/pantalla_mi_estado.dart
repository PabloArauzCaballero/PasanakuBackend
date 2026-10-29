import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'acciones_de_grupo.dart';
import 'pantalla_mis_grupos.dart';
import 'tarjeta_de_aporte.dart';
import 'dominio/estado_de_mora.dart';
import 'textos.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';

/// Estado del aporte y restricción vigente, **solo con hechos guardados**. Ni
/// `EstadoDelParticipante` ni `RestriccionVigente` traen días de mora ni fecha
/// límite por persona (huecos declarados: `planes/informes/carril-M3.md`); esta
/// pantalla nunca inventa un número de días — muestra `alDia`, `deudaVigente` y
/// `montoQueLaLevanta` tal como el backend los guarda. Cumple el gate propio de F5:
/// la mora se comunica en hechos, nunca en probabilidad.
/// El importe con espacio sin corte entre la moneda y la cifra: en una frase, «Bs» no puede
/// quedar solo al final de una línea y «4,44» en la siguiente.
String _importe(String monto, String moneda) =>
    formatearMonto(monto: monto, moneda: moneda).replaceAll(' ', '\u00A0');

class PantallaMiEstado extends ConsumerWidget {
  const PantallaMiEstado({
    super.key,
    required this.participanteId,
    required this.usuarioId,
    this.grupoId,
    this.nombreDelGrupo,
  });
  final String participanteId;
  final String usuarioId;
  final String? grupoId;

  /// El nombre del grupo, para que el título diga de qué grupo es este estado.
  final String? nombreDelGrupo;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(estadoDelParticipanteProvider(participanteId));
    final restriccion = ref.watch(restriccionVigenteProvider(usuarioId));
    final nombre = nombreDelGrupo;
    return Scaffold(
      appBar: AppBar(
        title: Text(
          nombre == null || nombre.isEmpty ? TextosPasanaku.tituloMora : nombre,
        ),
      ),
      body: SafeArea(
        // Deslizar hacia abajo vuelve a pedir el estado del aporte y la restricción.
        child: RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(estadoDelParticipanteProvider(participanteId));
            ref.invalidate(restriccionVigenteProvider(usuarioId));
            await ref
                .read(estadoDelParticipanteProvider(participanteId).future)
                .then((_) {}, onError: (Object _) {});
          },
          child: SingleChildScrollView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(Espacio.s4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                MarcaDeTutorial(
                  id: 'pasanaku.mi-estado',
                  hijo: EstadoDePantalla<EstadoDelParticipante>(
                    valor: estado,
                    etiquetaDeCarga: TextosPasanaku.cargando,
                    mensajeVacio: TextosPasanaku.alDia,
                    reintentar: () => ref.invalidate(
                      estadoDelParticipanteProvider(participanteId),
                    ),
                    exito: (e) => TarjetaDeAporte(e: e),
                  ),
                ),
                const SizedBox(height: Espacio.s5),
                EstadoDePantalla(
                  valor: restriccion,
                  etiquetaDeCarga: TextosPasanaku.cargando,
                  mensajeVacio: TextosPasanaku.sinRestriccion,
                  reintentar: () =>
                      ref.invalidate(restriccionVigenteProvider(usuarioId)),
                  exito: (r) => AvisoDeRestriccion(
                    vigente: r.vigente,
                    // La restricción no trae moneda y la app opera solo en bolivianos
                    // (`Dinero`: BOB).
                    montoQueLaLevanta: _importe(r.montoQueLaLevanta, 'BOB'),
                  ),
                ),
                const SizedBox(height: Espacio.s5),
                AccionesDeGrupo(grupoId: grupoId),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// La pestaña «Grupos». Con `participante` y `usuario` en la ruta (al tocar una tarjeta o
/// desde un aviso) abre el estado de ESE grupo; sin ellos muestra la lista de mis grupos,
/// una tarjeta por grupo (B36: antes pedía ids vacíos y mostraba solo el primero).
class PantallaMiEstadoDeLaSesion extends StatelessWidget {
  const PantallaMiEstadoDeLaSesion({
    super.key,
    this.participanteId,
    this.usuarioId,
    this.grupoId,
    this.nombreDelGrupo,
  });
  final String? participanteId;
  final String? usuarioId;
  final String? grupoId;
  final String? nombreDelGrupo;

  @override
  Widget build(BuildContext context) {
    final p = participanteId;
    final u = usuarioId;
    if (p != null && p.isNotEmpty && u != null && u.isNotEmpty) {
      return PantallaMiEstado(
        participanteId: p,
        usuarioId: u,
        grupoId: grupoId,
        nombreDelGrupo: nombreDelGrupo,
      );
    }
    return const PantallaMisGrupos();
  }
}
