import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/acciones_rapidas.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';

/// Las acciones del saldo como círculos (ADR-053), con **Aportar destacada**: es lo que
/// mantiene vivo el pasanaku. Transferir existe como pantalla pero ninguna vista la
/// enlaza todavía; no se expone acá sin confirmar la decisión (ambigüedad A8 del plan
/// del rediseño).
class AccionesDeSaldo extends StatelessWidget {
  const AccionesDeSaldo({super.key, required this.cuentaId});

  final String cuentaId;

  @override
  Widget build(BuildContext context) {
    return AccionesRapidas(
      destacada: 0,
      acciones: [
        (
          texto: TextosBilletera.aportar,
          icono: IconosDeMarca.aportar,
          onTap: () => context.pushNamed('pasanaku.miEstado'),
        ),
        (
          texto: TextosBilletera.recargar,
          icono: IconosDeMarca.recargar,
          onTap: () => context.pushNamed(
            'billetera.recargar',
            queryParameters: {'cuenta': cuentaId},
          ),
        ),
        (
          texto: TextosBilletera.retirar,
          icono: IconosDeMarca.retirar,
          onTap: () => context.pushNamed(
            'billetera.retirar',
            queryParameters: {'cuenta': cuentaId},
          ),
        ),
      ],
    );
  }
}
