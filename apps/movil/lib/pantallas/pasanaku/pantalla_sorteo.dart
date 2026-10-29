import 'dart:async';
import 'dart:math' as math;

import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/errores.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../proveedores/haptica.dart';
import 'anuncio_del_sorteo.dart';
import 'dominio/detalle_del_grupo.dart';
import 'orden_de_cobro.dart';
import 'orquesta_del_sorteo.dart';
import 'ruleta_del_sorteo.dart';
import 'textos_detalle_del_grupo.dart';

part 'pantalla_sorteo_orden.dart';
part 'pantalla_sorteo_vista.dart';

/// El sorteo de turnos (CU-60). El organizador toca «Girar»: el servidor compromete y
/// revela, y la ruleta pone en escena el resultado persona por persona — amague, giro
/// que frena, aguja que golpea las clavijas, el gajo que brilla y su cartel. Con el
/// sorteo ya hecho, cualquiera ve acá el orden de cobro. Con movimiento reducido no
/// gira: el orden aparece entero.
class PantallaSorteo extends ConsumerStatefulWidget {
  const PantallaSorteo({super.key, required this.grupoId});
  final String grupoId;

  @override
  ConsumerState<PantallaSorteo> createState() => _PantallaSorteoState();
}

class _PantallaSorteoState extends ConsumerState<PantallaSorteo>
    with SingleTickerProviderStateMixin {
  late final OrquestaDelSorteo _orquesta = OrquestaDelSorteo(this, alTic: _tic);
  bool _sorteando = false;
  bool _ceremonia = false;
  int? _enJuego;
  String? _error;
  final List<String> _revelados = [];

  /// Quienes todavía no salieron: los únicos gajos de la ruleta durante la ceremonia.
  final List<String> _restantes = [];

  void _tic() => unawaited(
    ref.read(hapticaProvider).seleccion().catchError((Object _) {}),
  );

  @override
  void dispose() {
    _orquesta.dispose();
    super.dispose();
  }

  Future<void> _girar(DetalleDelGrupo d) async {
    if (_sorteando) return;
    final quieto = MediaQuery.of(context).disableAnimations;
    setState(() {
      _sorteando = true;
      _ceremonia = !quieto;
      _error = null;
      _revelados.clear();
    });
    try {
      final sorteo = d.sorteo;
      final orden = await ref.read(sortearTurnosProvider)(
        widget.grupoId,
        sorteo?.estado == SorteoDelDetalleEstadoEnum.COMPROMETIDO
            ? sorteo!.sorteoId
            : null,
      );
      final yo = d.integrantes.where((i) => i.soyYo).map((i) => i.cupoId);
      setState(() {
        _restantes
          ..clear()
          ..addAll(d.integrantes.map((i) => i.cupoId).whereType<String>());
      });
      for (var k = 0; k < orden.length && mounted; k++) {
        if (!quieto) {
          setState(() => _enJuego = k + 1);
          final gajo = _restantes.indexOf(orden[k]);
          if (_restantes.length == 1) {
            // Queda una sola persona: el último turno es suyo, sin girar.
            await _orquesta.senalar(0);
          } else if (gajo >= 0) {
            await _orquesta.girarHasta(
              gajo,
              _restantes.length,
              primera: k == 0,
            );
          }
        }
        if (!mounted) return;
        setState(() => _revelados.add(orden[k]));
        if (!quieto) {
          if (yo.contains(orden[k])) {
            unawaited(
              ref.read(hapticaProvider).exito().catchError((Object _) {}),
            );
          }
          await _orquesta.pausa(
            Duration(milliseconds: math.min(900, 1400 - 60 * k)),
          );
          // Quien salió se va de la ruleta: su gajo se angosta y los demás lo cubren.
          await _orquesta.retirar();
          if (!mounted) return;
          setState(() {
            _orquesta.olvidarGanador();
            _restantes.remove(orden[k]);
          });
        }
      }
      if (!quieto && mounted) {
        setState(() => _enJuego = null);
        await _orquesta.celebrar();
      }
      ref.invalidate(detalleDelGrupoProvider(widget.grupoId));
    } on Object catch (e) {
      if (mounted) {
        setState(
          () => _error = e is ErrorPresentable
              ? e.mensaje
              : 'No pudimos hacer el sorteo. Probá de nuevo.',
        );
      }
    } finally {
      if (mounted) setState(() => _sorteando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final detalle = ref.watch(detalleDelGrupoProvider(widget.grupoId));
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          children: [
            const CabeceraDeSeccion(titulo: TextosDetalleDelGrupo.tituloSorteo),
            EstadoDePantalla<DetalleDelGrupo>(
              valor: detalle,
              mensajeVacio: '',
              reintentar: () =>
                  ref.invalidate(detalleDelGrupoProvider(widget.grupoId)),
              exito: (d) => _contenido(context, d),
            ),
          ],
        ),
      ),
    );
  }
}
