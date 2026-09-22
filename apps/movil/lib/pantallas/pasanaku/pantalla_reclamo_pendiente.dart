import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// CU-52/53 · reclamo. **Hueco declarado, no un olvido:** `cumplimiento` reserva el
/// prefijo `/reclamos` (comentario de cabecera de su OpenAPI) y hasta documenta los
/// códigos `AP-CU52-0x`/`AP-CU53-0x`, pero **no publica ninguna operación** bajo ese
/// prefijo — ni para crear un reclamo, ni para leer su plazo guardado o su
/// correlativo. Programar contra una forma inventada violaría la regla cero (no se
/// inventa lo que no está en el contrato); por eso esta pantalla es un aviso, no un
/// formulario que llama a un endpoint que no existe. Pedido de contrato anotado en
/// `planes/informes/carril-M3.md`, primero en la lista de lo que sigue.
class PantallaReclamoPendiente extends StatelessWidget {
  const PantallaReclamoPendiente({super.key});

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloReclamo)),
      body: Padding(
        padding: const EdgeInsets.all(Espacio.s4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              TextosPasanaku.contratoPendienteHueco,
              style: texto.bodyLarge?.copyWith(color: t.text),
            ),
            const SizedBox(height: Espacio.s3),
            Text(
              TextosPasanaku.segundaInstanciaDisponible,
              style: texto.bodyMedium?.copyWith(color: t.text2),
            ),
          ],
        ),
      ),
    );
  }
}
