import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_contrasena.dart';
import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'aviso_alta.dart';
import 'dominio/estado_sesion.dart';
import 'dominio/validaciones.dart';
import 'sesion_pie.dart';
import 'textos.dart';
import '../../navegacion/retorno_de_invitacion.dart';

/// CU-04: teléfono y contraseña; el servidor decide si pide MFA (invariante 7).
/// Primera pantalla que solicita datos: marca y saludo dan contexto de confianza.
class PantallaDeSesion extends ConsumerStatefulWidget {
  const PantallaDeSesion({
    super.key,
    this.cuentaRecienCreada = false,
    this.fotosPendientes = false,
  });

  /// Se llega acá desde el último paso del alta. Sin decirlo, el salto del contrato
  /// aceptado a un formulario de acceso se lee como que algo falló: se completaron
  /// ocho pasos y la app pide entrar de nuevo, sin explicar que la cuenta quedó hecha.
  final bool cuentaRecienCreada;
  final bool fotosPendientes;

  @override
  ConsumerState<PantallaDeSesion> createState() => _PantallaDeSesionState();
}

class _PantallaDeSesionState extends ConsumerState<PantallaDeSesion> {
  final _telefono = TextEditingController();
  final _contrasena = TextEditingController();
  final _focoContrasena = FocusNode();

  @override
  void dispose() {
    _telefono.dispose();
    _contrasena.dispose();
    _focoContrasena.dispose();
    super.dispose();
  }

  /// Validación local mínima; la decisión de autenticación siempre es del servidor.
  bool get _valido =>
      errorTelefono(_telefono.text) == null && _contrasena.text.length >= 8;

  Future<void> _ingresar() async {
    if (!_valido || ref.read(sesionIdentidadProvider).enviando) return;
    final n = ref.read(sesionIdentidadProvider.notifier);
    n.actualizarCredenciales(
      telefono: _telefono.text.trim(),
      contrasena: _contrasena.text,
    );
    final avanzo = await n.enviarCredenciales();
    if (!avanzo || !mounted) return;
    // A dónde ir lo dijo el servidor, no esta pantalla.
    final paso = ref.read(sesionIdentidadProvider).paso;
    final retorno = retornoDeInvitacion(
      GoRouterState.of(context).uri.queryParameters['volver'],
    );
    if (paso == PasoSesion.mfa) {
      context.push(
        retorno == null
            ? '/identidad/mfa'
            : '/identidad/mfa?volver=${Uri.encodeComponent(retorno)}',
      );
    } else {
      context.go(retorno ?? '/billetera/inicio');
    }
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(sesionIdentidadProvider);
    final t = Tokens.of(context);
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Column(
          children: [
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: Espacio.s4),
              child: CabeceraDeSeccion(titulo: ''),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(
                  Espacio.s4,
                  0,
                  Espacio.s4,
                  Espacio.s4,
                ),
                children: [
                  AparicionEscalonada(
                    children: [
                      const Marca(tamano: 52),
                      const SizedBox(height: Espacio.s4),
                      Text(
                        TextosIdentidad.sesionSaludo,
                        style: Tipo.cuerpo.copyWith(color: t.text2),
                      ),
                      Semantics(
                        header: true,
                        child: Text(
                          TextosIdentidad.sesionTitular,
                          style: Tipo.titulo1.copyWith(color: t.text),
                        ),
                      ),
                      const SizedBox(height: Espacio.s5),
                      if (widget.cuentaRecienCreada &&
                          estado.error == null) ...[
                        AvisoDeAlta(fotosPendientes: widget.fotosPendientes),
                        const SizedBox(height: Espacio.s4),
                      ],
                      if (estado.error != null) ...[
                        Alerta(tono: Tono.error, titulo: estado.error!),
                        const SizedBox(height: Espacio.s4),
                      ],
                      Campo(
                        etiqueta: TextosIdentidad.telefono,
                        icono: Icons.phone_iphone,
                        ayuda: TextosIdentidad.sesionCelularAyuda,
                        controlador: _telefono,
                        tipoDeTeclado: TextInputType.phone,
                        accionDeTeclado: TextInputAction.next,
                        onSubmitted: (_) => _focoContrasena.requestFocus(),
                        onChanged: (_) => setState(() {}),
                      ),
                      const SizedBox(height: Espacio.s4),
                      CampoContrasena(
                        etiqueta: TextosIdentidad.contrasena,
                        controlador: _contrasena,
                        foco: _focoContrasena,
                        accionDeTeclado: TextInputAction.done,
                        onSubmitted: (_) => _ingresar(),
                        onChanged: (_) => setState(() {}),
                      ),
                      const SizedBox(height: Espacio.s2),
                      const SesionOlvide(),
                      const SizedBox(height: Espacio.s6),
                      // La custodia va acá, no en un pie de página: este es el momento
                      // exacto en que alguien duda antes de entregar su contraseña, y
                      // lo que responde esa duda es dónde está su plata.
                      Row(
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
                      ),
                    ],
                  ),
                ],
              ),
            ),
            BarraDeAccion(
              hijo: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Boton(
                    texto: TextosIdentidad.ingresar,
                    icono: Icons.login,
                    variante: BotonVariante.primario,
                    expandido: true,
                    cargando: estado.enviando,
                    onPressed: _valido ? _ingresar : null,
                  ),
                  if (MediaQuery.viewInsetsOf(context).bottom == 0)
                    const SesionSinCuenta(),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
