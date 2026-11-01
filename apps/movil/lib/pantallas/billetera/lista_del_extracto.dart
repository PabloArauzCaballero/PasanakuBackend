import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/fecha.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu13_consultar_saldo.dart';
import 'lista_de_movimientos.dart' show FilaDeMovimiento;
import 'movimientos_del_periodo.dart';
import 'textos.dart';

const _opciones = {
  FiltroDelExtracto.todos: TextosBilletera.filtroTodos,
  FiltroDelExtracto.entradas: TextosBilletera.filtroEntradas,
  FiltroDelExtracto.salidas: TextosBilletera.filtroSalidas,
};

/// El detalle del período del extracto: filtros por sentido y los movimientos agrupados por
/// día. Sale de la misma lectura del libro que la pantalla de movimientos, recortada al período.
class ListaDelExtracto extends ConsumerStatefulWidget {
  const ListaDelExtracto({
    super.key,
    required this.cuentaId,
    required this.desde,
    required this.hasta,
  });
  final String cuentaId;
  final DateTime desde;
  final DateTime hasta;

  @override
  ConsumerState<ListaDelExtracto> createState() => _ListaDelExtractoState();
}

class _ListaDelExtractoState extends ConsumerState<ListaDelExtracto> {
  var _filtro = FiltroDelExtracto.todos;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final lectura = ref.watch(movimientosBilleteraProvider(widget.cuentaId));
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const TituloDeSeccion(titulo: TextosBilletera.detalleDelPeriodo),
        SelectorSegmentado<FiltroDelExtracto>(
          opciones: _opciones,
          valor: _filtro,
          onChanged: (v) => setState(() => _filtro = v),
        ),
        const SizedBox(height: Espacio.s3),
        lectura.when(
          loading: () => Semantics(
            label: TextosBilletera.cargandoMovimientos,
            child: const LinearProgressIndicator(minHeight: 2),
          ),
          error: (_, _) => Row(
            children: [
              Expanded(
                child: Text(
                  TextosBilletera.errorMovimientos,
                  style: Tipo.cuerpoChico.copyWith(color: t.text2),
                ),
              ),
              Boton(
                texto: TextosBilletera.reintentar,
                variante: BotonVariante.enlace,
                onPressed: () => ref.invalidate(
                  movimientosBilleteraProvider(widget.cuentaId),
                ),
              ),
            ],
          ),
          data: (r) {
            final dias = movimientosDelPeriodo(
              r.movimientos,
              desde: widget.desde,
              hasta: widget.hasta,
              filtro: _filtro,
            );
            return Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (dias.isEmpty)
                  const EstadoVacio(
                    icono: IconosDeMarca.sinDatos,
                    mensaje: TextosBilletera.sinMovimientosConEseFiltro,
                  )
                else
                  for (final d in dias) ...[
                    Padding(
                      padding: const EdgeInsets.only(
                        top: Espacio.s2,
                        bottom: Espacio.s1,
                      ),
                      child: Semantics(
                        header: true,
                        child: Text(
                          // Un día de calendario: se muestra con sus propios día, mes y año.
                          Fecha.formatearDia(d.dia),
                          style: Tipo.etiqueta.copyWith(color: t.text2),
                        ),
                      ),
                    ),
                    for (final MovimientoBilletera m in d.filas)
                      FilaDeMovimiento(movimiento: m),
                  ],
                if (lecturaPudoQuedarCorta(
                  r.movimientos,
                  limite: 100,
                  desde: widget.desde,
                ))
                  Padding(
                    padding: const EdgeInsets.only(top: Espacio.s3),
                    child: Text(
                      TextosBilletera.lecturaCorta,
                      style: Tipo.cuerpoChico.copyWith(color: t.text2),
                    ),
                  ),
              ],
            );
          },
        ),
      ],
    );
  }
}
