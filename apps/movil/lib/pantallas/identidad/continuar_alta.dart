import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Retomar un alta a medias desde el login.
///
/// El servidor guarda el borrador del alta y lo devuelve al confirmar un código del
/// mismo correo (`BorradorOnboardingRepositorio.resolverResultado`); no hay otra forma
/// de pedirlo. Por eso continuar es «el mismo correo + un código»: el alta ya lo
/// restaura al confirmar (`AltaNotifier.confirmarCodigoCorreo`). Lo que faltaba era la
/// puerta: desde el login no había cómo saber que se podía retomar.
abstract final class TextosContinuarAlta {
  static const pregunta = '¿Dejaste tu registro a medias?';
  static const enlace = 'Continuar mi registro';
  static const avisoTitulo = 'Seguís donde lo dejaste';
  static const avisoDetalle =
      'Escribí el mismo correo con el que empezaste. Te mandamos un código y, al '
      'confirmarlo, volvés al paso donde quedaste con lo que ya habías cargado. '
      'La contraseña la elegís de nuevo: por seguridad no la guardamos.';
}

/// La ruta del alta en modo continuar.
const rutaContinuarAlta = '/registro?continuar=1';

/// El enlace del login. Va debajo de «¿Todavía no tenés cuenta?»: es otra salida
/// hacia el alta, para quien ya la empezó.
class SesionContinuarAlta extends StatelessWidget {
  const SesionContinuarAlta({super.key});

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    // `Wrap`, como «¿Todavía no tenés cuenta?»: con letra grande pasa a dos líneas.
    return Wrap(
      alignment: WrapAlignment.center,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        Text(
          TextosContinuarAlta.pregunta,
          style: Tipo.cuerpoChico.copyWith(color: t.text2),
        ),
        TextButton(
          onPressed: () => context.push(rutaContinuarAlta),
          child: const Text(TextosContinuarAlta.enlace),
        ),
      ],
    );
  }
}

/// Lo que se ve arriba del paso 1 cuando se entra a continuar: qué correo usar y por
/// qué hay que volver a elegir la contraseña.
class AvisoDeContinuarAlta extends StatelessWidget {
  const AvisoDeContinuarAlta({super.key});

  @override
  Widget build(BuildContext context) => const Padding(
    padding: EdgeInsets.only(bottom: Espacio.s4),
    child: Alerta(
      tono: Tono.info,
      titulo: TextosContinuarAlta.avisoTitulo,
      detalle: TextosContinuarAlta.avisoDetalle,
    ),
  );
}
