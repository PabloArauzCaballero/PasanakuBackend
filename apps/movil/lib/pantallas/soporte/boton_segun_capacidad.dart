import 'package:flutter/material.dart';

import '../../infraestructura/capacidades.dart';

/// Un botón que se deshabilita solo cuando el puerto que necesita **no está
/// soportado** en esta plataforma, con su motivo accesible (H2.S2.M2, madre
/// H6.S2.M2): un lector de pantalla lo anuncia por `Semantics.hint`, y la persona
/// que ve la pantalla lo lee como texto bajo el botón. `degradado` NO deshabilita —
/// degradado significa "funciona, con una limitación conocida", no "no funciona".
///
/// Vive en `pantallas/soporte/` porque es un componente compartido entre pantallas
/// (billetera, identidad, ajustes), no una regla de negocio de ninguna en
/// particular — el mismo lugar donde ya vive `capa_de_tutorial.dart`.
class BotonSegunCapacidad extends StatelessWidget {
  const BotonSegunCapacidad({
    super.key,
    required this.grado,
    required this.etiqueta,
    required this.motivoSiNoSoportado,
    required this.alPresionar,
    this.icono,
  });

  final GradoDeSoporte grado;
  final String etiqueta;
  final String motivoSiNoSoportado;
  final VoidCallback alPresionar;
  final IconData? icono;

  bool get _soportado => grado != GradoDeSoporte.noSoportado;

  @override
  Widget build(BuildContext context) {
    final boton = Semantics(
      hint: _soportado ? null : motivoSiNoSoportado,
      child: FilledButton.icon(
        onPressed: _soportado ? alPresionar : null,
        icon: icono == null ? const SizedBox.shrink() : Icon(icono),
        label: Text(etiqueta),
      ),
    );
    if (_soportado) return boton;
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        boton,
        Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Text(
            motivoSiNoSoportado,
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ),
      ],
    );
  }
}
