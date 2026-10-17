import 'dart:async';

import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_sesion.dart';
import 'avisos_de_sesion.dart';
import 'sesion_pie.dart';
import 'sacudida_de_credenciales.dart';
import 'campos_de_sesion.dart';
import 'textos.dart';
import '../../navegacion/acceso_a_rutas.dart';
import '../../navegacion/retorno_de_invitacion.dart';
import '../../dominio/proteccion_de_pantalla_dinero.dart';
import '../../proveedores/sesion.dart';
import '../../proveedores/haptica.dart';

/// CU-04, paso 1: teléfono y contraseña. El servidor decide si además pide MFA —
/// esta pantalla nunca asume que no hace falta (invariante 7).
///
/// Es la segunda pantalla que ve alguien que abre la app, después de la portada, y
/// la primera que le pide algo. Por eso tiene la marca arriba y un saludo: entre
/// «AportaYa te reconoce» y «formulario de acceso» hay una diferencia de confianza
/// que en una billetera se paga.
class PantallaDeSesion extends ConsumerStatefulWidget {
  const PantallaDeSesion({super.key, this.cuentaRecienCreada = false});

  /// Se llega acá desde el último paso del alta. Sin decirlo, el salto del contrato
  /// aceptado a un formulario de acceso se lee como que algo falló: se completaron
  /// seis pasos y la app pide entrar de nuevo, sin explicar que la cuenta quedó hecha.
  final bool cuentaRecienCreada;

  @override
  ConsumerState<PantallaDeSesion> createState() => _PantallaDeSesionState();
}

class _PantallaDeSesionState extends ConsumerState<PantallaDeSesion>
    with ConProteccionDePantalla<PantallaDeSesion> {
  final _identificador = IdentificadorDeIngreso();
  final _contrasena = TextEditingController();
  MotivoDeSalida? _motivoDeSalida;
  final _sacudida = GlobalKey<SacudidaDeCredencialesState>();

  @override
  void initState() {
    super.initState();
    // El motivo pertenece a esta entrada al login: se toma una vez y no se vuelve
    // a mostrar al reconstruir el formulario.
    _motivoDeSalida = ref.read(sesionProvider).tomarMotivoDeSalida();
  }

  @override
  void dispose() {
    _identificador.dispose();
    _contrasena.dispose();
    super.dispose();
  }

  /// El formato mínimo que el contrato acepta: la contraseña no baja de ocho. Es
  /// ayuda para no gastar un viaje al servidor, no la decisión — la de verdad la
  /// toma el servidor. El `+591` del celular ya está puesto en el campo.
  bool get _valido => _identificador.valido && _contrasena.text.length >= 8;

  Future<void> _ingresar() async {
    final n = ref.read(sesionIdentidadProvider.notifier);
    n.actualizarCredenciales(
      identificador: _identificador.valor,
      contrasena: _contrasena.text,
    );
    final avanzo = await n.enviarCredenciales();
    if (!avanzo || !mounted) return;
    // A dónde ir lo dijo el servidor, no esta pantalla.
    final paso = ref.read(sesionIdentidadProvider).paso;
    final volver = GoRouterState.of(context).uri.queryParameters['volver'];
    final retornoInvitacion = retornoDeInvitacion(volver);
    final destino = destinoTrasIngresar(volver);
    if (paso == PasoSesion.mfa) {
      context.push(
        retornoInvitacion == null
            ? '/identidad/mfa'
            : '/identidad/mfa?volver=${Uri.encodeComponent(retornoInvitacion)}',
      );
    } else {
      context.go(retornoInvitacion ?? destino);
    }
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(sesionIdentidadProvider);
    ref.listen(sesionIdentidadProvider, (anterior, siguiente) {
      if (!siguiente.credencialesRechazadas ||
          anterior?.credencialesRechazadas == true) {
        return;
      }
      _sacudida.currentState?.ejecutar();
      unawaited(
        ref
            .read(hapticaProvider)
            .avisoDeCredencialesIncorrectas()
            .catchError((Object _) {}),
      );
    });
    final t = Tokens.of(context);
    return Scaffold(
      key: const Key('pantalla-de-ingreso'),
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
                      ...avisosDeSesion(
                        cuentaRecienCreada: widget.cuentaRecienCreada,
                        motivoDeSalida: _motivoDeSalida,
                        error: estado.error,
                        trazaId: estado.trazaId,
                      ),
                      SacudidaDeCredenciales(
                        key: _sacudida,
                        child: CamposDeSesion(
                          identificador: _identificador,
                          contrasena: _contrasena,
                          onChanged: () => setState(() {}),
                        ),
                      ),
                      const SizedBox(height: Espacio.s2),
                      const SesionOlvide(),
                      const SizedBox(height: Espacio.s6),
                      notaDeCustodia(context),
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
