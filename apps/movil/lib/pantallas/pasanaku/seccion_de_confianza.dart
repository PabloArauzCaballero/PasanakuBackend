import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/motivo_vacio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'seccion_de_contacto.dart';
import 'textos_perfil_del_integrante.dart';

/// La reputación del integrante, tal como la calcula `transparencia`: puntaje,
/// nivel, puntualidad, grupos, incumplimientos, antigüedad e insignias. Sin
/// historial no hay ceros: se dice que todavía no tiene.
class SeccionDeConfianza extends StatelessWidget {
  const SeccionDeConfianza({
    super.key,
    required this.valor,
    required this.reintentar,
  });

  final AsyncValue<PerfilDeReputacionEnGrupo?> valor;
  final VoidCallback reintentar;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const TituloDeSeccion(titulo: TextosPerfilDelIntegrante.confianza),
        EstadoDePantalla<PerfilDeReputacionEnGrupo?>(
          valor: valor,
          etiquetaDeCarga: TextosPerfilDelIntegrante.cargando,
          vacio: (p) => p == null,
          motivoVacio: MotivoVacio.porPermiso,
          mensajeVacio: TextosPerfilDelIntegrante.soloMismoGrupo,
          reintentar: reintentar,
          exito: (p) => _Reputacion(perfil: p!),
        ),
      ],
    );
  }
}

class _Reputacion extends StatelessWidget {
  const _Reputacion({required this.perfil});

  final PerfilDeReputacionEnGrupo perfil;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final p = perfil;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Align(
          alignment: Alignment.centerLeft,
          child: ChipEstado(
            texto: TextosPerfilDelIntegrante.nivelDeConfianza(
              p.nivelDeConfianza,
            ),
            tono: _tonoDe(p.nivelDeConfianza),
          ),
        ),
        const SizedBox(height: Espacio.s3),
        if (!p.tieneHistorial)
          Text(
            TextosPerfilDelIntegrante.sinHistorial,
            style: Tipo.cuerpo.copyWith(color: t.text2),
          )
        else
          Panel(
            hijo: FilasDePanel(
              filas: [
                _fila(TextosPerfilDelIntegrante.puntaje, p.puntaje),
                _fila(
                  TextosPerfilDelIntegrante.puntualidad,
                  p.indicePuntualidad == null
                      ? null
                      : TextosPerfilDelIntegrante.porcentaje(
                          p.indicePuntualidad!,
                        ),
                ),
                _fila(
                  TextosPerfilDelIntegrante.completados,
                  p.gruposCompletados?.toString(),
                ),
                _fila(
                  TextosPerfilDelIntegrante.abandonados,
                  p.gruposAbandonados?.toString(),
                ),
                _fila(
                  TextosPerfilDelIntegrante.incumplimientos,
                  p.incumplimientosAbiertos?.toString(),
                ),
                _fila(
                  TextosPerfilDelIntegrante.antiguedad,
                  p.antiguedadMeses == null
                      ? null
                      : TextosPerfilDelIntegrante.meses(p.antiguedadMeses!),
                ),
              ].whereType<Widget>().toList(),
            ),
          ),
        const SizedBox(height: Espacio.s4),
        const TituloDeSeccion(titulo: TextosPerfilDelIntegrante.insignias),
        if (p.insignias.isEmpty)
          Text(
            TextosPerfilDelIntegrante.sinInsignias,
            style: Tipo.cuerpo.copyWith(color: t.text2),
          )
        else
          Panel(
            hijo: FilasDePanel(
              filas: [
                for (final i in p.insignias)
                  FilaDeInsignia(nombre: i.nombre, descripcion: i.descripcion),
              ],
            ),
          ),
        const SizedBox(height: Espacio.s3),
        Text(
          TextosPerfilDelIntegrante.queMide,
          style: Tipo.ayuda.copyWith(color: t.text3),
        ),
      ],
    );
  }

  /// Una cifra que el servidor no mandó no se inventa: la fila no aparece.
  static Widget? _fila(String etiqueta, String? valor) =>
      valor == null ? null : FilaDelPerfil(etiqueta: etiqueta, valor: valor);

  static Tono _tonoDe(String nivel) => switch (nivel) {
    'REFERENTE' || 'MUY_CONFIABLE' || 'CONFIABLE' => Tono.ok,
    'EN_OBSERVACION' => Tono.aviso,
    'RESTRINGIDO' => Tono.error,
    _ => Tono.neutro,
  };
}
