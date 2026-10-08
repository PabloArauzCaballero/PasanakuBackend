import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'acciones_de_saldo.dart';
import '../../proveedores/conexion.dart';
import 'textos.dart';
import 'package:aportaya_diseno/moleculas/aparicion_escalonada.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/organismos/encabezado_de_saldo.dart';
import 'package:aportaya_diseno/organismos/aviso_de_conexion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';

/// Lo que se ve cuando hay saldo: la cifra arriba, sus tres acciones, y debajo las
/// secciones que dan contexto.
///
/// El saldo ya no vive en una tarjeta verde. La única superficie destacada de la
/// pantalla es la de **lo que vence**: si todo grita, nada avisa.
class CuerpoDeSaldo extends StatelessWidget {
  const CuerpoDeSaldo({
    super.key,
    required this.saldo,
    required this.conexion,
    required this.reintentarConexion,
  });

  final SaldoBilletera saldo;
  final AsyncValue<bool> conexion;
  final VoidCallback reintentarConexion;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final hayRetenido = saldo.retenido.monto != '0.00';
    final operacionesPausadas = !conexion.permiteOperar;
    return ListView(
      padding: const EdgeInsets.only(top: Espacio.s4, bottom: Espacio.s6),
      children: [
        if (operacionesPausadas) ...[
          AvisoDeConexion(
            estado: conexion,
            detalleSinConexion: TextosBilletera.saldoSinConexion,
            reintentar: reintentarConexion,
          ),
          const SizedBox(height: Espacio.s4),
        ],
        AparicionEscalonada(
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
                acciones: MarcaDeTutorial(
                  id: 'billetera.acciones',
                  hijo: AccionesDeSaldo(
                    operacionesPausadas: operacionesPausadas,
                  ),
                ),
              ),
            ),
            const SizedBox(height: Espacio.s5),
            const TituloDeSeccion(titulo: TextosBilletera.tusPasanakus),
            Panel(
              hijo: Text(
                TextosBilletera.detallePasanakusPendiente,
                style: Tipo.cuerpoChico.copyWith(color: t.text2),
              ),
            ),
            const SizedBox(height: Espacio.s5),
            TituloDeSeccion(
              titulo: TextosBilletera.extracto,
              accion: TextosBilletera.generarExtracto,
              alTocarAccion: () => context.pushNamed('billetera.extracto'),
            ),
            Panel(
              hijo: Text(
                TextosBilletera.consultaExtracto,
                style: Tipo.cuerpoChico.copyWith(color: t.text2),
              ),
            ),
            const SizedBox(height: Espacio.s5),
            // La custodia va acá abajo, en voz baja: tranquiliza cuando alguien la busca y
            // no le compite a la cifra cuando no. Antes vivía bajo el título «Movimientos»,
            // donde no es ni un movimiento ni nada que ese título anuncie.
            Text(
              hayRetenido
                  ? TextosBilletera.custodia
                  : '${TextosBilletera.custodia}. ${TextosBilletera.nadaTrabado}.',
              style: Tipo.ayuda.copyWith(color: t.text3),
            ),
          ],
        ),
      ],
    );
  }
}
