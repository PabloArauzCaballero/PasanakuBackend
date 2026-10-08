import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/error_de_operacion_de_dinero.dart';
import 'dominio/clave_persistida_de_aporte.dart';
import 'monto_fijo_aporte.dart';
import 'textos.dart';

const canalesDeAporte = {
  'BILLETERA_MOVIL': 'Billetera',
  'QR_INTEROPERABLE': 'QR',
  'TRANSFERENCIA_BANCARIA': 'Transferencia',
  'TARJETA': 'Tarjeta',
};

/// Datos editables antes de la revisión: esta pantalla no envía dinero.
class FormularioAporte extends StatelessWidget {
  const FormularioAporte({
    super.key,
    required this.monto,
    required this.canal,
    required this.referencia,
    required this.intentoEnviar,
    required this.estado,
    required this.avisoConexion,
    required this.onCanalChanged,
    required this.onReferenciaChanged,
    required this.onRevisar,
  });

  final String monto;
  final String canal;
  final TextEditingController referencia;
  final bool intentoEnviar;
  final AsyncValue<SalidaCobro?> estado;
  final Widget? avisoConexion;
  final ValueChanged<String> onCanalChanged;
  final VoidCallback onReferenciaChanged;
  final VoidCallback onRevisar;

  @override
  Widget build(BuildContext context) => CustomScrollView(
    keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
    slivers: [
      SliverPadding(
        padding: const EdgeInsets.all(Espacio.s4),
        sliver: SliverFillRemaining(
          hasScrollBody: false,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (avisoConexion != null) ...[
                avisoConexion!,
                const SizedBox(height: Espacio.s4),
              ],
              MontoFijoAporte(monto: monto),
              const SizedBox(height: Espacio.s5),
              Text(
                TextosPasanaku.aporteCanal,
                style: Theme.of(context).textTheme.labelLarge,
              ),
              const SizedBox(height: Espacio.s1),
              DropdownButtonFormField<String>(
                initialValue: canal,
                isExpanded: true,
                items: [
                  for (final opcion in canalesDeAporte.entries)
                    DropdownMenuItem(
                      value: opcion.key,
                      child: Text(opcion.value),
                    ),
                ],
                onChanged: estado.isLoading
                    ? null
                    : (valor) {
                        if (valor != null) onCanalChanged(valor);
                      },
              ),
              const SizedBox(height: Espacio.s4),
              Campo(
                etiqueta: TextosPasanaku.aporteReferencia,
                controlador: referencia,
                ayuda: TextosPasanaku.aporteReferenciaAyuda,
                error: intentoEnviar && referencia.text.trim().isEmpty
                    ? TextosPasanaku.aporteReferenciaFaltante
                    : null,
                habilitado: !estado.isLoading,
                onChanged: (_) {
                  if (intentoEnviar) onReferenciaChanged();
                },
                onSubmitted: (_) => onRevisar(),
              ),
              const SizedBox(height: Espacio.s5),
              if (estado.hasError)
                EstadoError(
                  error: estado.error!,
                  reintentar:
                      estado.error is ErrorDeOperacionPendiente ||
                          estado.error is ErrorDeAportePendiente
                      ? null
                      : onRevisar,
                )
              else ...[
                const Spacer(),
                Boton(
                  texto: TextosPasanaku.revisarAporte,
                  variante: BotonVariante.primario,
                  expandido: true,
                  cargando: estado.isLoading,
                  onPressed: onRevisar,
                ),
              ],
            ],
          ),
        ),
      ),
    ],
  );
}
