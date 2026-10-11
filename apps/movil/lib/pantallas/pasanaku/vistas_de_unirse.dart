import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos_invitacion.dart';

// Los estados de «Invitación al grupo» (verificando, falta ingresar, ingresó, falló). Salen
// de `pantalla_unirse.dart`, que conserva el estado y las dos llamadas al servidor; acá no
// hay red ni estado. Las tarjetas del grupo y el reglamento están en `tarjetas_de_unirse.dart`.

/// Mientras se comprueba en el servidor que el teléfono es el destinatario.
class VerificandoInvitacion extends StatelessWidget {
  const VerificandoInvitacion({super.key});

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.all(Espacio.s6),
      child: Column(
        children: [
          const CircularProgressIndicator(),
          const SizedBox(height: Espacio.s3),
          Text(
            TextosInvitacion.verificandoInvitacion,
            style: Tipo.cuerpo.copyWith(color: t.text2),
          ),
        ],
      ),
    );
  }
}

/// Sin sesión: se ofrece ingresar o crear la cuenta y volver a este mismo enlace.
class IngresoRequerido extends StatelessWidget {
  const IngresoRequerido({super.key, required this.retorno});

  /// La ruta a la que se vuelve tras ingresar, ya codificada para la URL.
  final String retorno;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            TextosInvitacion.ingresaParaVerGrupo,
            style: Tipo.cuerpo.copyWith(color: t.text),
          ),
          const SizedBox(height: Espacio.s4),
          Boton(
            texto: TextosInvitacion.ingresar,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => context.go('/ingreso?volver=$retorno'),
          ),
          const SizedBox(height: Espacio.s2),
          Boton(
            texto: TextosInvitacion.crearCuenta,
            variante: BotonVariante.fantasma,
            expandido: true,
            onPressed: () => context.go('/registro?volver=$retorno'),
          ),
        ],
      ),
    );
  }
}

/// La confirmación de que el ingreso quedó hecho.
class IngresoAceptado extends StatelessWidget {
  const IngresoAceptado({super.key});

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(Espacio.s2),
                decoration: BoxDecoration(
                  color: t.okBg,
                  shape: BoxShape.circle,
                ),
                child: Icon(Icons.check_rounded, color: t.okTexto),
              ),
              const SizedBox(width: Espacio.s3),
              Expanded(
                child: Text(
                  TextosInvitacion.yaFormasParte,
                  style: Tipo.titulo3.copyWith(color: t.text),
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s4),
          Boton(
            texto: TextosInvitacion.verMisGrupos,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => context.go('/pasanaku/mi-estado'),
          ),
        ],
      ),
    );
  }
}

/// Lo que salió mal, con la salida a un toque cuando se puede volver a intentar.
class ErrorDeInvitacion extends StatelessWidget {
  const ErrorDeInvitacion({
    super.key,
    required this.mensaje,
    this.alReintentar,
  });
  final String mensaje;

  /// Nulo cuando reintentar no tiene sentido (ya hay datos, o falta ingresar).
  final VoidCallback? alReintentar;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SizedBox(height: Espacio.s4),
        Alerta(tono: Tono.error, titulo: mensaje),
        if (alReintentar != null) ...[
          const SizedBox(height: Espacio.s3),
          Boton(
            texto: TextosInvitacion.reintentar,
            variante: BotonVariante.fantasma,
            expandido: true,
            onPressed: alReintentar,
          ),
        ],
      ],
    );
  }
}
