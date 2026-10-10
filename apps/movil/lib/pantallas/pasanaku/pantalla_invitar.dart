import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moleculas/pregunta.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/cu69_invitar_al_grupo.dart';
import 'invitacion_creada.dart';
import 'textos.dart';

const _canales = {'ENLACE': 'Enlace', 'SMS': 'SMS', 'WHATSAPP': 'WhatsApp'};

/// CU-69 · invitar a un contacto. El teléfono se valida con el mismo formato que
/// exige el contrato (`+591` y 8 dígitos); el servidor decide el resto (si está
/// suprimido, si ya es participante, etc.) — esta pantalla no adivina esas
/// respuestas, solo muestra el mensaje que vuelve.
///
/// La cabecera es la misma de «Organizar un grupo» (`CabeceraDeSeccion`, con la flecha
/// de volver en botón): antes llevaba un `AppBar` aparte y se leía como otra app.
class PantallaInvitar extends ConsumerStatefulWidget {
  const PantallaInvitar({super.key, required this.grupoId});
  final String grupoId;

  @override
  ConsumerState<PantallaInvitar> createState() => _PantallaInvitarState();
}

class _PantallaInvitarState extends ConsumerState<PantallaInvitar> {
  /// Los ocho dígitos que escribe quien invita; el `+591` ya está puesto en el campo.
  String _digitos = '';
  String _nombre = '';
  String _canal = 'ENLACE';

  static const _prefijo = '+591';
  String get _telefono => _digitos.isEmpty ? '' : '$_prefijo$_digitos';

  bool get _telefonoValido => RegExp(r'^\+591\d{8}$').hasMatch(_telefono);

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(invitarAlGrupoProvider);
    final resultado = estado.value;

    return Scaffold(
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: Espacio.s4),
              child: CabeceraDeSeccion(titulo: TextosPasanaku.tituloInvitar),
            ),
            Expanded(
              child: resultado != null
                  ? InvitacionCreada(
                      resultado: resultado,
                      telefono: _telefono,
                      alInvitarOtra: () {
                        setState(() {
                          _digitos = '';
                          _nombre = '';
                        });
                        ref.invalidate(invitarAlGrupoProvider);
                      },
                    )
                  : _formulario(estado),
            ),
          ],
        ),
      ),
    );
  }

  Widget _formulario(AsyncValue<SalidaInvitacion?> estado) => Column(
    children: [
      Expanded(
        child: ListView(
          padding: const EdgeInsets.all(Espacio.s4),
          children: [
            Pregunta(
              etiqueta: TextosPasanaku.telefonoInvitado,
              control: Campo(
                etiqueta: '',
                prefijo: _prefijo,
                tipoDeTeclado: TextInputType.phone,
                onChanged: (v) => setState(() => _digitos = v.trim()),
              ),
            ),
            const SizedBox(height: Espacio.s5),
            Pregunta(
              etiqueta: TextosPasanaku.nombreSugerido,
              control: Campo(
                etiqueta: '',
                onChanged: (v) => setState(() => _nombre = v),
              ),
            ),
            const SizedBox(height: Espacio.s5),
            Pregunta(
              etiqueta: TextosPasanaku.canalInvitacion,
              control: SelectorSegmentado<String>(
                opciones: _canales,
                valor: _canal,
                onChanged: (v) => setState(() => _canal = v),
              ),
            ),
            if (estado.hasError) ...[
              const SizedBox(height: Espacio.s4),
              EstadoError(
                error: estado.error!,
                reintentar: () => ref.invalidate(invitarAlGrupoProvider),
              ),
            ],
          ],
        ),
      ),
      BarraDeAccion(
        hijo: Boton(
          texto: TextosPasanaku.enviarInvitacion,
          variante: BotonVariante.primario,
          expandido: true,
          cargando: estado.isLoading,
          onPressed: _telefonoValido
              ? () => ref
                    .read(invitarAlGrupoProvider.notifier)
                    .enviar(
                      grupoId: widget.grupoId,
                      telefonoInvitado: _telefono,
                      canal: EntradaInvitacionCanalEnum.values.byName(_canal),
                      nombreSugerido: _nombre.isEmpty ? null : _nombre,
                    )
              : null,
        ),
      ),
    ],
  );
}
