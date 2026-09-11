import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_contrato.dart';
import 'textos.dart';

/// CU-05 — el contrato se muestra ENTERO antes de poder aceptar. Verificable con
/// `flutter test`: `aceptar_deshabilitado_hasta_scroll_completo_test.dart` hace
/// scroll parcial y comprueba que el botón sigue deshabilitado, luego hace scroll
/// hasta el final y comprueba que se habilita.
///
/// **Hueco declarado:** el texto real del contrato y el tarifario vigente los
/// publica `servicios/cumplimiento` (CU-05 se mudó ahí en el carril 0T, según
/// `docs/CasosDeUso/CU-05`). Sin ese endpoint ni el cliente Dart generado, el
/// cuerpo es un texto de relleno con la extensión suficiente para ejercer el
/// gate de scroll; se reemplaza por el contenido real cuando el hueco se cierre.
class PantallaDeContrato extends ConsumerStatefulWidget {
  const PantallaDeContrato({super.key});

  @override
  ConsumerState<PantallaDeContrato> createState() => _PantallaDeContratoState();
}

class _PantallaDeContratoState extends ConsumerState<PantallaDeContrato> {
  final _scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_alDesplazar);
  }

  void _alDesplazar() {
    if (_scroll.position.pixels >= _scroll.position.maxScrollExtent - 8) {
      ref.read(contratoProvider.notifier).marcarLeidoHastaElFinal();
    }
  }

  @override
  void dispose() {
    _scroll.removeListener(_alDesplazar);
    _scroll.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(contratoProvider);
    final notifier = ref.read(contratoProvider.notifier);
    return Scaffold(
      appBar: AppBar(title: const Text(TextosIdentidad.tituloContrato)),
      body: SafeArea(
        child: Column(
          children: [
            if (!estado.leidoHastaElFinal)
              const Padding(
                padding: EdgeInsets.all(Espacio.s3),
                child: Text(TextosIdentidad.avisoLeerContrato),
              ),
            Expanded(
              child: Scrollbar(
                controller: _scroll,
                thumbVisibility: true,
                child: SingleChildScrollView(
                  controller: _scroll,
                  padding: const EdgeInsets.all(Espacio.s4),
                  child: const Text(_textoDeRelleno),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(Espacio.s4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Casilla(
                    etiqueta: TextosIdentidad.aceptoContrato,
                    valor: estado.aceptaContrato,
                    onChanged: estado.leidoHastaElFinal
                        ? notifier.alternarContrato
                        : null,
                  ),
                  Casilla(
                    etiqueta: TextosIdentidad.aceptoTarifario,
                    valor: estado.aceptaTarifario,
                    onChanged: estado.leidoHastaElFinal
                        ? notifier.alternarTarifario
                        : null,
                  ),
                  Casilla(
                    etiqueta: TextosIdentidad.aceptoTratamientoDatos,
                    valor: estado.aceptaTratamientoDatos,
                    onChanged: estado.leidoHastaElFinal
                        ? notifier.alternarTratamientoDatos
                        : null,
                  ),
                  const SizedBox(height: Espacio.s3),
                  Boton(
                    texto: TextosIdentidad.aceptarYContinuar,
                    variante: BotonVariante.primario,
                    expandido: true,
                    // El servidor es quien realmente registra la aceptación
                    // (CU-05); acá solo se habilita seguir. Cuando exista el
                    // cliente de cumplimiento, este onPressed llama a
                    // `POST /contratos/{id}/aceptaciones` antes de volver.
                    onPressed: estado.puedeAceptar
                        ? () => context.pop(true)
                        : null,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

const _textoDeRelleno = '''
CONTRATO DE ADHESIÓN — BILLETERA APORTAYA

[Contenido de relleno para ejercer el gate de scroll — el texto real lo publica
servicios/cumplimiento, hueco declarado en planes/informes/carril-M1.md §3.]

1. Objeto. AportaYa presta un servicio de billetera de dinero electrónico...
2. Apertura de cuenta y verificación de identidad...
3. Límites operativos por nivel de debida diligencia...
4. Comisiones y tarifario vigente, publicado y actualizable...
5. Obligaciones del usuario...
6. Obligaciones de AportaYa...
7. Tratamiento de datos personales...
8. Reclamos y resolución de controversias...
9. Terminación y baja de cuenta...
10. Legislación aplicable y jurisdicción...

[Se repite intencionalmente para simular la extensión real y forzar scroll]
''';
