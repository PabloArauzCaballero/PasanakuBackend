import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_tamano.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/errores.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'avatar_con_aro.dart';
import 'dominio/camara_identidad.dart';
import 'dominio/foto_de_perfil.dart';

/// La foto de perfil arriba del perfil, con su acción para cambiarla.
///
/// Mientras carga o si no hay foto se ven las iniciales: la foto no es un dato que
/// bloquee nada, así que un fallo al leerla no tapa el resto del perfil. Lo que sí se
/// dice con texto es un fallo al **subirla**, porque ahí la persona hizo algo y espera
/// verlo.
class FotoDePerfilEditable extends ConsumerStatefulWidget {
  const FotoDePerfilEditable({super.key});

  @override
  ConsumerState<FotoDePerfilEditable> createState() =>
      _FotoDePerfilEditableState();
}

class _FotoDePerfilEditableState extends ConsumerState<FotoDePerfilEditable> {
  bool _subiendo = false;
  String? _error;

  Future<void> _cambiar() async {
    if (_subiendo) return;
    final origen = await showModalBottomSheet<_Origen>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              key: const ValueKey('foto-perfil-camara'),
              leading: const Icon(Icons.photo_camera_outlined),
              title: const Text('Sacar una foto'),
              onTap: () => Navigator.of(context).pop(_Origen.camara),
            ),
            ListTile(
              key: const ValueKey('foto-perfil-galeria'),
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('Elegir de mis fotos'),
              onTap: () => Navigator.of(context).pop(_Origen.galeria),
            ),
          ],
        ),
      ),
    );
    if (origen == null || !mounted) return;
    final camara = ref.read(camaraProvider);
    // La frontal: es una foto de la persona, no de un documento.
    final ruta = origen == _Origen.camara
        ? await camara.capturar(esDocumento: false)
        : await camara.elegirDeLasFotos();
    // `null` es cancelar o permiso denegado: no es un error que haya que gritar.
    if (ruta == null || !mounted) return;
    setState(() {
      _subiendo = true;
      _error = null;
    });
    try {
      await ref.read(subirFotoDePerfilProvider)(ruta);
    } on Object catch (e) {
      if (mounted) {
        setState(
          () => _error = e is ErrorPresentable
              ? e.mensaje
              : 'No pudimos subir tu foto. Probá de nuevo.',
        );
      }
    } finally {
      if (mounted) setState(() => _subiendo = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final foto = ref.watch(fotoDePerfilProvider);
    final nombre = ref.watch(nombreDeLaCuentaProvider).asData?.value ?? '';
    final bytes = foto.asData?.value;
    final tieneFoto = bytes != null;
    final radio = BorderRadius.circular(Radios.xxl);
    // La misma tarjeta verde que el saldo del inicio (`EncabezadoDeSaldo`): el perfil
    // abre con la persona, no con un formulario.
    final cabecera = DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: radio,
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [t.heroInicio, t.heroFin],
        ),
        boxShadow: Elevacion.tarjeta(t),
      ),
      child: DecoratedBox(
        decoration: BoxDecoration(
          borderRadius: radio,
          gradient: RadialGradient(
            center: Alignment.topRight,
            radius: 1.1,
            colors: [t.heroBrillo, t.heroBrillo.withValues(alpha: 0)],
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s5),
          child: Row(
            children: [
              AvatarConAro(
                nombre: nombre,
                imagen: tieneFoto ? MemoryImage(bytes) : null,
                alTocar: _subiendo ? null : _cambiar,
              ),
              const SizedBox(width: Espacio.s4),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (nombre.isNotEmpty) ...[
                      Text(
                        nombre,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: Tipo.titulo2.copyWith(color: t.heroTexto),
                      ),
                      const SizedBox(height: Espacio.s3),
                    ],
                    Boton(
                      texto: _subiendo
                          ? 'Subiendo foto…'
                          : (tieneFoto ? 'Cambiar foto' : 'Agregar foto'),
                      variante: BotonVariante.secundario,
                      tamano: BotonTamano.sm,
                      icono: Icons.photo_camera_outlined,
                      cargando: _subiendo,
                      onPressed: _cambiar,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        cabecera,
        if (_error != null) ...[
          const SizedBox(height: Espacio.s3),
          Alerta(
            key: const ValueKey('foto-perfil-error'),
            titulo: _error!,
            tono: Tono.error,
          ),
        ],
      ],
    );
  }
}

enum _Origen { camara, galeria }
