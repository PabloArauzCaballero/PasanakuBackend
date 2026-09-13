import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';

/// «¿Olvidaste tu contraseña?», alineado a la derecha debajo del campo — donde la
/// mano ya está cuando la contraseña no entra.
///
/// **Hueco declarado:** el contrato de identidad no expone ninguna operación de
/// recuperación —el cliente generado tiene autenticar, registrar, buscar por
/// teléfono, token de invitación y verificar titularidad, y nada más—, y el dominio
/// `soporte` todavía no tiene pantallas. Así que esto **no navega**: dice qué hacer
/// ahora mismo. Un enlace que lleva a «ruta no encontrada» justo cuando alguien no
/// puede entrar a su plata es peor que no ofrecerlo.
class SesionOlvide extends StatelessWidget {
  const SesionOlvide({super.key});

  @override
  Widget build(BuildContext context) => Align(
    alignment: Alignment.centerRight,
    child: TextButton(
      onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text(TextosIdentidad.sesionOlvideAyuda)),
      ),
      child: const Text(TextosIdentidad.sesionOlvide),
    ),
  );
}

/// La salida hacia el alta, para quien llegó al login por error. Va en la barra de
/// acciones, debajo del botón: es la segunda opción, no compite con ingresar.
class SesionSinCuenta extends StatelessWidget {
  const SesionSinCuenta({super.key});

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(
          TextosIdentidad.sesionSinCuenta,
          style: Tipo.cuerpoChico.copyWith(color: t.text2),
        ),
        TextButton(
          onPressed: () => context.push('/identidad/registro'),
          child: const Text(TextosIdentidad.portadaCrearCuenta),
        ),
      ],
    );
  }
}
