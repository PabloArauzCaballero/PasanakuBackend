import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/capturas_del_expediente.dart';
import 'dominio/seguimiento_del_alta.dart';
import 'dominio/subida_del_expediente.dart';
import 'fila_de_subida.dart';
import 'textos_de_subida.dart';

/// La subida en lote de las cinco fotos ya validadas, con la misma máquina de
/// estados de Atlas: "Subiendo…", "está tardando" a los 10 s, y "Reintentar" o
/// "Repetir la foto" ante un fallo.
class PantallaDeSubidaDelExpediente extends ConsumerStatefulWidget {
  const PantallaDeSubidaDelExpediente({super.key, required this.usuarioId});
  final String usuarioId;

  @override
  ConsumerState<PantallaDeSubidaDelExpediente> createState() =>
      _PantallaDeSubidaDelExpedienteState();
}

class _PantallaDeSubidaDelExpedienteState
    extends ConsumerState<PantallaDeSubidaDelExpediente> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _subir());
  }

  Future<void> _subir() async {
    final capturas = _capturas(ref.read(seguimientoDelAltaProvider));
    await ref
        .read(subidaProvider.notifier)
        .subirPendientes(usuarioId: widget.usuarioId, capturas: capturas);
  }

  Future<void> _repetir(CaraDelCarril cara) async {
    ref.read(subidaProvider.notifier).rotarClaveParaRepetir(cara);
    final nueva = await context.push<Object?>('/registro/camara', extra: cara);
    if (!mounted || nueva is! Captura) return;
    ref
        .read(seguimientoDelAltaProvider.notifier)
        .reemplazarCaptura(cara, nueva);
    await ref
        .read(subidaProvider.notifier)
        .reintentar(usuarioId: widget.usuarioId, cara: cara, ruta: nueva.ruta);
  }

  @override
  Widget build(BuildContext context) {
    final subida = ref.watch(subidaProvider);
    final capturas = _capturas(ref.watch(seguimientoDelAltaProvider));
    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        title: Text(
          subida.completa
              ? TextosDeSubida.tituloEnviados
              : TextosDeSubida.tituloSubiendo,
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: ListView(
                  children: [
                    for (final cara in CapturasDelExpediente.orden)
                      FilaDeSubida(
                        cara: cara,
                        estado: subida.porCara[cara],
                        ruta: capturas.porCara[cara]?.ruta,
                        onCancelar: () =>
                            ref.read(subidaProvider.notifier).cancelar(cara),
                        onReintentar: () => ref
                            .read(subidaProvider.notifier)
                            .reintentar(
                              usuarioId: widget.usuarioId,
                              cara: cara,
                              ruta: capturas.porCara[cara]!.ruta,
                            ),
                        onRepetir: () => _repetir(cara),
                      ),
                  ],
                ),
              ),
              // Una foto que no sube no frena el alta (`subida_del_expediente.dart`
              // original): la cuenta ya existe y el backoffice ve el expediente
              // incompleto, que es recuperable. "Continuar" solo espera a que no
              // quede ninguna subida EN CURSO, no a que las cinco hayan salido bien.
              Boton(
                texto: TextosDeSubida.continuar,
                variante: BotonVariante.primario,
                expandido: true,
                onPressed: subida.subiendoAlguna
                    ? null
                    : () => context.go(
                        '/registro/estado?usuario=${widget.usuarioId}',
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Sin seguimiento (p. ej. la app se reabrió en esta ruta) no hay nada que subir.
CapturasDelExpediente _capturas(AltaEnSeguimiento? seguimiento) =>
    seguimiento?.capturas ?? const CapturasDelExpediente();
