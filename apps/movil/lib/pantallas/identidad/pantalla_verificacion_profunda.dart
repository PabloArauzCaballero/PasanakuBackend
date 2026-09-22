import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/estado_cuenta.dart';
import 'textos.dart';

/// CU-02 (elevar nivel de debida diligencia) + CU-03 (declaración PEP), una sola
/// pantalla como marca `planes/12` fila 2.8 de la maqueta: la elevación de nivel
/// la dispara el servidor cuando corresponde (por ejemplo, un monto declarado alto
/// en el perfil transaccional); acá solo se recoge la declaración PEP, que es lo
/// único con forma propia en la bóveda para esta pantalla.
class PantallaDeVerificacionProfunda extends ConsumerStatefulWidget {
  const PantallaDeVerificacionProfunda({super.key});

  @override
  ConsumerState<PantallaDeVerificacionProfunda> createState() =>
      _PantallaDeVerificacionProfundaState();
}

class _PantallaDeVerificacionProfundaState
    extends ConsumerState<PantallaDeVerificacionProfunda> {
  bool? _esPep;
  final _detalle = TextEditingController();

  @override
  void dispose() {
    _detalle.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Verificación adicional')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(TextosIdentidad.preguntaPep),
              const SizedBox(height: Espacio.s3),
              SegmentedButton<bool>(
                segments: const [
                  ButtonSegment(value: false, label: Text('No')),
                  ButtonSegment(value: true, label: Text('Sí')),
                ],
                selected: {?_esPep},
                emptySelectionAllowed: true,
                onSelectionChanged: (v) =>
                    setState(() => _esPep = v.isEmpty ? null : v.first),
              ),
              if (_esPep == true) ...[
                const SizedBox(height: Espacio.s3),
                Campo(
                  etiqueta: TextosIdentidad.detallePep,
                  controlador: _detalle,
                  lineas: 3,
                ),
              ],
              const SizedBox(height: Espacio.s5),
              Boton(
                texto: TextosIdentidad.continuar,
                variante: BotonVariante.primario,
                expandido: true,
                onPressed: _esPep == null
                    ? null
                    : () => ref
                          .read(pepProvider.notifier)
                          .declarar(esPep: _esPep!, detalle: _detalle.text),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
