import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos_de_refirma_y_etapa.dart';

/// El aviso de que hay una versión nueva del reglamento para aceptar (H1.S7.M4). Lo
/// decide el servidor (`reglamentoPendienteDeAceptar`): soy integrante ACTIVO y no
/// acepté la versión vigente, la que publica `iniciarGrupo` al achicar. Lleva a la
/// misma pantalla de firma, que ya muestra la versión nueva. Es distinto de
/// `debeFirmar`, la primera firma con el cupo RESERVADO, que tiene su propio aviso.
class AvisoDeRefirma extends StatelessWidget {
  const AvisoDeRefirma({super.key, required this.detalle});
  final DetalleDelGrupo detalle;

  /// Solo si el servidor lo pide y no es la primera firma (esa ya tiene su aviso).
  static bool aplica(DetalleDelGrupo d) =>
      !d.debeFirmar && d.reglamentoPendienteDeAceptar == true;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      const Alerta(
        titulo: TextosDeRefirmaYEtapa.avisoTitulo,
        detalle: TextosDeRefirmaYEtapa.avisoDetalle,
        tono: Tono.aviso,
      ),
      const SizedBox(height: Espacio.s2),
      Boton(
        key: const ValueKey('firmar-version-nueva'),
        texto: TextosDeRefirmaYEtapa.avisoBoton,
        variante: BotonVariante.primario,
        expandido: true,
        onPressed: () =>
            context.push('/pasanaku/grupos/${detalle.grupoId}/reglamento'),
      ),
      const SizedBox(height: Espacio.s3),
    ],
  );
}
