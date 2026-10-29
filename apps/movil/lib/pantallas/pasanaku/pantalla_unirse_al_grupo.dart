import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/errores.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../navegacion/acceso_a_rutas.dart';
import '../../proveedores/sesion.dart';
import 'dominio/enlace_del_grupo.dart';
import 'dominio/mis_grupos.dart';
import 'textos_de_conversion.dart';
import 'textos_enlace_del_grupo.dart';

/// Quien abre el enlace del grupo (o escanea su QR) ve el grupo —sin datos de sus
/// integrantes— y pide unirse; el organizador decide (CU-69, 2026-10-19). Sin la
/// identidad verificada, se la pide primero: el servidor la exige igual (AP-CU68-02).
class PantallaUnirseAlGrupo extends ConsumerStatefulWidget {
  const PantallaUnirseAlGrupo({super.key, required this.codigo});
  final String codigo;

  @override
  ConsumerState<PantallaUnirseAlGrupo> createState() =>
      _PantallaUnirseAlGrupoState();
}

class _PantallaUnirseAlGrupoState extends ConsumerState<PantallaUnirseAlGrupo> {
  bool _pidiendo = false;
  bool _pedido = false;
  String? _error;

  Future<void> _pedir() async {
    if (_pidiendo) return;
    setState(() {
      _pidiendo = true;
      _error = null;
    });
    try {
      await ref.read(pedirUnirseProvider)(widget.codigo);
      ref.invalidate(misGruposProvider);
      if (mounted) setState(() => _pedido = true);
    } on Object catch (e) {
      if (mounted) {
        setState(
          () => _error = e is ErrorPresentable
              ? e.mensaje
              : 'No pudimos enviar tu pedido. Probá de nuevo.',
        );
      }
    } finally {
      if (mounted) setState(() => _pidiendo = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final detalle = ref.watch(detalleDelEnlaceProvider(widget.codigo));
    final requiereVerificacion = ref.watch(sesionProvider).requiereVerificacion;
    final error = _error;
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          children: [
            const CabeceraDeSeccion(titulo: TextosEnlaceDelGrupo.tituloUnirse),
            if (_pedido)
              ..._pedidoEnviado(context)
            else
              EstadoDePantalla<DetalleEnlaceDelGrupo>(
                valor: detalle,
                mensajeVacio: '',
                reintentar: () =>
                    ref.invalidate(detalleDelEnlaceProvider(widget.codigo)),
                exito: (d) => Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _TarjetaDelGrupo(detalle: d),
                    const SizedBox(height: Espacio.s4),
                    if (error != null) ...[
                      Alerta(titulo: error, tono: Tono.error),
                      const SizedBox(height: Espacio.s3),
                    ],
                    if (d.cuposLibres == 0)
                      const Alerta(
                        titulo: TextosEnlaceDelGrupo.grupoLleno,
                        tono: Tono.aviso,
                      )
                    else if (requiereVerificacion) ...[
                      const Alerta(
                        titulo: TextosEnlaceDelGrupo.verificarPrimero,
                        tono: Tono.info,
                      ),
                      const SizedBox(height: Espacio.s3),
                      Boton(
                        texto: TextosEnlaceDelGrupo.verificarMiIdentidad,
                        variante: BotonVariante.primario,
                        expandido: true,
                        onPressed: () => context.push(rutaDeVerificarIdentidad),
                      ),
                    ] else ...[
                      const Alerta(
                        titulo: TextosDeConversion.alPedirTitulo,
                        detalle: TextosEnlaceDelGrupo.avisoDeDatos,
                        tono: Tono.info,
                      ),
                      const SizedBox(height: Espacio.s3),
                      Boton(
                        key: const ValueKey('pedir-unirme'),
                        texto: TextosEnlaceDelGrupo.pedirUnirme,
                        variante: BotonVariante.primario,
                        expandido: true,
                        cargando: _pidiendo,
                        onPressed: _pedir,
                      ),
                    ],
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }

  List<Widget> _pedidoEnviado(BuildContext context) => [
    const Alerta(
      titulo: TextosEnlaceDelGrupo.pedidoEnviado,
      detalle: TextosEnlaceDelGrupo.pedidoEnviadoDetalle,
      tono: Tono.ok,
    ),
    const SizedBox(height: Espacio.s4),
    Boton(
      texto: TextosEnlaceDelGrupo.verMisGrupos,
      variante: BotonVariante.primario,
      expandido: true,
      onPressed: () => context.go('/pasanaku/mi-estado'),
    ),
  ];
}

class _TarjetaDelGrupo extends StatelessWidget {
  const _TarjetaDelGrupo({required this.detalle});
  final DetalleEnlaceDelGrupo detalle;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final monto = formatearMonto(
      monto: detalle.montoAporte,
      moneda: detalle.moneda,
    );
    return Tarjeta(
      etiqueta: 'Grupo ${detalle.nombre}',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(detalle.nombre, style: Tipo.titulo2.copyWith(color: t.text)),
          const SizedBox(height: Espacio.s2),
          Text(
            TextosEnlaceDelGrupo.aporte(monto, detalle.periodicidad),
            style: Tipo.cuerpo.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s1),
          Text(
            TextosEnlaceDelGrupo.cuposLibres(detalle.cuposLibres),
            style: Tipo.cuerpoChico.copyWith(color: t.text3),
          ),
        ],
      ),
    );
  }
}
