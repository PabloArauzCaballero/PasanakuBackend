import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/cu69_invitar_al_grupo.dart';
import 'textos.dart';

const _canales = {'ENLACE': 'Enlace', 'SMS': 'SMS', 'WHATSAPP': 'WhatsApp'};

/// CU-69 · invitar a un contacto. El teléfono se valida con el mismo formato que
/// exige el contrato (`+591` y 8 dígitos); el servidor decide el resto (si está
/// suprimido, si ya es participante, etc.) — esta pantalla no adivina esas
/// respuestas, solo muestra el mensaje que vuelve.
class PantallaInvitar extends ConsumerStatefulWidget {
  const PantallaInvitar({super.key, required this.grupoId});
  final String grupoId;

  @override
  ConsumerState<PantallaInvitar> createState() => _PantallaInvitarState();
}

class _PantallaInvitarState extends ConsumerState<PantallaInvitar> {
  String _telefono = '';
  String _nombre = '';
  String _canal = 'ENLACE';

  bool get _telefonoValido => RegExp(r'^\+591\d{8}$').hasMatch(_telefono);

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(invitarAlGrupoProvider);
    final resultado = estado.value;

    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloInvitar)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: resultado != null
              ? Text(resultado.mensaje)
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Campo(
                      etiqueta: TextosPasanaku.telefonoInvitado,
                      tipoDeTeclado: TextInputType.phone,
                      onChanged: (v) => setState(() => _telefono = v),
                    ),
                    const SizedBox(height: Espacio.s4),
                    Campo(
                      etiqueta: TextosPasanaku.nombreSugerido,
                      onChanged: (v) => setState(() => _nombre = v),
                    ),
                    const SizedBox(height: Espacio.s4),
                    const Text(TextosPasanaku.canalInvitacion),
                    const SizedBox(height: Espacio.s2),
                    SelectorSegmentado<String>(
                      opciones: _canales,
                      valor: _canal,
                      onChanged: (v) => setState(() => _canal = v),
                    ),
                    const SizedBox(height: Espacio.s4),
                    if (estado.hasError)
                      EstadoError(
                        error: estado.error!,
                        reintentar: () =>
                            ref.invalidate(invitarAlGrupoProvider),
                      ),
                    const Spacer(),
                    Boton(
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
                                  canal: EntradaInvitacionCanalEnum.values
                                      .byName(_canal),
                                  nombreSugerido: _nombre.isEmpty
                                      ? null
                                      : _nombre,
                                )
                          : null,
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}
