import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/cu68_postular_al_grupo.dart';
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
      appBar: AppBar(title: const Text(TextosPasanaku.tituloPedirCupo)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: resultado != null
              ? _PedidoEnviado(resultado: resultado)
              : _Formulario(
                  cupos: _cupos,
                  onCuposChanged: (v) => setState(() => _cupos = v),
                  mensaje: _mensaje,
                  onMensajeChanged: (v) => setState(() => _mensaje = v),
                  cargando: estado.isLoading,
                  error: estado.hasError ? estado.error : null,
                  onEnviar: () => ref
                      .read(postularAlGrupoProvider(widget.grupoId).notifier)
                      .enviar(
                        grupoId: widget.grupoId,
                        cuposSolicitados: _cupos,
                        mensaje: _mensaje.isEmpty ? null : _mensaje,
                      ),
                ),
        ),
      ),
    );
  }
}

class _Formulario extends StatelessWidget {
  const _Formulario({
    required this.cupos,
    required this.onCuposChanged,
    required this.mensaje,
    required this.onMensajeChanged,
    required this.cargando,
    required this.error,
    required this.onEnviar,
  });

  final int cupos;
  final ValueChanged<int> onCuposChanged;
  final String mensaje;
  final ValueChanged<String> onMensajeChanged;
  final bool cargando;
  final Object? error;
  final VoidCallback onEnviar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          TextosPasanaku.cupoNoOcupadoAviso,
          style: Theme.of(
            context,
          ).textTheme.bodySmall?.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s4),
        Campo(
          etiqueta: TextosPasanaku.cuposSolicitados,
          valorInicial: '$cupos',
          tipoDeTeclado: TextInputType.number,
          formateadores: [FilteringTextInputFormatter.digitsOnly],
          onChanged: (v) {
            final n = int.tryParse(v);
            if (n != null && n >= 1 && n <= 3) onCuposChanged(n);
          },
        ),
        const SizedBox(height: Espacio.s4),
        Campo(
          etiqueta: TextosPasanaku.mensajeOpcional,
          lineas: 3,
          onChanged: onMensajeChanged,
        ),
        const SizedBox(height: Espacio.s4),
        Boton(
          texto: TextosPasanaku.pedirCupoBoton,
          variante: BotonVariante.primario,
          cargando: cargando,
          onPressed: onEnviar,
        ),
      ],
    );
  }
}

class _PedidoEnviado extends StatelessWidget {
  const _PedidoEnviado({required this.resultado});
  final dynamic resultado;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            TextosPasanaku.pedidoEnviadoTitulo,
            style: texto.titleLarge?.copyWith(color: t.text),
          ),
          const SizedBox(height: Espacio.s3),
          Text(TextosPasanaku.quienDecide, style: texto.bodyMedium),
          const SizedBox(height: Espacio.s2),
          Text(TextosPasanaku.enCuanto, style: texto.bodyMedium),
          const SizedBox(height: Espacio.s2),
          Text(TextosPasanaku.queVeDeVos, style: texto.bodyMedium),
          const SizedBox(height: Espacio.s2),
          Text(TextosPasanaku.tresSalidas, style: texto.bodyMedium),
          const SizedBox(height: Espacio.s4),
          Text(
            TextosPasanaku.cupoNoOcupadoAviso,
            style: texto.bodySmall?.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s4),
          if ((resultado.motivos as List).isNotEmpty) ...[
            Text(
              TextosPasanaku.puntajeTitulo,
              style: texto.titleMedium?.copyWith(color: t.text),
            ),
            const SizedBox(height: Espacio.s2),
            for (final m in resultado.motivos as List<String>)
              Text('• $m', style: texto.bodyMedium),
          ],
        ],
      ),
    );
  }
}
