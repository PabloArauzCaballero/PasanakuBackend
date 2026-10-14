import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo_contrasena.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/referencia_de_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos.dart';
import '../textos_del_alta.dart';
import 'bloque_de_formulario.dart';

/// Clave y envío del código de confirmación del correo.
class BloqueClaveDeCuenta extends StatelessWidget {
  const BloqueClaveDeCuenta({
    super.key,
    required this.clave,
    required this.repetida,
    required this.focoClave,
    required this.errorClave,
    required this.errorRepetida,
    required this.errorRemoto,
    required this.trazaId,
    required this.errorCodigo,
    required this.cargando,
    required this.onCambioClave,
    required this.onCambioRepetida,
    required this.onContinuar,
  });

  final TextEditingController clave;
  final TextEditingController repetida;
  final FocusNode focoClave;
  final String? errorClave;
  final String? errorRepetida;
  final String? errorRemoto;
  final String? trazaId;
  final String? errorCodigo;
  final bool cargando;
  final ValueChanged<String> onCambioClave;
  final ValueChanged<String> onCambioRepetida;
  final VoidCallback onContinuar;

  @override
  Widget build(BuildContext context) => BloqueDeFormulario(
    icono: Icons.lock_outline,
    titulo: 'Cómo entrás',
    hijos: [
      Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            TextosDelAlta.claveIntro,
            style: Tipo.cuerpoChico.copyWith(color: Tokens.of(context).text2),
          ),
          const SizedBox(height: Espacio.s4),
          CampoContrasena(
            etiqueta: TextosDelAlta.claveNueva,
            controlador: clave,
            foco: focoClave,
            ayuda: TextosDelAlta.claveAyuda,
            ejemplo: TextosDelAlta.ejemploContrasena,
            error: errorRemoto ?? errorClave,
            onChanged: onCambioClave,
          ),
          if (errorRemoto != null) ReferenciaDeError(trazaId: trazaId),
          const SizedBox(height: Espacio.s4),
          CampoContrasena(
            etiqueta: TextosDelAlta.claveRepetir,
            ejemplo: TextosDelAlta.ejemploRepetirContrasena,
            controlador: repetida,
            error: errorRepetida,
            onChanged: onCambioRepetida,
          ),
          const SizedBox(height: Espacio.s4),
          const Alerta(
            tono: Tono.info,
            titulo: TextosDelAlta.claveAvisoTitulo,
            detalle: TextosDelAlta.claveAvisoDetalle,
          ),
          if (errorCodigo != null) ...[
            const SizedBox(height: Espacio.s4),
            Alerta(tono: Tono.error, titulo: errorCodigo!),
            ReferenciaDeError(trazaId: trazaId),
          ],
          const SizedBox(height: Espacio.s5),
          Boton(
            texto: TextosIdentidad.continuar,
            variante: BotonVariante.primario,
            expandido: true,
            cargando: cargando,
            onPressed: onContinuar,
          ),
        ],
      ),
    ],
  );
}
