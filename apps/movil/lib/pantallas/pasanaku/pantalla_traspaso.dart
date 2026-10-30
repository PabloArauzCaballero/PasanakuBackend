import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/barra_de_pasos.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'accion_del_traspaso.dart';
import 'comprobante_del_traspaso.dart';
import 'dominio/cu64_traspasar_cupo.dart';
import 'dominio/detalle_del_grupo.dart';
import 'dominio/votacion_del_traspaso.dart';
import 'errores_del_traspaso.dart';
import 'pasos_del_traspaso.dart';
import 'textos_del_traspaso.dart';

part 'pantalla_traspaso_envio.dart';

/// CU-64 · traspasar mi cupo, en tres pasos y una hoja de confirmación. Con la forma de
/// una orden P2P (pasos, tarjeta de la orden, contraparte, condiciones, acción grande
/// abajo, comprobante) y sin nada de su negocio: no hay precio, ni pago, ni plazo.
class PantallaTraspaso extends ConsumerStatefulWidget {
  const PantallaTraspaso({
    super.key,
    required this.grupoId,
    required this.cupoId,
    this.acuerdoId,
  });
  final String grupoId;
  final String cupoId;

  /// El acuerdo aprobado que autoriza el traspaso, si quien abre la pantalla lo tiene.
  final String? acuerdoId;

  @override
  ConsumerState<PantallaTraspaso> createState() => _PantallaTraspasoState();
}

const _misGrupos = '/pasanaku';

class _PantallaTraspasoState extends ConsumerState<PantallaTraspaso>
    with _EnvioDelTraspaso {
  var _paso = 0;
  String? _elegido;

  /// Volver retrocede un paso; desde el primero (o con el traspaso hecho) sale.
  void _volver() {
    if (ref.read(traspasarCupoProvider).value != null) {
      context.go(_misGrupos);
    } else if (_paso > 0) {
      setState(() => _paso--);
    } else if (context.canPop()) {
      context.pop();
    } else {
      context.go(_misGrupos);
    }
  }

  @override
  Widget build(BuildContext context) {
    final detalle = ref.watch(detalleDelGrupoProvider(widget.grupoId));
    final d = detalle.value;
    final mio = d?.integrantes
        .where((i) => i.cupoId == widget.cupoId && i.soyYo)
        .firstOrNull;
    final candidatos =
        d?.integrantes.where((i) => !i.soyYo && i.usuarioId != null).toList() ??
        const <IntegranteDelGrupo>[];
    final entrante = candidatos
        .where((c) => c.usuarioId == _elegido)
        .firstOrNull;
    final votacion = widget.acuerdoId != null
        ? VotacionAprobada(widget.acuerdoId!)
        : ref.watch(votacionDelCupoProvider(_cupo)).value;
    final pidiendo = ref.watch(pedirVotacionProvider).isLoading;
    final envio = ref.watch(traspasarCupoProvider);
    final salida = envio.value;
    final listo = salida != null && mio != null && entrante != null;
    return Scaffold(
      body: PantallaConCabecera(
        titulo: TextosDelTraspaso.titulo,
        alVolver: _volver,
        cuerpo: SafeArea(
          top: false,
          bottom: false,
          // Cada paso (y el comprobante) arranca arriba, no donde quedó el anterior.
          child: SingleChildScrollView(
            key: ValueKey(listo ? 'comprobante' : 'paso-$_paso'),
            padding: const EdgeInsets.all(Espacio.s4),
            child: EstadoDePantalla<DetalleDelGrupo>(
              valor: detalle,
              mensajeVacio: '',
              reintentar: () =>
                  ref.invalidate(detalleDelGrupoProvider(widget.grupoId)),
              exito: (d) {
                if (mio == null) {
                  return const EstadoVacio(
                    titulo: TextosDelTraspaso.sinCupoTitulo,
                    mensaje: TextosDelTraspaso.sinCupoDetalle,
                    icono: IconosDeMarca.grupos,
                  );
                }
                final resumen = entrante == null
                    ? null
                    : ResumenDelTraspaso(
                        mio: mio,
                        entrante: entrante,
                        motivo: _motivo.text.trim(),
                      );
                if (listo) {
                  return ComprobanteDelTraspaso(
                    salida: salida,
                    resumen: resumen!,
                    nombreEntrante:
                        entrante.nombre ?? TextosDelTraspaso.sinNombre,
                  );
                }
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    BarraDePasos(
                      total: TextosDelTraspaso.pasos.length,
                      actual: _paso + 1,
                      nombre: TextosDelTraspaso.pasos[_paso],
                    ),
                    const SizedBox(height: Espacio.s5),
                    switch (_paso) {
                      0 => PasoElegir(
                        candidatos: candidatos,
                        elegido: _elegido,
                        alElegir: (id) => setState(() => _elegido = id),
                      ),
                      1 => PasoMotivo(
                        controlador: _motivo,
                        alCambiar: (_) => setState(() {}),
                      ),
                      _ => PasoRevisar(
                        mio: mio,
                        entrante: entrante!,
                        nombreDelGrupo: d.nombre,
                        hayAcuerdo: votacion is VotacionAprobada,
                        detalleAcuerdo: _detalleDe(votacion),
                        rechazo: envio.hasError
                            ? rechazoDelTraspaso(envio.error!)
                            : null,
                      ),
                    },
                  ],
                );
              },
            ),
          ),
        ),
      ),
      bottomNavigationBar: d == null || mio == null
          ? null
          : AccionDelTraspaso(
              paso: _paso,
              hayElegido: entrante != null,
              largoDelMotivo: _motivo.text.trim().length,
              enviando: envio.isLoading,
              confirmando: _confirmando,
              listo: listo,
              alContinuar: () => setState(() => _paso++),
              alTraspasar: () => _confirmarYEnviar(mio, entrante!),
              alTerminar: () => context.go(_misGrupos),
              votacion: votacion,
              pidiendo: pidiendo,
              alPedirVotacion: () => _pedirVotacion(mio, entrante),
            ),
    );
  }
}
