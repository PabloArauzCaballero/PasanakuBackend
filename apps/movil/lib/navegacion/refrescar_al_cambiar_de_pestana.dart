import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/cu13_consultar_saldo.dart';
import '../pantallas/identidad/dominio/foto_de_perfil.dart';
import '../pantallas/pasanaku/dominio/detalle_del_grupo.dart';
import '../pantallas/pasanaku/dominio/estado_de_mora.dart';
import '../pantallas/pasanaku/dominio/mis_grupos.dart';

/// Lo que hay que hacer al tocar una pestaña (2026-10-30).
///
/// El shell es un `indexedStack`: la rama que se deja **sigue montada** por detrás.
/// Eso tiene dos efectos que Pablo vio en el iPhone:
///
/// - el campo de monto de «Aportar» conservaba el foco y el teclado quedaba abierto
///   encima del Inicio;
/// - los `autoDispose` nunca se descartaban porque la pantalla de atrás los seguía
///   escuchando, así que firmar o aportar en «Grupos» no cambiaba nada en «Inicio».
///
/// Se suelta el foco y se vuelven a pedir los datos que se ven en más de una pestaña.
/// Riverpod conserva el valor anterior mientras recarga: no hay parpadeo de «cargando».
void refrescarAlCambiarDePestana(WidgetRef ref) {
  FocusManager.instance.primaryFocus?.unfocus();
  ref
    ..invalidate(saldoProvider)
    ..invalidate(movimientosBilleteraProvider)
    ..invalidate(misGruposProvider)
    ..invalidate(detalleDelGrupoProvider)
    ..invalidate(estadoDelParticipanteProvider)
    ..invalidate(fotoDePerfilProvider);
}
