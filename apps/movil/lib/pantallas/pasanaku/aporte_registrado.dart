import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/organismos/estado_de_tramite.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// El aporte ya registrado: qué pasó y en qué estado quedó la obligación, en palabras. Antes se
/// mostraba el nombre del enum («PAGADO») y «misma clave», que no le dicen nada a nadie.
class AporteRegistrado extends StatelessWidget {
  const AporteRegistrado({super.key, required this.resultado});

  final SalidaCobro resultado;

  @override
  Widget build(BuildContext context) {
    return EstadoDeTramite(
      icono: IconosDeMarca.tramiteListo,
      tono: Tono.ok,
      titulo: TextosPasanaku.aporteConfirmado,
      // La idempotencia dicha en cristiano: un reintento no cobra dos veces.
      detalle: resultado.esNuevo ? null : TextosPasanaku.pagoYaRegistrado,
      extra: ChipEstado(
        texto: _estadoDeObligacion(resultado.estadoObligacion),
        tono: _tonoDeObligacion(resultado.estadoObligacion),
      ),
    );
  }
}

String _estadoDeObligacion(SalidaCobroEstadoObligacionEnum e) => switch (e) {
  SalidaCobroEstadoObligacionEnum.PENDIENTE => 'Pendiente',
  SalidaCobroEstadoObligacionEnum.PAGADO_PARCIAL => 'Pago parcial',
  SalidaCobroEstadoObligacionEnum.PAGADO => 'Pagado',
  SalidaCobroEstadoObligacionEnum.EN_MORA => 'En mora',
  SalidaCobroEstadoObligacionEnum.VENCIDO => 'Vencida',
};

Tono _tonoDeObligacion(SalidaCobroEstadoObligacionEnum e) => switch (e) {
  SalidaCobroEstadoObligacionEnum.PAGADO => Tono.ok,
  SalidaCobroEstadoObligacionEnum.PAGADO_PARCIAL => Tono.info,
  SalidaCobroEstadoObligacionEnum.PENDIENTE => Tono.neutro,
  SalidaCobroEstadoObligacionEnum.EN_MORA ||
  SalidaCobroEstadoObligacionEnum.VENCIDO => Tono.aviso,
};
