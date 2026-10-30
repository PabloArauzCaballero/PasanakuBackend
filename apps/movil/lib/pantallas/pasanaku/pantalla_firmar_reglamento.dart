import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/errores.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moviles/mostrar_snackbar.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/detalle_del_grupo.dart';
import 'dominio/mis_grupos.dart';
import 'tarjetas_de_unirse.dart';
import 'textos_detalle_del_grupo.dart';

/// Firmar el reglamento del grupo (CU-69, 2026-10-20). Se lee el texto exacto que
/// devuelve el servidor y se firma ese texto, por su hash: si cambió entre que se leyó
/// y se firmó, el servidor no lo acepta (AP-CU69-05). Firmar ocupa el cupo reservado.
class PantallaFirmarReglamento extends ConsumerStatefulWidget {
  const PantallaFirmarReglamento({super.key, required this.grupoId});
  final String grupoId;

  @override
  ConsumerState<PantallaFirmarReglamento> createState() =>
      _PantallaFirmarReglamentoState();
}

class _PantallaFirmarReglamentoState
    extends ConsumerState<PantallaFirmarReglamento> {
  bool _acepta = false;
  bool _firmando = false;
  String? _error;

  Future<void> _firmar(String hash) async {
    if (_firmando || !_acepta) return;
    setState(() {
      _firmando = true;
      _error = null;
    });
    try {
      final completo = await ref.read(firmarReglamentoProvider)(
        widget.grupoId,
        hash,
      );
      ref.invalidate(detalleDelGrupoProvider(widget.grupoId));
      // «Tus pasanakus» y «Mis grupos» muestran «Falta firmar»: también cambian.
      ref.invalidate(misGruposProvider);
      if (!mounted) return;
      mostrarSnackbar(
        context,
        completo
            ? TextosDetalleDelGrupo.firmadoYCompleto
            : TextosDetalleDelGrupo.firmado,
      );
      if (context.canPop()) context.pop();
    } on Object catch (e) {
      if (mounted) {
        setState(
          () => _error = e is ErrorPresentable
              ? e.mensaje
              : 'No pudimos guardar tu firma. Probá de nuevo.',
        );
      }
    } finally {
      if (mounted) setState(() => _firmando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final detalle = ref.watch(detalleDelGrupoProvider(widget.grupoId));
    final error = _error;
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          children: [
            const CabeceraDeSeccion(titulo: TextosDetalleDelGrupo.tituloFirma),
            EstadoDePantalla<DetalleDelGrupo>(
              valor: detalle,
              mensajeVacio: '',
              reintentar: () =>
                  ref.invalidate(detalleDelGrupoProvider(widget.grupoId)),
              exito: (d) {
                final reglamento = d.reglamento;
                if (reglamento == null) return const SizedBox.shrink();
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    ReglamentoDelGrupo(
                      reglamento: reglamento.texto,
                      acepta: _acepta,
                      alCambiar: (v) => setState(() => _acepta = v),
                    ),
                    const SizedBox(height: Espacio.s3),
                    if (error != null) ...[
                      Alerta(titulo: error, tono: Tono.error),
                      const SizedBox(height: Espacio.s3),
                    ],
                    Boton(
                      key: const ValueKey('confirmar-firma'),
                      texto: TextosDetalleDelGrupo.firmar,
                      variante: BotonVariante.primario,
                      expandido: true,
                      cargando: _firmando,
                      onPressed: _acepta && d.debeFirmar
                          ? () => _firmar(reglamento.hash)
                          : null,
                    ),
                    const SizedBox(height: Espacio.s5),
                  ],
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
