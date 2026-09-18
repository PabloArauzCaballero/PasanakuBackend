import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/contratos_vigentes.dart';
import 'dominio/estado_contrato.dart';
import 'texto_del_contrato.dart';
import 'textos.dart';
import 'textos_del_alta.dart';

/// CU-05 — el contrato se muestra ENTERO antes de poder aceptar. Verificable con
/// `flutter test`: `pantalla_contrato_test.dart` hace scroll parcial y comprueba que
/// el botón sigue deshabilitado, luego hace scroll hasta el final y comprueba que se
/// habilita.
///
/// El cuerpo es el contrato de verdad (`texto_del_contrato.dart`), escrito sobre la
/// estructura de los contratos de billetera registrados en Bolivia y con el tarifario
/// que ya vive en el repo. Antes era un relleno que decía «[contenido de relleno]» en
/// la primera pantalla: se le pedía a alguien que aceptara un texto que declaraba no
/// ser un texto.
///
/// **Los ids vienen del servidor.** `GET /cumplimiento/contratos/vigentes` dice qué
/// contrato rige hoy, en qué versión y con qué hash. Sin esa consulta las tres casillas
/// no valen nada: `aceptaContratos` de CU-01 son UUID, y los ids se generan por
/// entorno. Mientras la consulta no responda, no se puede aceptar — y se dice, en vez
/// de dejar tocar un botón que iba a fallar al final.
///
/// **Hueco declarado:** el número y la fecha de registro ante ASFI viajan en la
/// respuesta (`numeroRegistro`, `fechaRegistro`) pero todavía llegan vacíos porque el
/// registro no se obtuvo. El pie muestra lo que haya y no inventa un número.
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
    final vigentes = ref.watch(contratosDelAltaProvider);

    // Lo que publica el servidor entra al estado apenas responde: es lo que convierte
    // tres casillas marcadas en tres ids que el alta puede mandar.
    ref.listen(contratosDelAltaProvider, (_, siguiente) {
      final lista = siguiente.asData?.value;
      if (lista != null) notifier.fijarVigentes(lista);
    });

    // Si la consulta falló, la pantalla ES el error: no hay contrato que aceptar y
    // dejar leer el texto igual ofrecería un botón que iba a fallar al final del alta.
    // Se reemplaza el cuerpo entero en vez de apilar un cartel arriba, que además
    // desbordaba la columna en una pantalla chica.
    final problema = switch (vigentes) {
      AsyncError(:final error) => EstadoError(
        error: error,
        reintentar: () => ref.invalidate(contratosDelAltaProvider),
      ),
      AsyncData(:final value) when !value.tieneLosDelAlta => const Alerta(
        tono: Tono.error,
        titulo: TextosDelAlta.contratosIncompletos,
      ),
      _ => null,
    };
    if (problema != null) {
      return Scaffold(
        appBar: AppBar(title: const Text(TextosIdentidad.tituloContrato)),
        body: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(Espacio.s4),
              child: problema,
            ),
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text(TextosIdentidad.tituloContrato)),
      body: SafeArea(
        child: Column(
          children: [
            // Mientras la consulta viaja, el texto ya se puede ir leyendo: lo único
            // que falta son los ids, y llegan antes de que nadie termine de deslizar.
            if (vigentes.isLoading)
              const Padding(
                padding: EdgeInsets.all(Espacio.s3),
                child: Text(TextosDelAlta.contratosCargando),
              ),
            if (!estado.leidoHastaElFinal)
              const Padding(
                padding: EdgeInsets.all(Espacio.s3),
                child: Text(TextosDelAlta.avisoLeerContrato),
              ),
            Expanded(
              child: Scrollbar(
                controller: _scroll,
                thumbVisibility: true,
                child: SingleChildScrollView(
                  controller: _scroll,
                  padding: const EdgeInsets.all(Espacio.s4),
                  child: const Text(textoDelContrato),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(Espacio.s4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Casilla(
                    etiqueta: TextosDelAlta.aceptoContrato,
                    valor: estado.aceptaContrato,
                    onChanged: estado.leidoHastaElFinal
                        ? notifier.alternarContrato
                        : null,
                  ),
                  Casilla(
                    etiqueta: TextosDelAlta.aceptoTarifario,
                    valor: estado.aceptaTarifario,
                    onChanged: estado.leidoHastaElFinal
                        ? notifier.alternarTarifario
                        : null,
                  ),
                  Casilla(
                    etiqueta: TextosDelAlta.aceptoTratamientoDatos,
                    valor: estado.aceptaTratamientoDatos,
                    onChanged: estado.leidoHastaElFinal
                        ? notifier.alternarTratamientoDatos
                        : null,
                  ),
                  const SizedBox(height: Espacio.s3),
                  Boton(
                    texto: TextosDelAlta.aceptarYContinuar,
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
