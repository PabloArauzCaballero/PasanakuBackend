import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_diseno/moleculas/referencia_de_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

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
  String? _error;
  String? _trazaId;

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
      _error = null;
      _trazaId = null;
    });
    try {
      await ref
          .read(actualizarFechaExpiracionDocumentoProvider)
          .ejecutar(usuarioId: widget.usuarioId, fecha: fecha);
      ref.invalidate(kycDelTitularProvider(widget.usuarioId));
    } on Object catch (error) {
      _mostrarError(
        error,
        mensajeDesconocido:
            'No pudimos guardar la fecha del documento. Probá de nuevo en un momento.',
      );
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
      _error = null;
      _trazaId = null;
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
      _mostrarError(
        error,
        mensajeDesconocido:
            'No pudimos subir la foto. Probá de nuevo en un momento.',
      );
    } finally {
      if (mounted) setState(() => _subiendo.remove(cara));
    }
  }

  @override
  Widget build(BuildContext context) {
    final resultado = ref.watch(kycDelTitularProvider(widget.usuarioId));
    return Scaffold(
      appBar: AppBar(title: const Text('Completar verificación KYC')),
      body: SafeArea(
        child: resultado.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, _) => Center(
            child: TextButton(
              onPressed: () =>
                  ref.invalidate(kycDelTitularProvider(widget.usuarioId)),
              child: const Text('No pudimos cargar tu expediente. Reintentar'),
            ),
          ),
          data: _contenido,
        ),
      ),
    );
  }

  Widget _contenido(EstadoDeVerificacion expediente) {
    if (!kycAdmiteCorreccion(expediente)) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(Espacio.s4),
          child: Text(
            'La revisión ya terminó. No se pueden modificar estos datos desde aquí.',
          ),
        ),
      );
    }
    final faltanIzquierda = !expediente.fotos.contains(
      CaraDelExpediente.PERFIL_IZQUIERDO,
    );
    final faltanDerecha = !expediente.fotos.contains(
      CaraDelExpediente.PERFIL_DERECHO,
    );
    final faltaFecha = expediente.fechaExpiracionDocumento == null;
    final hoy = DateTime.now();
    final fechaVisible = _fecha ?? expediente.fechaExpiracionDocumento;
    final todoCompleto = !faltanIzquierda && !faltanDerecha && !faltaFecha;
    return ListView(
      padding: const EdgeInsets.all(Espacio.s4),
      children: [
        Text(
          'Completá solo lo que falta. La aprobación la decide una persona después de revisar el documento.',
          style: Theme.of(context).textTheme.bodyLarge,
        ),
        const SizedBox(height: Espacio.s3),
        if (faltanIzquierda)
          FotoPendienteKyc(
            titulo: TextoCompletarKyc.perfilIzquierdo,
            cargando: _subiendo.contains(CaraDelCarril.perfilIzquierdo),
            onPressed: () => _capturar(CaraDelCarril.perfilIzquierdo),
          ),
        if (faltanDerecha)
          FotoPendienteKyc(
            titulo: TextoCompletarKyc.perfilDerecho,
            cargando: _subiendo.contains(CaraDelCarril.perfilDerecho),
            onPressed: () => _capturar(CaraDelCarril.perfilDerecho),
          ),
        if (faltaFecha || fechaVisible != null) ...[
          const SizedBox(height: Espacio.s3),
          CampoDeFecha(
            etiqueta: 'Vencimiento del documento',
            valor: fechaVisible,
            primera: DateTime(1950),
            ultima: DateTime(2100, 12, 31),
            inicial: DateTime(hoy.year + 1, hoy.month, hoy.day),
            ayuda: 'Ingresá la fecha que aparece en tu documento.',
            icono: Icons.event_available_outlined,
            onElegida: (fecha) => setState(() => _fecha = fecha),
          ),
          const SizedBox(height: Espacio.s2),
          Boton(
            texto: _guardandoFecha ? 'Guardando fecha…' : 'Guardar fecha',
            variante: BotonVariante.primario,
            expandido: true,
            cargando: _guardandoFecha,
            onPressed: _fecha == null ? null : _guardarFecha,
          ),
        ],
        if (_error != null) ...[
          const SizedBox(height: Espacio.s2),
          Alerta(tono: Tono.error, titulo: _error!),
          ReferenciaDeError(trazaId: _trazaId),
        ],
        if (todoCompleto) ...[
          const SizedBox(height: Espacio.s4),
          const Text(
            'Los datos faltantes están enviados. La revisión de identidad sigue siendo manual.',
          ),
        ],
      ],
    );
  }
}
