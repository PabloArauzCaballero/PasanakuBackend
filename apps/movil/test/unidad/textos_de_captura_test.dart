import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/juez_de_pose.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_captura.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_subida.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const rotulos = <CaraDelCarril, String>{
    CaraDelCarril.anverso: 'Repetir foto del anverso',
    CaraDelCarril.reverso: 'Repetir foto del reverso',
    CaraDelCarril.selfie: 'Repetir foto de frente',
    CaraDelCarril.perfilIzquierdo: 'Repetir foto del perfil izquierdo',
    CaraDelCarril.perfilDerecho: 'Repetir foto del perfil derecho',
  };

  test('los rótulos del carrusel nombran cada cara', () {
    for (final entrada in rotulos.entries) {
      expect(TextosDeCaptura.repetirFotoDe(entrada.key), entrada.value);
    }
  });

  test('los rótulos de subida nombran cada cara', () {
    for (final entrada in rotulos.entries) {
      expect(TextosDeSubida.repetirFotoDe(entrada.key), entrada.value);
    }
  });

  test('la prueba de vida usa el copy de cada pose y fase', () {
    expect(TextosDeCaptura.fotoDeVida(2, 3), 'FOTO 2 DE 3');
    expect(TextosDeCaptura.tituloDePose(PoseDeVida.frente), 'Mirá de frente');
    expect(
      TextosDeCaptura.instruccionDePose(PoseDeVida.frente),
      'Poné tu cara dentro del óvalo.',
    );
    expect(
      TextosDeCaptura.tituloDePose(PoseDeVida.izquierda),
      'Girá a tu izquierda',
    );
    expect(
      TextosDeCaptura.instruccionDePose(PoseDeVida.izquierda),
      'Despacio, hasta que se vea tu oreja derecha.',
    );
    expect(
      TextosDeCaptura.tituloDePose(PoseDeVida.derecha),
      'Girá a tu derecha',
    );
    expect(
      TextosDeCaptura.instruccionDePose(PoseDeVida.derecha),
      'Despacio, hasta que se vea tu oreja izquierda.',
    );
    expect(TextosDeCaptura.quietoFoto, 'Quedate quieto: la foto sale sola.');
    expect(
      TextosDeCaptura.instruccionDuranteCuenta(PoseDeVida.frente),
      'Poné tu cara dentro del óvalo. Quedate quieto: la foto sale sola.',
    );
    expect(TextosDeCaptura.fotoTomada, '¡Foto tomada!');
    expect(TextosDeCaptura.seguirSiguiente, 'Seguí la siguiente indicación.');
    expect(TextosDeCaptura.vidaLista, '¡Listo! Tus tres fotos están');
  });
}
