import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../dominio/cliente.dart';
import '../../proveedores/idempotencia.dart';
import '../../proveedores/sesion.dart';

/// Entrada pública del enlace. Los datos del grupo llegan solo tras comprobar en
/// el servidor que el teléfono autenticado es el destinatario de la invitación.
class PantallaUnirse extends ConsumerStatefulWidget {
  const PantallaUnirse({super.key, required this.codigo});
  final String codigo;

  @override
  ConsumerState<PantallaUnirse> createState() => _PantallaUnirseState();
}

class _PantallaUnirseState extends ConsumerState<PantallaUnirse> {
  static final _formato = RegExp(
    r'^([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\.([0-9a-f]{64})$',
  );

  bool _cargando = true;
  bool _necesitaIngreso = false;
  bool _aceptaReglamento = false;
  bool _enviando = false;
  String? _error;
  Map<String, dynamic>? _detalle;
  String? _grupoAceptado;

  RegExpMatch? get _partes => _formato.firstMatch(widget.codigo);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _consultar());
  }

  Future<void> _consultar() async {
    final partes = _partes;
    if (partes == null) {
      setState(() {
        _cargando = false;
        _error = 'El enlace de invitación no es válido.';
      });
      return;
    }
    String? acceso;
    try {
      acceso = await ref.read(sesionProvider).tokenDeAcceso();
    } on Object {
      acceso = null;
    }
    if (!mounted) return;
    if (acceso == null || acceso.isEmpty) {
      setState(() {
        _cargando = false;
        _necesitaIngreso = true;
      });
      return;
    }
    try {
      final respuesta = await ref
          .read(dioProvider)
          .post<Map<String, dynamic>>(
            '/grupos/invitaciones/enlace/consultar',
            data: {'tokenId': partes.group(1), 'token': partes.group(2)},
          );
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _detalle = respuesta.data;
      });
    } on Object {
      if (!mounted) return;
      setState(() {
        _cargando = false;
        _error =
            'No pudimos verificar la invitación. Revisá el enlace o probá de nuevo.';
      });
    }
  }

  Future<void> _aceptar() async {
    if (_enviando ||
        !_aceptaReglamento ||
        _detalle == null ||
        _partes == null) {
      return;
    }
    setState(() => _enviando = true);
    final partes = _partes!;
    final formulario = 'cu69-aceptar-${partes.group(1)}';
    final clave = ref.read(idempotenciaProvider.notifier).claveDe(formulario);
    try {
      final respuesta = await ref
          .read(dioProvider)
          .post<Map<String, dynamic>>(
            '/grupos/invitaciones/enlace/aceptar',
            data: {
              'tokenId': partes.group(1),
              'token': partes.group(2),
              'hashReglamento': _detalle!['hashReglamento'],
              'aceptaReglamento': true,
            },
            options: Options(headers: {'Idempotency-Key': clave}),
          );
      ref.read(idempotenciaProvider.notifier).cerrar(formulario);
      if (!mounted) return;
      setState(() {
        _grupoAceptado = respuesta.data?['grupoId'] as String?;
        _detalle = null;
        _error = null;
      });
    } on Object {
      if (!mounted) return;
      setState(
        () => _error = 'No pudimos completar tu ingreso. Probá de nuevo.',
      );
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final retorno = Uri.encodeComponent('/pasanaku/unirse/${widget.codigo}');
    return Scaffold(
      appBar: AppBar(title: const Text('Invitación al grupo')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            if (_cargando) const Center(child: CircularProgressIndicator()),
            if (_necesitaIngreso) ...[
              const Text(
                'Ingresá con el teléfono que recibió la invitación para ver el grupo.',
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () => context.go('/ingreso?volver=$retorno'),
                child: const Text('Ingresar'),
              ),
              TextButton(
                onPressed: () => context.go('/registro?volver=$retorno'),
                child: const Text('Crear cuenta'),
              ),
            ],
            if (_detalle case final detalle?) ...[
              Text(
                detalle['nombre'] as String? ?? '',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 12),
              Text(
                '${detalle['montoAporte']} ${detalle['moneda']} por período · ${detalle['periodicidad']}',
              ),
              const SizedBox(height: 24),
              const Text('Reglamento del grupo'),
              const SizedBox(height: 8),
              Text(detalle['reglamento'] as String? ?? ''),
              CheckboxListTile(
                value: _aceptaReglamento,
                onChanged: (valor) =>
                    setState(() => _aceptaReglamento = valor ?? false),
                title: const Text('Leí y acepto este reglamento'),
                controlAffinity: ListTileControlAffinity.leading,
              ),
              ElevatedButton(
                onPressed: _aceptaReglamento && !_enviando ? _aceptar : null,
                child: const Text('Unirme al grupo'),
              ),
            ],
            if (_grupoAceptado != null) ...[
              const Text('Ya formás parte del grupo.'),
              TextButton(
                onPressed: () => context.go('/pasanaku/mi-estado'),
                child: const Text('Ver mis grupos'),
              ),
            ],
            if (_error != null) ...[
              Text(
                _error!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
              if (_detalle == null && !_necesitaIngreso)
                TextButton(
                  onPressed: _consultar,
                  child: const Text('Reintentar'),
                ),
            ],
          ],
        ),
      ),
    );
  }
}
