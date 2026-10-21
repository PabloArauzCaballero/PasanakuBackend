import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:aportaya_diseno/organismos/encabezado_de_saldo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'acciones_de_saldo.dart';
import 'lista_de_movimientos.dart';
import 'textos.dart';

/// La cifra, las acciones principales y el contexto financiero de la billetera.
class CuerpoDeSaldo extends StatelessWidget {
  const CuerpoDeSaldo({
    super.key,
    required this.saldo,
    required this.movimientos,
    required this.reintentarMovimientos,
  });

  final SaldoBilletera saldo;
  final AsyncValue<ListaMovimientosBilletera> movimientos;
  final VoidCallback reintentarMovimientos;

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.only(top: Espacio.s4, bottom: Espacio.s6),
      children: [
        MarcaDeTutorial(
          id: 'billetera.saldo',
          hijo: EncabezadoDeSaldo(
            etiqueta: TextosBilletera.disponible,
            etiquetaHablada: TextosBilletera.saldoDisponible,
            monto: saldo.disponible.monto,
            moneda: saldo.disponible.moneda.value,
            cifras: [
              (
                etiqueta: TextosBilletera.saldoRetenido,
                monto: saldo.retenido.monto,
              ),
            ],
            distintivo: TextosBilletera.custodia,
            acciones: MarcaDeTutorial(
              id: 'billetera.acciones',
              hijo: AccionesDeSaldo(cuentaId: saldo.cuentaId),
            ),
          ),
        ),
        const SizedBox(height: Espacio.s5),
        const TituloDeSeccion(titulo: TextosBilletera.tusPasanakus),
        // El vacío vende la primera acción (ADR-053): dos caminos, crear uno o entrar con
        // el enlace o el QR de quien organiza.
        Tarjeta(
          child: EstadoVacio(
            icono: IconosDeMarca.grupos,
            titulo: TextosBilletera.tituloSinPasanakus,
            mensaje: TextosBilletera.sinPasanakus,
            accion: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Boton(
                  texto: TextosBilletera.crearUnGrupo,
                  variante: BotonVariante.primario,
                  expandido: true,
                  onPressed: () => context.push('/pasanaku/grupos/nuevo'),
                ),
                const SizedBox(height: Espacio.s2),
                Boton(
                  texto: TextosBilletera.entrarConEnlace,
                  variante: BotonVariante.secundario,
                  expandido: true,
                  onPressed: () => context.push('/pasanaku/escanear'),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: Espacio.s5),
        TituloDeSeccion(
          titulo: TextosBilletera.movimientos,
          accion: TextosBilletera.todos,
          alTocarAccion: () => context.pushNamed(
            'billetera.extracto',
            queryParameters: {'cuenta': saldo.cuentaId},
          ),
        ),
        ListaDeMovimientos(
          valor: movimientos,
          reintentar: reintentarMovimientos,
        ),
      ],
    );
  }
}
