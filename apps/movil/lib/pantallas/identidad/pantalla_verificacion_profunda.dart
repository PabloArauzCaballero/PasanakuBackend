import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/grupo_radio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/moleculas/intro_de_pantalla.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';

import 'dominio/estado_cuenta.dart';
import 'textos.dart';
import 'textos_de_tramites.dart';

/// CU-02 (elevar nivel de debida diligencia) + CU-03 (declaración PEP), una sola
/// pantalla como marca `planes/12` fila 2.8 de la maqueta: la elevación de nivel
/// la dispara el servidor cuando corresponde (por ejemplo, un monto declarado alto
/// en el perfil transaccional); acá solo se recoge la declaración PEP, que es lo
/// único con forma propia en la bóveda para esta pantalla.
class PantallaDeVerificacionProfunda extends ConsumerStatefulWidget {
  const PantallaDeVerificacionProfunda({super.key});

  @override
  ConsumerState<PantallaDeVerificacionProfunda> createState() =>
      _PantallaDeVerificacionProfundaState();
}

class _PantallaDeVerificacionProfundaState
    extends ConsumerState<PantallaDeVerificacionProfunda> {
  bool? _esPep;
  final _detalle = TextEditingController();

  @override
  void dispose() {
    _detalle.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      // La acción, anclada abajo como en pedir cupo: dentro del cuerpo, para que
      // suba con el teclado mientras se escribe.
      body: Column(
        children: [
          Expanded(
            child: PantallaConCabecera(
              titulo: 'Verificación adicional',
              cuerpo: SafeArea(
                top: false,
                // Sin desplazamiento propio, la cabecera no se compactaría.
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(Espacio.s4),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const IntroDePantalla(
                        icono: IconosDeMarca.expediente,
                        titulo: TextosDeTramites.pepTitulo,
                        explicacion: TextosDeTramites.pepExplicacion,
                      ),
                      Tarjeta(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            Text(
                              TextosIdentidad.preguntaPep,
                              style: Tipo.cuerpoFuerte.copyWith(
                                color: Tokens.of(context).text,
                              ),
                            ),
                            const SizedBox(height: Espacio.s2),
                            // El radio del sistema, sin opción marcada de antemano: en una
                            // declaración de cumplimiento la respuesta la elige la persona.
                            GrupoRadio<bool>(
                              opciones: const {
                                false: TextosDeTramites.pepNo,
                                true: TextosDeTramites.pepSi,
                              },
                              valor: _esPep,
                              onChanged: (v) => setState(() => _esPep = v),
                            ),
                            if (_esPep == true) ...[
                              const SizedBox(height: Espacio.s3),
                              Campo(
                                etiqueta: TextosIdentidad.detallePep,
                                controlador: _detalle,
                                lineas: 3,
                              ),
                            ],
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
          BarraDeAccion(
            hijo: Boton(
              texto: TextosDeTramites.pepConfirmar,
              variante: BotonVariante.primario,
              expandido: true,
              motivoDeBloqueo: _esPep == null
                  ? TextosDeTramites.pepBloqueo
                  : null,
              onPressed: _esPep == null
                  ? null
                  : () => ref
                        .read(pepProvider.notifier)
                        .declarar(esPep: _esPep!, detalle: _detalle.text),
            ),
          ),
        ],
      ),
    );
  }
}
