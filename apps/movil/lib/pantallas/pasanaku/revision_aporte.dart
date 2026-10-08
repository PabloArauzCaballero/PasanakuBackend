import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../../dominio/error_de_operacion_de_dinero.dart';
import 'dominio/clave_persistida_de_aporte.dart';
import 'monto_fijo_aporte.dart';
import 'textos.dart';

/// Revisión previa al POST: el servidor aún no recibió ningún aporte.
class RevisionAporte extends StatelessWidget {
  const RevisionAporte({
    super.key,
    required this.monto,
    required this.medio,
    required this.referencia,
    required this.cargando,
    required this.envioIniciado,
    required this.error,
    required this.operacionPausada,
    required this.avisoConexion,
    required this.onConfirmar,
    required this.onEditar,
  });

  final String monto;
  final String medio;
  final String referencia;
  final bool cargando;
  final bool envioIniciado;
  final Object? error;
  final bool operacionPausada;
  final Widget? avisoConexion;
  final VoidCallback onConfirmar;
  final VoidCallback? onEditar;

  @override
  Widget build(BuildContext context) => CustomScrollView(
    slivers: [
      SliverPadding(
        padding: const EdgeInsets.all(Espacio.s4),
        sliver: SliverFillRemaining(
          hasScrollBody: false,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (avisoConexion != null) ...[
                avisoConexion!,
                const SizedBox(height: Espacio.s4),
              ],
              MontoFijoAporte(monto: monto),
              const SizedBox(height: Espacio.s4),
              _DatoDeRevision(
                etiqueta: TextosPasanaku.aporteMedioResumen,
                valor: medio,
              ),
              const SizedBox(height: Espacio.s3),
              _DatoDeRevision(
                etiqueta: TextosPasanaku.aporteReferencia,
                valor: referencia,
              ),
              const SizedBox(height: Espacio.s4),
              Text(
                !envioIniciado
                    ? TextosPasanaku.aporteRevisionAyuda
                    : cargando
                    ? TextosPasanaku.aporteEnviandoAyuda
                    : TextosPasanaku.aporteReintentoAyuda,
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              const SizedBox(height: Espacio.s4),
              if (error != null)
                EstadoError(
                  error: error!,
                  reintentar:
                      operacionPausada ||
                          error is ErrorDeOperacionPendiente ||
                          error is ErrorDeAportePendiente
                      ? null
                      : onConfirmar,
                )
              else ...[
                const Spacer(),
                Boton(
                  texto: cargando
                      ? TextosPasanaku.aportando
                      : TextosPasanaku.confirmarAporte,
                  variante: BotonVariante.primario,
                  expandido: true,
                  cargando: cargando,
                  onPressed: operacionPausada ? null : onConfirmar,
                ),
              ],
              if (onEditar != null && !cargando) ...[
                const SizedBox(height: Espacio.s2),
                Boton(
                  texto: TextosPasanaku.cambiarDatosAporte,
                  variante: BotonVariante.enlace,
                  expandido: true,
                  onPressed: onEditar,
                ),
              ],
            ],
          ),
        ),
      ),
    ],
  );
}

class _DatoDeRevision extends StatelessWidget {
  const _DatoDeRevision({required this.etiqueta, required this.valor});

  final String etiqueta;
  final String valor;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(etiqueta, style: Theme.of(context).textTheme.labelLarge),
      const SizedBox(height: Espacio.s1),
      Text(valor, style: Theme.of(context).textTheme.bodyLarge),
    ],
  );
}
