import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/estado_de_mora.dart';
import 'textos.dart';

/// Estado del aporte y restricción vigente, **solo con hechos guardados**. Ni
/// `EstadoDelParticipante` ni `RestriccionVigente` traen días de mora ni fecha
/// límite por persona (huecos declarados: `planes/informes/carril-M3.md`); esta
/// pantalla nunca inventa un número de días — muestra `alDia`, `deudaVigente` y
/// `montoQueLaLevanta` tal como el backend los guarda. Cumple el gate propio de F5:
/// la mora se comunica en hechos, nunca en probabilidad.
class PantallaMiEstado extends ConsumerWidget {
  const PantallaMiEstado({
    super.key,
    required this.participanteId,
    required this.usuarioId,
  });
  final String participanteId;
  final String usuarioId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(estadoDelParticipanteProvider(participanteId));
    final restriccion = ref.watch(restriccionVigenteProvider(usuarioId));
    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloMora)),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              EstadoDePantalla<EstadoDelParticipante>(
                valor: estado,
                etiquetaDeCarga: TextosPasanaku.cargando,
                mensajeVacio: TextosPasanaku.alDia,
                reintentar: () => ref.invalidate(
                  estadoDelParticipanteProvider(participanteId),
                ),
                exito: (e) => _Aportes(e: e),
              ),
              const SizedBox(height: Espacio.s5),
              EstadoDePantalla(
                valor: restriccion,
                etiquetaDeCarga: TextosPasanaku.cargando,
                mensajeVacio: TextosPasanaku.sinRestriccion,
                reintentar: () =>
                    ref.invalidate(restriccionVigenteProvider(usuarioId)),
                exito: (r) => _Restriccion(r: r),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Aportes extends StatelessWidget {
  const _Aportes({required this.e});
  final EstadoDelParticipante e;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        ChipEstado(
          texto: e.alDia ? 'Al día' : 'Con deuda vigente',
          tono: e.alDia ? Tono.ok : Tono.aviso,
        ),
        const SizedBox(height: Espacio.s3),
        if (!e.alDia) ...[
          Text(
            TextosPasanaku.deudaVigente(e.deudaVigente),
            style: texto.bodyMedium,
          ),
          const SizedBox(height: Espacio.s2),
        ],
        Monto(
          monto: e.porAportar,
          moneda: e.moneda.value,
          etiqueta: TextosPasanaku.porAportarAviso(e.porAportar),
        ),
        const SizedBox(height: Espacio.s2),
        Text(
          'Obligaciones abiertas: ${e.obligacionesAbiertas}',
          style: texto.bodySmall,
        ),
      ],
    );
  }
}

class _Restriccion extends StatelessWidget {
  const _Restriccion({required this.r});
  final dynamic r;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    if (!(r.vigente as bool)) {
      return Text(TextosPasanaku.sinRestriccion, style: texto.bodyMedium);
    }
    return Text(
      TextosPasanaku.restriccionVigente(r.montoQueLaLevanta as String),
      style: texto.bodyMedium,
    );
  }
}
