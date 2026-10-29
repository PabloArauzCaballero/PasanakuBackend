part of 'pantalla_sorteo.dart';

/// Lo que se ve del sorteo: la ruleta, el cartel del último puesto, el botón y el orden.
extension _VistaDelSorteo on _PantallaSorteoState {
  Widget _contenido(BuildContext context, DetalleDelGrupo d) {
    final t = Tokens.of(context);
    final error = _error;
    final nombres = {
      for (final i in d.integrantes)
        i.cupoId: i.nombre ?? TextosDetalleDelGrupo.sinNombre,
    };
    final cupos = d.integrantes.map((i) => i.cupoId).toList();
    final yo = d.integrantes
        .where((i) => i.soyYo)
        .map((i) => i.cupoId)
        .firstOrNull;
    // Sorteo ya revelado: el orden sale de los turnos del detalle.
    final yaSorteado = [...d.integrantes.where((i) => i.turno != null)]
      ..sort((a, b) => a.turno!.compareTo(b.turno!));
    final orden = _revelados.isNotEmpty || _sorteando
        ? _revelados
        : yaSorteado.map((i) => i.cupoId).toList();
    final puedeGirar =
        d.puedeSortear ||
        d.sorteo?.estado == SorteoDelDetalleEstadoEnum.COMPROMETIDO;
    // Durante la ceremonia, el cartel es el último puesto; al terminar, el propio.
    final terminado = _ceremonia && !_sorteando && _revelados.contains(yo);
    final ultimo = terminado
        ? yo
        : (_ceremonia && _revelados.isNotEmpty ? _revelados.last : null);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          TextosDetalleDelGrupo.sorteoAyuda,
          style: Tipo.cuerpoChico.copyWith(color: t.text2),
        ),
        const SizedBox(height: Espacio.s5),
        LayoutBuilder(
          builder: (context, limites) => Center(
            child: ListenableBuilder(
              listenable: _orquesta,
              builder: (context, _) {
                // Durante la ceremonia, solo quienes faltan: quien sale se va de la
                // ruleta. Con el orden completo, se rearma con todos en su puesto.
                final enCeremonia = _ceremonia && _restantes.isNotEmpty;
                final completo =
                    !enCeremonia &&
                    orden.isNotEmpty &&
                    orden.length == cupos.length;
                final enRueda = enCeremonia
                    ? _restantes
                    : (completo ? orden : cupos);
                return RuletaDelSorteo(
                  nombres: [for (final c in enRueda) nombres[c]!],
                  colores: [for (final c in enRueda) cupos.indexOf(c)],
                  diametro: math.min(limites.maxWidth * 0.9, 340),
                  angulo: completo ? 0 : _orquesta.angulo,
                  puestos: completo
                      ? {for (var k = 0; k < orden.length; k++) k: k + 1}
                      : const {},
                  ganador: enCeremonia ? _orquesta.ganador : null,
                  resalte: _orquesta.resalte,
                  salida: _orquesta.salida,
                  luces: _orquesta.luces,
                  todas: _orquesta.todas,
                  aleta: _orquesta.aleta,
                  centro: _enJuego == null ? null : '$_enJuego.º',
                );
              },
            ),
          ),
        ),
        const SizedBox(height: Espacio.s5),
        if (ultimo != null) ...[
          AnuncioDelSorteo(
            puesto: _revelados.indexOf(ultimo) + 1,
            nombre: nombres[ultimo] ?? TextosDetalleDelGrupo.sinNombre,
            soyYo: yo == ultimo,
            gajo: cupos.indexOf(ultimo),
          ),
          const SizedBox(height: Espacio.s4),
        ],
        if (error != null) ...[
          Alerta(titulo: error, tono: Tono.error),
          const SizedBox(height: Espacio.s3),
        ],
        if (puedeGirar && orden.isEmpty)
          Boton(
            key: const ValueKey('girar-ruleta'),
            texto: _sorteando
                ? TextosDetalleDelGrupo.girando
                : TextosDetalleDelGrupo.girar,
            variante: BotonVariante.primario,
            expandido: true,
            cargando: _sorteando,
            onPressed: () => _girar(d),
          ),
        if (orden.isNotEmpty)
          _OrdenConCierre(
            puestos: [
              for (final cupo in orden)
                (
                  nombre: nombres[cupo] ?? TextosDetalleDelGrupo.sinNombre,
                  soyYo: yo == cupo,
                  gajo: cupos.indexOf(cupo),
                ),
            ],
            sorteando: _sorteando,
          ),
        const SizedBox(height: Espacio.s5),
      ],
    );
  }
}
