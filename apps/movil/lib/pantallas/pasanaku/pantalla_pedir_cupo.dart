import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/intro_de_pantalla.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moleculas/pregunta.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/cu68_postular_al_grupo.dart';
import 'pedido_enviado.dart';
import 'textos.dart';

/// CU-68 · pedir un cupo. **D-15**: el botón dice "Pedir mi cupo", nunca "Unirme" —
/// pedir el cupo no lo ocupa, lo ocupa el organizador al aceptar. Tras el envío se
/// muestra "Tu pedido de cupo" con quién decide, en cuánto, qué ve de vos y las tres
/// salidas posibles (D-15), y el aviso explícito de que la lista de grupos propios
/// todavía no lo va a traer.
class PantallaPedirCupo extends ConsumerStatefulWidget {
  const PantallaPedirCupo({super.key, required this.grupoId});
  final String grupoId;

  @override
  ConsumerState<PantallaPedirCupo> createState() => _PantallaPedirCupoState();
}

class _PantallaPedirCupoState extends ConsumerState<PantallaPedirCupo> {
  int _cupos = 1;
  String _mensaje = '';

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(postularAlGrupoProvider(widget.grupoId));
    final resultado = estado.value;

    return Scaffold(
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: Espacio.s4),
              child: CabeceraDeSeccion(titulo: TextosPasanaku.tituloPedirCupo),
            ),
            Expanded(
              child: resultado != null
                  ? Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: Espacio.s4,
                      ),
                      child: PedidoEnviado(
                        motivos: (resultado.motivos as List).cast<String>(),
                      ),
                    )
                  : _Formulario(
                      cupos: _cupos,
                      onCuposChanged: (v) => setState(() => _cupos = v),
                      onMensajeChanged: (v) => setState(() => _mensaje = v),
                      cargando: estado.isLoading,
                      error: estado.hasError ? estado.error : null,
                      onEnviar: () => ref
                          .read(
                            postularAlGrupoProvider(widget.grupoId).notifier,
                          )
                          .enviar(
                            grupoId: widget.grupoId,
                            cuposSolicitados: _cupos,
                            mensaje: _mensaje.isEmpty ? null : _mensaje,
                          ),
                      reintentar: () => ref.invalidate(
                        postularAlGrupoProvider(widget.grupoId),
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Formulario extends StatelessWidget {
  const _Formulario({
    required this.cupos,
    required this.onCuposChanged,
    required this.onMensajeChanged,
    required this.cargando,
    required this.error,
    required this.onEnviar,
    required this.reintentar,
  });

  final int cupos;
  final ValueChanged<int> onCuposChanged;
  final ValueChanged<String> onMensajeChanged;
  final bool cargando;
  final Object? error;
  final VoidCallback onEnviar;
  final VoidCallback reintentar;

  @override
  Widget build(BuildContext context) => Column(
    children: [
      Expanded(
        child: ListView(
          padding: const EdgeInsets.all(Espacio.s4),
          children: [
            // Qué pide y quién decide, como entrada y no como alerta: no es un problema.
            const IntroDePantalla(
              icono: IconosDeMarca.solicitudes,
              titulo: TextosPasanaku.cupoEnEspera,
              explicacion: TextosPasanaku.cupoNoOcupadoAviso,
            ),
            const SizedBox(height: Espacio.s1),
            Pregunta(
              etiqueta: TextosPasanaku.cuposSolicitados,
              control: SelectorSegmentado<int>(
                opciones: const {1: '1', 2: '2', 3: '3'},
                valor: cupos,
                onChanged: onCuposChanged,
              ),
            ),
            const SizedBox(height: Espacio.s5),
            Campo(
              etiqueta: TextosPasanaku.mensajeOpcional,
              lineas: 3,
              onChanged: onMensajeChanged,
            ),
            if (error != null) ...[
              const SizedBox(height: Espacio.s4),
              EstadoError(error: error!, reintentar: reintentar),
            ],
          ],
        ),
      ),
      BarraDeAccion(
        hijo: Boton(
          texto: TextosPasanaku.pedirCupoBoton,
          variante: BotonVariante.primario,
          expandido: true,
          cargando: cargando,
          onPressed: onEnviar,
        ),
      ),
    ],
  );
}
