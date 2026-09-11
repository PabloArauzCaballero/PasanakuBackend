import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/puertos/camara.dart';
import '../../../infraestructura/plataforma.dart';

/// El puerto `Camara` no trae proveedor propio en `proveedores/` (los siete
/// puertos de F2 solo declararon la interfaz — ver comentario en
/// `dominio/puertos/camara.dart`: "el primero es F3"). Como `proveedores/` está
/// congelado para este carril, el provider vive acá, en el dominio que primero lo
/// necesita, envolviendo la misma fábrica que usan los otros puertos
/// (`camaraDeLaPlataforma`). Si un carril hermano necesita esto mismo, corresponde
/// un micro-PR que lo suba a `proveedores/camara.dart` — no una segunda copia.
final camaraProvider = Provider<Camara>((_) => camaraDeLaPlataforma());
