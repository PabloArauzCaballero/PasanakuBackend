import 'package:flutter/material.dart';

class FotoPendienteKyc extends StatelessWidget {
  const FotoPendienteKyc({
    super.key,
    required this.titulo,
    required this.cargando,
    required this.onPressed,
  });

  final String titulo;
  final bool cargando;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) => Card(
    child: ListTile(
      title: Text(titulo),
      subtitle: const Text('Tomá la foto real con buena luz.'),
      trailing: IconButton(
        tooltip: 'Tomar foto',
        onPressed: cargando ? null : onPressed,
        icon: cargando
            ? const CircularProgressIndicator()
            : const Icon(Icons.camera_alt_outlined),
      ),
    ),
  );
}
