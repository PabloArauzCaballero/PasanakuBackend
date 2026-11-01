import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_diseno/moleculas/intro_de_pantalla.dart';
import 'package:aportaya_diseno/moleculas/referencia_de_error.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/organismos/estado_vacio.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';

import 'dominio/cu02_actualizar_fecha_expiracion.dart';
import 'dominio/capturas_del_expediente.dart';
import 'dominio/cu02_subir_foto.dart';
import 'dominio/kyc_del_titular.dart';
import 'dominio/estado_sesion.dart' show mensajeDeError, trazaDeError;
import 'foto_pendiente_kyc.dart';
import 'pasos_alta/sacar_captura.dart';
import 'texto_completar_kyc.dart';

/// Edición de evidencia propia desde Perfil. No permite cambiar el estado ni aprobarse.
class PantallaCompletarKyc extends ConsumerStatefulWidget {
  const PantallaCompletarKyc({super.key, required this.usuarioId});
  final String usuarioId;

  @override
  ConsumerState<PantallaCompletarKyc> createState() =>
      _PantallaCompletarKycState();
}

class _PantallaCompletarKycState extends ConsumerState<PantallaCompletarKyc> {
  DateTime? _fecha;
  bool _guardandoFecha = false;
  final Set<CaraDelCarril> _subiendo = {};
  String? _error, _trazaId;

  void _mostrarError(Object error, {required String mensajeDesconocido}) {
    if (!mounted) return;
    setState(() {
      _error = mensajeDeError(error, mensajeDesconocido: mensajeDesconocido);
      _trazaId = trazaDeError(error);
    });
  }

  Future<void> _guardarFecha() async {
    final fecha = _fecha;
    if (fecha == null || _guardandoFecha) return;
    setState(() {
      _guardandoFecha = true;
      _error = _trazaId = null;
    });
    try {
      await ref
          .read(actualizarFechaExpiracionDocumentoProvider)
          .ejecutar(usuarioId: widget.usuarioId, fecha: fecha);
      ref.invalidate(kycDelTitularProvider(widget.usuarioId));
    } on Object catch (error) {
      _mostrarError(error, mensajeDesconocido: TextoCompletarKyc.errorFecha);
    } finally {
      if (mounted) setState(() => _guardandoFecha = false);
    }
  }

  Future<void> _capturar(CaraDelCarril cara) async {
    if (_subiendo.contains(cara)) return;
    final resultado = await sacarCaptura(context, cara, listas: {cara});
    if (!mounted || resultado is! Map<CaraDelCarril, Captura>) return;
    final captura = resultado[cara];
    if (captura == null) return;
    setState(() {
      _subiendo.add(cara);
      _error = _trazaId = null;
    });
    try {
      await ref
          .read(subidaDeFotosProvider)
          .subir(
            usuarioId: widget.usuarioId,
            cara: cara,
            rutaLocal: captura.ruta,
            formularioId: 'kyc-completar-${cara.name}',
          );
      await captura.eliminarArchivoLocal();
      ref.invalidate(kycDelTitularProvider(widget.usuarioId));
    } on Object catch (error) {
      _mostrarError(error, mensajeDesconocido: TextoCompletarKyc.errorFoto);
    } finally {
      if (mounted) setState(() => _subiendo.remove(cara));
    }
  }

  @override
  Widget build(BuildContext context) {
    final kyc = kycDelTitularProvider(widget.usuarioId);
    return Scaffold(
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      body: PantallaConCabecera(
        titulo: TextoCompletarKyc.titulo,
        cuerpo: SafeArea(
          top: false,
          child: EstadoDePantalla<EstadoDeVerificacion>(
            valor: ref.watch(kyc),
            etiquetaDeCarga: TextoCompletarKyc.cargando,
            mensajeVacio: TextoCompletarKyc.revisionTerminada,
            reintentar: () => ref.invalidate(kyc),
            exito: _contenido,
          ),
        ),
      ),
    );
  }

  Widget _contenido(EstadoDeVerificacion expediente) {
    if (!kycAdmiteCorreccion(expediente)) {
      return const EstadoVacio(
        icono: IconosDeMarca.expediente,
        titulo: TextoCompletarKyc.revisionTerminadaTitulo,
        mensaje: TextoCompletarKyc.revisionTerminada,
      );
    }
    final fotos = expediente.fotos;
    final faltanIzquierda = !fotos.contains(CaraDelExpediente.PERFIL_IZQUIERDO);
    final faltanDerecha = !fotos.contains(CaraDelExpediente.PERFIL_DERECHO);
    final faltaFecha = expediente.fechaExpiracionDocumento == null;
    final hoy = DateTime.now();
    final fechaVisible = _fecha ?? expediente.fechaExpiracionDocumento;
    final seccion = Tipo.titulo3.copyWith(color: Tokens.of(context).text);
    return ListView(
      padding: const EdgeInsets.all(Espacio.s4),
      children: [
        const IntroDePantalla(
          icono: IconosDeMarca.expediente,
          titulo: TextoCompletarKyc.introTitulo,
          explicacion: TextoCompletarKyc.introExplicacion,
        ),
        if (faltanIzquierda || faltanDerecha) ...[
          const SizedBox(height: Espacio.s5),
          Text(TextoCompletarKyc.fotosQueFaltan, style: seccion),
          const SizedBox(height: Espacio.s3),
          FotosPendientesKyc(
            caras: [
              if (faltanIzquierda) CaraDelCarril.perfilIzquierdo,
              if (faltanDerecha) CaraDelCarril.perfilDerecho,
            ],
            subiendo: _subiendo,
            onCapturar: _capturar,
          ),
        ],
        if (faltaFecha || fechaVisible != null) ...[
          const SizedBox(height: Espacio.s5),
          Text(TextoCompletarKyc.tuDocumento, style: seccion),
          const SizedBox(height: Espacio.s3),
          Tarjeta(
            child: CampoDeFecha(
              etiqueta: TextoCompletarKyc.vencimiento,
              valor: fechaVisible,
              primera: DateTime(1950),
              ultima: DateTime(2100, 12, 31),
              inicial: DateTime(hoy.year + 1, hoy.month, hoy.day),
              ayuda: TextoCompletarKyc.ayudaVencimiento,
              icono: Icons.event_available_outlined,
              onElegida: (fecha) => setState(() => _fecha = fecha),
            ),
          ),
          // El error queda pegado al botón que se tocó, no debajo del pliegue.
          if (_error != null) ...[
            const SizedBox(height: Espacio.s3),
            Alerta(tono: Tono.error, titulo: _error!),
            ReferenciaDeError(trazaId: _trazaId),
          ],
          const SizedBox(height: Espacio.s3),
          Boton(
            texto: _guardandoFecha
                ? TextoCompletarKyc.guardandoFecha
                : TextoCompletarKyc.guardarFecha,
            variante: BotonVariante.primario,
            expandido: true,
            cargando: _guardandoFecha,
            motivoDeBloqueo: TextoCompletarKyc.elegiFecha,
            onPressed: _fecha == null ? null : _guardarFecha,
          ),
        ],
        if (!faltanIzquierda && !faltanDerecha && !faltaFecha) ...[
          const SizedBox(height: Espacio.s4),
          const Alerta(
            tono: Tono.ok,
            titulo: TextoCompletarKyc.todoEnviado,
            detalle: TextoCompletarKyc.todoEnviadoDetalle,
          ),
        ],
      ],
    );
  }
}
