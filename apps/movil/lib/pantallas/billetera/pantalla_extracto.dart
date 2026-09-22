import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu15_emitir_extracto.dart';
import '../../dominio/proteccion_de_pantalla_dinero.dart';
import 'textos.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';

/// CU-15 — extracto y certificado de saldo del período elegido. El importe final se
/// pinta con el átomo `Monto`, nunca formateado a mano.
class PantallaExtracto extends ConsumerStatefulWidget {
  const PantallaExtracto({super.key, required this.cuentaId});
  final String cuentaId;

  @override
  ConsumerState<PantallaExtracto> createState() => _PantallaExtractoState();
}

class _PantallaExtractoState extends ConsumerState<PantallaExtracto>
    with ConProteccionDePantalla<PantallaExtracto> {
  DateTimeRange? _rango;

  @override
  Widget build(BuildContext context) {
    final rango = _rango;
    return Scaffold(
      appBar: AppBar(title: const Text(TextosBilletera.extracto)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              MarcaDeTutorial(
                id: 'billetera.extracto',
                hijo: Boton(
                  texto: TextosBilletera.generarExtracto,
                  variante: BotonVariante.secundario,
                  onPressed: () async {
                    final ahora = DateTime.now();
                    final elegido = await showDateRangePicker(
                      context: context,
                      firstDate: ahora.subtract(const Duration(days: 365)),
                      lastDate: ahora,
                    );
                    if (elegido != null) setState(() => _rango = elegido);
                  },
                ),
              ),
              const SizedBox(height: Espacio.s4),
              Expanded(
                child: rango == null
                    ? const Center(child: Text(TextosBilletera.sinExtracto))
                    : EstadoDePantalla(
                        valor: ref.watch(
                          extractoProvider(
                            ParametrosExtracto(
                              cuentaId: widget.cuentaId,
                              desde: rango.start,
                              hasta: rango.end,
                            ),
                          ),
                        ),
                        etiquetaDeCarga: TextosBilletera.cargandoSaldo,
                        mensajeVacio: TextosBilletera.sinMovimientos,
                        vacio: (e) => (e.cantidadMovimientos ?? 0) == 0,
                        reintentar: () => ref.invalidate(
                          extractoProvider(
                            ParametrosExtracto(
                              cuentaId: widget.cuentaId,
                              desde: rango.start,
                              hasta: rango.end,
                            ),
                          ),
                        ),
                        exito: (e) => Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '${TextosBilletera.desde} ${e.desde.toLocal()} · '
                              '${TextosBilletera.hasta} ${e.hasta.toLocal()}',
                            ),
                            const SizedBox(height: Espacio.s2),
                            Monto(
                              monto: e.saldoFinal.monto,
                              moneda: e.saldoFinal.moneda.value,
                              etiqueta: TextosBilletera.saldoDisponible,
                            ),
                          ],
                        ),
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
