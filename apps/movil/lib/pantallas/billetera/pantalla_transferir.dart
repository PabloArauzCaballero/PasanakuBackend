import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/campo_monto_grande.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/organismos/estado_de_tramite.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/cu12_transferir_saldo.dart';
import '../../dominio/proteccion_de_pantalla_dinero.dart';
import 'textos.dart';

const _tipos = {
  EntradaTransferenciaDestinoTipoEnum.ALIAS: 'Alias',
  EntradaTransferenciaDestinoTipoEnum.GRUPO: 'Grupo',
};

/// CU-12 — transferir saldo. El botón queda `cargando` mientras la transferencia
/// viaja: un doble toque en mala señal reusa la misma clave de idempotencia.
class PantallaTransferir extends ConsumerStatefulWidget {
  const PantallaTransferir({super.key, required this.cuentaOrigenId});
  final String cuentaOrigenId;

  @override
  ConsumerState<PantallaTransferir> createState() => _PantallaTransferirState();
}

class _PantallaTransferirState extends ConsumerState<PantallaTransferir>
    with ConProteccionDePantalla<PantallaTransferir> {
  String? _monto;
  String _destino = '';
  String _concepto = '';
  EntradaTransferenciaDestinoTipoEnum _tipo =
      EntradaTransferenciaDestinoTipoEnum.ALIAS;

  bool get _puedeEnviar =>
      _monto != null && _destino.isNotEmpty && _concepto.isNotEmpty;

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(transferirSaldoProvider);
    final notifier = ref.read(transferirSaldoProvider.notifier);
    final resultado = estado.value;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const CabeceraDeSeccion(titulo: TextosBilletera.transferir),
              Expanded(
                child: resultado != null
                    ? const Center(
                        child: EstadoDeTramite(
                          icono: IconosDeMarca.tramiteListo,
                          tono: Tono.ok,
                          titulo: TextosBilletera.transferenciaHecha,
                        ),
                      )
                    : SingleChildScrollView(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            const SizedBox(height: Espacio.s3),
                            SelectorSegmentado<
                              EntradaTransferenciaDestinoTipoEnum
                            >(
                              opciones: _tipos,
                              valor: _tipo,
                              onChanged: (v) => setState(() => _tipo = v),
                            ),
                            const SizedBox(height: Espacio.s6),
                            CampoMontoGrande(
                              etiqueta: TextosBilletera.montoATransferir,
                              onChanged: (v) => setState(() => _monto = v),
                            ),
                            const SizedBox(height: Espacio.s6),
                            Campo(
                              etiqueta: TextosBilletera.alias,
                              onChanged: (v) => setState(() => _destino = v),
                            ),
                            const SizedBox(height: Espacio.s3),
                            Campo(
                              etiqueta: TextosBilletera.concepto,
                              onChanged: (v) => setState(() => _concepto = v),
                            ),
                            const SizedBox(height: Espacio.s4),
                            if (estado.hasError)
                              EstadoError(
                                error: estado.error!,
                                reintentar: () =>
                                    ref.invalidate(transferirSaldoProvider),
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
                  child: Boton(
                    texto: TextosBilletera.transferir,
                    variante: BotonVariante.primario,
                    expandido: true,
                    cargando: estado.isLoading,
                    onPressed: _puedeEnviar
                        ? () => notifier.enviar(
                            cuentaOrigenId: widget.cuentaOrigenId,
                            monto: _monto!,
                            tipoDestino: _tipo,
                            valorDestino: _destino,
                            concepto: _concepto,
                          )
                        : null,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
