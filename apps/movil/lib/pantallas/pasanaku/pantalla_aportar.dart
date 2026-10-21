import 'dart:async';

import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moleculas/chips_de_filtro.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'aporte_registrado.dart';
import 'dominio/cu21_cobrar_aporte.dart';
import 'tarjeta_del_monto_del_periodo.dart';
import '../../proveedores/haptica.dart';
import 'textos.dart';

const _canales = <({String clave, String texto, IconData icono})>[
  (
    clave: 'BILLETERA_MOVIL',
    texto: 'Billetera',
    icono: IconosDeMarca.canalBilletera,
  ),
  (clave: 'QR_INTEROPERABLE', texto: 'QR', icono: IconosDeMarca.canalQr),
  (
    clave: 'TRANSFERENCIA_BANCARIA',
    texto: 'Transferencia',
    icono: IconosDeMarca.canalBanco,
  ),
  (clave: 'TARJETA', texto: 'Tarjeta', icono: IconosDeMarca.canalTarjeta),
];

/// CU-21 · aportar. El monto lo fija el reglamento del grupo (no se inventa acá);
/// esta pantalla lo confirma y elige el canal, con la MISMA disciplina de doble
/// toque que billetera (F4): botón bloqueado con la clave de idempotencia.
class PantallaAportar extends ConsumerStatefulWidget {
  const PantallaAportar({
    super.key,
    required this.obligacionId,
    required this.montoDelPeriodo,
  });
  final String obligacionId;
  final String montoDelPeriodo;

  @override
  ConsumerState<PantallaAportar> createState() => _PantallaAportarState();
}

class _PantallaAportarState extends ConsumerState<PantallaAportar> {
  String _canal = 'BILLETERA_MOVIL';
  final _referencia = TextEditingController();

  @override
  void dispose() {
    _referencia.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cobrar = cobrarAporteProvider(widget.obligacionId);
    ref.listen(cobrar, (anterior, siguiente) {
      if (anterior?.value == null && siguiente.value != null) {
        unawaited(ref.read(hapticaProvider).exito().catchError((Object _) {}));
      }
    });
    final estado = ref.watch(cobrar);
    final resultado = estado.value;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const CabeceraDeSeccion(titulo: TextosPasanaku.tituloAportar),
              Expanded(
                child: resultado != null
                    ? Center(child: AporteRegistrado(resultado: resultado))
                    : SingleChildScrollView(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            const SizedBox(height: Espacio.s4),
                            TarjetaDelMontoDelPeriodo(
                              monto: widget.montoDelPeriodo,
                            ),
                            const SizedBox(height: Espacio.s5),
                            const TituloDeSeccion(
                              titulo: TextosPasanaku.comoPagas,
                            ),
                            ChipsDeFiltro<String>(
                              opciones: [
                                for (final c in _canales)
                                  (
                                    clave: c.clave,
                                    texto: c.texto,
                                    icono: c.icono,
                                  ),
                              ],
                              valor: _canal,
                              onChanged: (v) => setState(() => _canal = v),
                            ),
                            const SizedBox(height: Espacio.s5),
                            Campo(
                              etiqueta: TextosPasanaku.referenciaDelPago,
                              controlador: _referencia,
                              onChanged: (_) => setState(() {}),
                            ),
                            const SizedBox(height: Espacio.s4),
                            if (estado.hasError)
                              EstadoError(
                                error: estado.error!,
                                reintentar: () => ref.invalidate(
                                  cobrarAporteProvider(widget.obligacionId),
                                ),
                              ),
                          ],
                        ),
                      ),
              ),
              if (resultado == null)
                Padding(
                  padding: const EdgeInsets.only(
                    top: Espacio.s3,
                    bottom: Espacio.s4,
                  ),
                  // «Aportar» es pagar algo que vence: la única acción en naranja (A2).
                  child: Boton(
                    texto: TextosPasanaku.aportarBoton,
                    variante: BotonVariante.urgencia,
                    expandido: true,
                    cargando: estado.isLoading,
                    onPressed: _referencia.text.trim().isEmpty
                        ? null
                        : () => ref
                              .read(
                                cobrarAporteProvider(
                                  widget.obligacionId,
                                ).notifier,
                              )
                              .enviar(
                                monto: widget.montoDelPeriodo,
                                canal: _canal,
                                referenciaProveedor: _referencia.text.trim(),
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
