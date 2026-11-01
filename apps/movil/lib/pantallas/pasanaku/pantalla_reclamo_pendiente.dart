import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/intro_de_pantalla.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:flutter/material.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';

import 'textos.dart';
import 'textos_de_verificacion_del_grupo.dart';

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
    return Scaffold(
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      body: PantallaConCabecera(
        titulo: TextosPasanaku.tituloReclamo,
        cuerpo: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            mainAxisSize: MainAxisSize.min,
            children: [
              const IntroDePantalla(
                icono: IconosDeMarca.ayuda,
                titulo: TextosDeVerificacionDelGrupo.reclamoTitulo,
                explicacion: TextosPasanaku.contratoPendienteHueco,
              ),
              Tarjeta(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      TextosDeVerificacionDelGrupo.reclamoInstancias,
                      style: Tipo.cuerpoFuerte.copyWith(color: t.text),
                    ),
                    const SizedBox(height: Espacio.s1),
                    Text(
                      TextosPasanaku.segundaInstanciaDisponible,
                      style: Tipo.cuerpoChico.copyWith(color: t.text2),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
