import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart'
    show SalidaExtracto;
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu15_emitir_extracto.dart';
import '../../dominio/proteccion_de_pantalla_dinero.dart';
import 'recargable.dart';
import 'boton_de_certificado.dart';
import 'lista_del_extracto.dart';
import 'tarjeta_de_extracto.dart';
import 'textos.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';

/// CU-15 — extracto y certificado de saldo del período elegido. El importe final se
/// pinta con el átomo `Monto`, nunca con formato implementado localmente.
class PantallaExtracto extends ConsumerStatefulWidget {
  const PantallaExtracto({
    super.key,
    required this.cuentaId,
    this.rangoInicial,
  });
  final String cuentaId;

  /// El período con el que abre; sin él, la pantalla pide elegirlo.
  final DateTimeRange? rangoInicial;

  @override
  ConsumerState<PantallaExtracto> createState() => _PantallaExtractoState();
}

class _PantallaExtractoState extends ConsumerState<PantallaExtracto>
    with ConProteccionDePantalla<PantallaExtracto> {
  late DateTimeRange? _rango = widget.rangoInicial;

  @override
  Widget build(BuildContext context) {
    final rango = _rango;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const CabeceraDeSeccion(titulo: TextosBilletera.extracto),
              const SizedBox(height: Espacio.s2),
              MarcaDeTutorial(
                id: 'billetera.extracto',
                hijo: Boton(
                  texto: rango == null
                      ? TextosBilletera.generarExtracto
                      : TextosBilletera.elegirOtroPeriodo,
                  variante: rango == null
                      ? BotonVariante.primario
                      : BotonVariante.secundario,
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
              // Deslizar hacia abajo vuelve a pedir el extracto elegido (2026-10-19).
              Expanded(
                child: Recargable(
                  alineacion: rango == null
                      ? Alignment.center
                      : Alignment.topCenter,
                  alRecargar: () async {
                    if (rango == null) return;
                    final parametros = ParametrosExtracto(
                      cuentaId: widget.cuentaId,
                      desde: rango.start,
                      hasta: rango.end,
                    );
                    ref.invalidate(extractoProvider(parametros));
                    try {
                      await ref.read(extractoProvider(parametros).future);
                    } on Object {
                      // El error ya lo pinta la pantalla.
                    }
                  },
                  hijo: rango == null
                      ? const EstadoVacio(
                          icono: IconosDeMarca.periodo,
                          titulo: TextosBilletera.elegiUnPeriodo,
                          mensaje: TextosBilletera.sinExtractoDetalle,
                        )
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
                          exito: (e) =>
                              _Extracto(cuentaId: widget.cuentaId, extracto: e),
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

class _Extracto extends StatelessWidget {
  const _Extracto({required this.cuentaId, required this.extracto});
  final String cuentaId;
  final SalidaExtracto extracto;

  @override
  Widget build(BuildContext context) {
    final e = extracto;
    return SizedBox(
      width: double.infinity,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TarjetaDeExtracto(
            desde: e.desde,
            hasta: e.hasta,
            saldoFinal: e.saldoFinal.monto,
            moneda: e.saldoFinal.moneda.value,
            cantidadMovimientos: e.cantidadMovimientos,
          ),
          const SizedBox(height: Espacio.s3),
          BotonDeCertificado(
            periodo: TarjetaDeExtracto.textoDelPeriodo(e.desde, e.hasta),
            moneda: e.saldoFinal.moneda.value,
            saldo: e.saldoFinal.monto,
          ),
          const SizedBox(height: Espacio.s5),
          ListaDelExtracto(cuentaId: cuentaId, desde: e.desde, hasta: e.hasta),
          const SizedBox(height: Espacio.s5),
        ],
      ),
    );
  }
}
