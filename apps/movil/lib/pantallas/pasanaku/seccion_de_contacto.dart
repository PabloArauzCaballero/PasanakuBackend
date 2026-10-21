import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/motivo_vacio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'textos_perfil_del_integrante.dart';

/// El teléfono del integrante. Si el servidor no lo muestra (no comparten el
/// grupo), lo dice; si falló la red, ofrece reintentar.
class SeccionDeContacto extends StatelessWidget {
  const SeccionDeContacto({
    super.key,
    required this.valor,
    required this.reintentar,
  });

  final AsyncValue<ContactoEnGrupo?> valor;
  final VoidCallback reintentar;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const TituloDeSeccion(titulo: TextosPerfilDelIntegrante.contacto),
        EstadoDePantalla<ContactoEnGrupo?>(
          valor: valor,
          etiquetaDeCarga: TextosPerfilDelIntegrante.cargando,
          vacio: (c) => c == null,
          motivoVacio: MotivoVacio.porPermiso,
          mensajeVacio: TextosPerfilDelIntegrante.soloMismoGrupo,
          reintentar: reintentar,
          exito: (c) => Panel(
            hijo: FilaDelPerfil(
              etiqueta: TextosPerfilDelIntegrante.telefono,
              valor: c!.telefonoE164,
            ),
          ),
        ),
      ],
    );
  }
}

/// Una fila etiqueta / valor dentro de un [Panel]: la etiqueta a la izquierda y el
/// valor alineado a la derecha. Se lee como una sola frase con el lector de pantalla.
class FilaDelPerfil extends StatelessWidget {
  const FilaDelPerfil({super.key, required this.etiqueta, required this.valor});

  final String etiqueta;
  final String valor;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return MergeSemantics(
      child: Row(
        children: [
          Expanded(
            child: Text(etiqueta, style: Tipo.cuerpo.copyWith(color: t.text2)),
          ),
          const SizedBox(width: Espacio.s2),
          Expanded(
            child: Text(
              valor,
              textAlign: TextAlign.end,
              style: Tipo.cuerpoFuerte.copyWith(color: t.text),
            ),
          ),
        ],
      ),
    );
  }
}

/// Una insignia ganada: el nombre y, debajo, qué hizo la persona para ganarla.
class FilaDeInsignia extends StatelessWidget {
  const FilaDeInsignia({
    super.key,
    required this.nombre,
    required this.descripcion,
  });

  final String nombre;
  final String descripcion;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    // Todo el ancho: [FilasDePanel] centra a sus hijos y una columna angosta quedaba corrida.
    return MergeSemantics(
      child: SizedBox(
        width: double.infinity,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(nombre, style: Tipo.cuerpoFuerte.copyWith(color: t.text)),
            Text(descripcion, style: Tipo.cuerpoChico.copyWith(color: t.text2)),
          ],
        ),
      ),
    );
  }
}
