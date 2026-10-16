import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu11_retirar_saldo.dart';
import '../../dominio/proteccion_de_pantalla_dinero.dart';
import 'textos.dart';

/// CU-11 — retirar saldo. **Hueco declarado:** CU-18 (registrar y verificar cuenta
/// bancaria de destino) no está construido en este carril — el destino se pide como
/// texto libre en vez de un selector de cuentas verificadas; ver informe del carril.
///
/// El botón se bloquea con `cargando` mientras el retiro viaja, con la misma clave
/// de idempotencia en cada intento (invariante 7, gate propio del carril).
class PantallaRetirar extends ConsumerStatefulWidget {
  const PantallaRetirar({super.key, required this.cuentaId});
  final String cuentaId;

  @override
  ConsumerState<PantallaRetirar> createState() => _PantallaRetirarState();
}

class _PantallaRetirarState extends ConsumerState<PantallaRetirar>
    with ConProteccionDePantalla<PantallaRetirar> {
  String? _monto;
  String _instrumentoDestinoId = '';
  String _factorMfa = '';

  bool get _puedeEnviar =>
      _monto != null &&
      _instrumentoDestinoId.isNotEmpty &&
      _factorMfa.length == 6;

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(retirarSaldoProvider);
    final notifier = ref.read(retirarSaldoProvider.notifier);
    final resultado = estado.value;
    return Scaffold(
      appBar: AppBar(title: const Text(TextosBilletera.retirar)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: resultado != null
              ? Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.check_circle_outline, size: Espacio.s7),
                    const SizedBox(height: Espacio.s3),
                    const Text(
                      TextosBilletera.retiroInstruido,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: Espacio.s3),
                    Monto(
                      monto: resultado.montoNeto.monto,
                      moneda: resultado.montoNeto.moneda.value,
                      etiqueta: TextosBilletera.monto,
                    ),
                  ],
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    CampoMonto(
                      etiqueta: TextosBilletera.monto,
                      onChanged: (v) => setState(() => _monto = v),
                    ),
                    const SizedBox(height: Espacio.s3),
                    Campo(
                      etiqueta: TextosBilletera.cuentaDestino,
                      onChanged: (v) =>
                          setState(() => _instrumentoDestinoId = v),
                    ),
                    const SizedBox(height: Espacio.s3),
                    Campo(
                      etiqueta: TextosBilletera.factorMfa,
                      tipoDeTeclado: TextInputType.number,
                      onChanged: (v) => setState(() => _factorMfa = v),
                    ),
                    const SizedBox(height: Espacio.s4),
                    if (estado.hasError)
                      EstadoError(
                        error: estado.error!,
                        reintentar: () => ref.invalidate(retirarSaldoProvider),
                      ),
                    const Spacer(),
                    Boton(
                      texto: TextosBilletera.retirar,
                      variante: BotonVariante.primario,
                      expandido: true,
                      cargando: estado.isLoading,
                      onPressed: _puedeEnviar
                          ? () => notifier.enviar(
                              cuentaId: widget.cuentaId,
                              monto: _monto!,
                              instrumentoDestinoId: _instrumentoDestinoId,
                              factorMfa: _factorMfa,
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
