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

import 'dominio/detalle_del_grupo.dart';
import 'orden_de_cobro.dart';
import 'ruleta_del_sorteo.dart';
import 'textos_detalle_del_grupo.dart';

/// El sorteo de turnos (CU-60, 2026-10-20). El organizador toca «Girar»: el servidor
/// compromete y revela, y la ruleta muestra el resultado persona por persona. Con el
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
  late final AnimationController _giro;
  double _angulo = 0;
  bool _sorteando = false;
  String? _error;
  final List<String> _revelados = [];

  @override
  void initState() {
    super.initState();
    _giro = AnimationController(vsync: this);
  }

  @override
  void dispose() {
    _giro.dispose();
    super.dispose();
  }

  Future<void> _girar(DetalleDelGrupo d) async {
    if (_sorteando) return;
    final quieto = MediaQuery.of(context).disableAnimations;
    setState(() {
      _sorteando = true;
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
      final cupos = d.integrantes.map((i) => i.cupoId).toList();
      for (var k = 0; k < orden.length && mounted; k++) {
        if (!quieto) {
          await _girarHasta(cupos.indexOf(orden[k]), cupos.length, k);
        }
        if (mounted) setState(() => _revelados.add(orden[k]));
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

  /// La primera vuelta es larga, para que se vea que gira; las demás, cortas.
  Future<void> _girarHasta(int indice, int segmentos, int paso) async {
    if (indice < 0) return;
    final desde = _angulo;
    final hasta = RuletaDelSorteo.anguloPara(
      indice,
      segmentos,
      desde: desde,
      vueltas: paso == 0 ? 4 : 1,
    );
    final curva = CurvedAnimation(parent: _giro, curve: Curves.easeOutCubic);
    void mover() =>
        setState(() => _angulo = desde + (hasta - desde) * curva.value);
    _giro.duration = Duration(milliseconds: paso == 0 ? 2600 : 900);
    _giro.addListener(mover);
    await _giro.forward(from: 0);
    _giro.removeListener(mover);
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
            const CabeceraDeSeccion(titulo: TextosDetalleDelGrupo.tituloSorteo),
            EstadoDePantalla<DetalleDelGrupo>(
              valor: detalle,
              mensajeVacio: '',
              reintentar: () =>
                  ref.invalidate(detalleDelGrupoProvider(widget.grupoId)),
              exito: (d) => _contenido(context, d, error),
            ),
          ],
        ),
      ),
    );
  }

  Widget _contenido(BuildContext context, DetalleDelGrupo d, String? error) {
    final t = Tokens.of(context);
    final nombres = {
      for (final i in d.integrantes)
        i.cupoId: i.nombre ?? TextosDetalleDelGrupo.sinNombre,
    };
    final yo = d.integrantes
        .where((i) => i.soyYo)
        .map((i) => i.cupoId)
        .firstOrNull;
    // Sorteo ya revelado: el orden sale de los turnos del detalle.
    final yaSorteado = [...d.integrantes.where((i) => i.turno != null)]
      ..sort((a, b) => a.turno!.compareTo(b.turno!));
    final orden = _revelados.isNotEmpty || _sorteando
        ? _revelados
        : yaSorteado.map((i) => i.cupoId).toList();
    final puedeGirar =
        d.puedeSortear ||
        d.sorteo?.estado == SorteoDelDetalleEstadoEnum.COMPROMETIDO;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          TextosDetalleDelGrupo.sorteoAyuda,
          style: Tipo.cuerpoChico.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s4),
        Center(
          child: RuletaDelSorteo(
            nombres: [for (final i in d.integrantes) nombres[i.cupoId]!],
            angulo: _angulo,
          ),
        ),
        const SizedBox(height: Espacio.s4),
        if (error != null) ...[
          Alerta(titulo: error, tono: Tono.error),
          const SizedBox(height: Espacio.s3),
        ],
        if (puedeGirar && orden.isEmpty)
          Boton(
            key: const ValueKey('girar-ruleta'),
            texto: _sorteando
                ? TextosDetalleDelGrupo.girando
                : TextosDetalleDelGrupo.girar,
            variante: BotonVariante.primario,
            expandido: true,
            cargando: _sorteando,
            onPressed: () => _girar(d),
          ),
        if (orden.isNotEmpty) ...[
          OrdenDeCobro(
            puestos: [
              for (final cupo in orden)
                (
                  nombre: nombres[cupo] ?? TextosDetalleDelGrupo.sinNombre,
                  soyYo: yo == cupo,
                ),
            ],
          ),
          if (!_sorteando) ...[
            const SizedBox(height: Espacio.s4),
            Boton(
              texto: TextosDetalleDelGrupo.listo,
              variante: BotonVariante.secundario,
              expandido: true,
              onPressed: () => context.canPop() ? context.pop() : null,
            ),
          ],
        ],
        const SizedBox(height: Espacio.s5),
      ],
    );
  }
}
