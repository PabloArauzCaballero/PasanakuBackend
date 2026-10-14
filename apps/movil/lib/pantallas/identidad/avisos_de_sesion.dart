import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/referencia_de_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../../proveedores/sesion.dart';
import 'textos.dart';

List<Widget> avisosDeSesion({
  required bool cuentaRecienCreada,
  required MotivoDeSalida? motivoDeSalida,
  required String? error,
  required String? trazaId,
}) => [
  if (cuentaRecienCreada && error == null) ...[
    const Alerta(
      tono: Tono.ok,
      titulo: TextosIdentidad.altaListaTitulo,
      detalle: TextosIdentidad.altaListaDetalle,
    ),
    const SizedBox(height: Espacio.s4),
  ],
  if (motivoDeSalida case final motivo?) ...[
    Alerta(
      tono: motivo == MotivoDeSalida.caducada ? Tono.aviso : Tono.info,
      titulo: motivo == MotivoDeSalida.caducada
          ? 'Por seguridad, tu sesión dura 8 horas.'
          : 'Tu sesión se cerró.',
      detalle: 'Volvé a entrar con tu contraseña.',
    ),
    const SizedBox(height: Espacio.s4),
  ],
  if (error != null) ...[
    Alerta(tono: Tono.error, titulo: error),
    ReferenciaDeError(trazaId: trazaId),
    const SizedBox(height: Espacio.s4),
  ],
];

Widget notaDeCustodia(BuildContext context) {
  final t = Tokens.of(context);
  return Row(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Icon(Icons.lock_outline, size: 16, color: t.text3),
      const SizedBox(width: Espacio.s2),
      Expanded(
        child: Text(
          TextosIdentidad.sesionCustodia,
          style: Tipo.ayuda.copyWith(color: t.text3),
        ),
      ),
    ],
  );
}
