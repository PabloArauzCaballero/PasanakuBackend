import 'package:aportaya_diseno/atomos/campo_busqueda.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moleculas/chips_de_filtro.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/organismos/motivo_vacio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../dominio/tutoriales/avance.dart';
import '../../dominio/tutoriales/modelo.dart';
import '../../proveedores/tutoriales.dart';
import 'resumen_de_avance.dart';
import 'tarjeta_de_tutorial.dart';
import 'textos.dart';
import 'vista_de_tutoriales.dart';

/// **El centro de tutoriales.** Lista lo que esta persona puede aprender, con su
/// avance, y lanza el recorrido sobre la app de verdad: al tocar «Comenzar» el motor
/// navega a la pantalla del tutorial y el globo aparece allá, no acá.
class PantallaCentroDeAyuda extends ConsumerStatefulWidget {
  const PantallaCentroDeAyuda({super.key});

  @override
  ConsumerState<PantallaCentroDeAyuda> createState() =>
      _PantallaCentroDeAyudaState();
}

class _PantallaCentroDeAyudaState extends ConsumerState<PantallaCentroDeAyuda> {
  final _busqueda = TextEditingController();
  FiltroDeEstado _filtro = FiltroDeEstado.todos;

  @override
  void initState() {
    super.initState();
    _busqueda.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _busqueda.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final disponibles = ref.watch(registroDeTutorialesProvider).disponibles;
    final progresos = ref.watch(progresoDeTutorialesProvider);
    final lista = componerLista(
      disponibles,
      progresos,
      texto: _busqueda.text,
      filtro: _filtro,
    );
    // El sugerido se esconde mientras se busca o se filtra: quien escribe «zzz» y no
    // encuentra nada tiene que ver que no encontró nada, no una tarjeta que no pidió.
    final hayFiltro =
        _busqueda.text.trim().isNotEmpty || _filtro != FiltroDeEstado.todos;
    final sugerido = hayFiltro
        ? null
        : recomendado(componerLista(disponibles, progresos));
    // Y no se repite abajo: ya está arriba, con su propia tarjeta.
    final resto = lista
        .where((f) => f.tutorial.id != sugerido?.tutorial.id)
        .toList();
    final hechos = disponibles
        .where(
          (d) => estadoDe(progresos[d.id], d) == EstadoDeProgreso.completado,
        )
        .length;

    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(
            Espacio.s4,
            0,
            Espacio.s4,
            Espacio.s6,
          ),
          children: [
            const CabeceraDeInicio(titulo: TextosSoporte.titulo),
            Text(
              TextosSoporte.proposito,
              style: Tipo.cuerpoChico.copyWith(color: t.text2),
            ),
            const SizedBox(height: Espacio.s4),
            MarcaDeTutorial(
              id: 'ayuda.avance',
              hijo: ResumenDeAvance(
                hechos: hechos,
                total: disponibles.length,
                fraccion: avanceGeneral(disponibles, progresos),
              ),
            ),
            const SizedBox(height: Espacio.s4),
            MarcaDeTutorial(
              id: 'ayuda.buscador',
              hijo: CampoBusqueda(
                controlador: _busqueda,
                etiqueta: TextosSoporte.buscar,
              ),
            ),
            const SizedBox(height: Espacio.s3),
            ChipsDeFiltro<FiltroDeEstado>(
              opciones: const [
                (
                  clave: FiltroDeEstado.todos,
                  texto: TextosSoporte.todos,
                  icono: Icons.list_alt,
                ),
                (
                  clave: FiltroDeEstado.pendiente,
                  texto: TextosSoporte.pendientes,
                  icono: Icons.circle_outlined,
                ),
                (
                  clave: FiltroDeEstado.enProgreso,
                  texto: TextosSoporte.enProgreso,
                  icono: Icons.play_circle_outline,
                ),
                (
                  clave: FiltroDeEstado.completado,
                  texto: TextosSoporte.completados,
                  icono: Icons.check_circle_outline,
                ),
              ],
              valor: _filtro,
              onChanged: (v) => setState(() => _filtro = v),
            ),
            if (sugerido != null) ...[
              const SizedBox(height: Espacio.s5),
              const TituloDeSeccion(titulo: TextosSoporte.recomendado),
              _Tarjeta(fila: sugerido),
            ],
            const SizedBox(height: Espacio.s5),
            MarcaDeTutorial(
              id: 'ayuda.lista',
              hijo: resto.isEmpty && sugerido == null
                  ? EstadoVacio(
                      motivo: disponibles.isEmpty
                          ? MotivoVacio.sinDatos
                          : MotivoVacio.porFiltro,
                      mensaje: disponibles.isEmpty
                          ? TextosSoporte.sinTutoriales
                          : TextosSoporte.sinResultados,
                    )
                  : Column(
                      children: [
                        for (final fila in resto) ...[
                          _Tarjeta(fila: fila),
                          const SizedBox(height: Espacio.s3),
                        ],
                      ],
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Tarjeta extends ConsumerWidget {
  const _Tarjeta({required this.fila});

  final TutorialEnLista fila;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final motor = ref.read(motorDeTutorialesProvider.notifier);
    final progresos = ref.read(progresoDeTutorialesProvider.notifier);
    return TarjetaDeTutorial(
      fila: fila,
      alComenzar: () => motor.iniciar(
        fila.tutorial.id,
        repeticiones: fila.progreso?.repeticiones ?? 0,
      ),
      alContinuar: () => fila.progreso == null
          ? motor.iniciar(fila.tutorial.id)
          : motor.continuar(fila.tutorial.id, fila.progreso!),
      alReiniciar: () => progresos.reiniciar(fila.tutorial.id),
    );
  }
}
