import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../proveedores/proteccion_pantalla.dart';
import 'estado_alta.dart';

/// Ciclo de privacidad y de navegación al error para la pantalla del alta.
mixin ComportamientoDePantallaDelAlta<T extends ConsumerStatefulWidget>
    on ConsumerState<T> {
  final alertaErrorKey = GlobalKey();
  late final _proteccion = ref.read(proteccionPantallaProvider);
  Future<void> _colaDeProteccion = Future<void>.value();
  var _protegida = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _actualizarProteccion(ref.read(altaProvider).paso.fase);
    });
  }

  void alCambiarEstadoDelAlta(EstadoAlta? anterior, EstadoAlta siguiente) {
    if (anterior?.paso.fase != siguiente.paso.fase) {
      _actualizarProteccion(siguiente.paso.fase);
    }
    if (anterior?.error != siguiente.error ||
        anterior?.errorCodigo != siguiente.errorCodigo ||
        anterior?.trazaId != siguiente.trazaId) {
      if (siguiente.error != null || siguiente.errorCodigo != null) {
        _desplazarAlError();
      }
    }
  }

  void _actualizarProteccion(FaseAlta fase) {
    final proteger = fase == FaseAlta.cuenta || fase == FaseAlta.identidad;
    if (proteger == _protegida) return;
    _protegida = proteger;
    _colaDeProteccion = _colaDeProteccion.then((_) async {
      if (proteger) {
        await _proteccion.activar();
      } else {
        await _proteccion.desactivar();
      }
    });
  }

  void _desplazarAlError() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final contexto = alertaErrorKey.currentContext;
      if (mounted && contexto != null) {
        Scrollable.ensureVisible(
          contexto,
          alignment: 0.08,
          duration: Movimiento.entrada,
          curve: Curves.easeOutCubic,
        );
      }
    });
  }

  @override
  void dispose() {
    if (_protegida) {
      _protegida = false;
      _colaDeProteccion = _colaDeProteccion.then(
        (_) => _proteccion.desactivar(),
      );
    }
    super.dispose();
  }
}
