enum EscenaDePauta {
  carnetClaro,
  carnetOscuro,
  carnetReflejo,
  carnetBorroso,
  carnetInclinado,
  rostroFrontal,
  rostroLadeado,
  rostroConAccesorios,
  rostroContraluz,
}

class ConsejoDeFoto {
  const ConsejoDeFoto({
    required this.titulo,
    required this.texto,
    required this.correcta,
    required this.incorrecta,
    required this.etiquetaCorrecta,
    required this.etiquetaIncorrecta,
  });

  final String titulo;
  final String texto;
  final EscenaDePauta correcta;
  final EscenaDePauta incorrecta;
  final String etiquetaCorrecta;
  final String etiquetaIncorrecta;
}

const pautasDeDocumento = [
  ConsejoDeFoto(
    titulo: 'Buena iluminación',
    texto:
        'Buscá una luz pareja. Si la foto sale muy oscura o muy brillante, el documento no se puede validar.',
    correcta: EscenaDePauta.carnetClaro,
    incorrecta: EscenaDePauta.carnetOscuro,
    etiquetaCorrecta: 'Carnet bien iluminado, se lee todo',
    etiquetaIncorrecta: 'Carnet demasiado oscuro, no se lee',
  ),
  ConsejoDeFoto(
    titulo: 'Evitá los reflejos',
    texto:
        'No uses la linterna. Los reflejos de lámparas o ventanas tapan el número y la fecha.',
    correcta: EscenaDePauta.carnetClaro,
    incorrecta: EscenaDePauta.carnetReflejo,
    etiquetaCorrecta: 'Carnet sin reflejos',
    etiquetaIncorrecta: 'Una franja de reflejo tapa el texto',
  ),
  ConsejoDeFoto(
    titulo: 'Enfoque y nitidez',
    texto:
        'Sostené firme el teléfono y esperá a que enfoque. No debe haber zonas borrosas.',
    correcta: EscenaDePauta.carnetClaro,
    incorrecta: EscenaDePauta.carnetBorroso,
    etiquetaCorrecta: 'Carnet nítido',
    etiquetaIncorrecta: 'Carnet borroso o movido',
  ),
  ConsejoDeFoto(
    titulo: 'De frente, sin inclinar',
    texto:
        'Apoyalo en una mesa lisa y fotografialo desde arriba, con el teléfono paralelo al carnet.',
    correcta: EscenaDePauta.carnetClaro,
    incorrecta: EscenaDePauta.carnetInclinado,
    etiquetaCorrecta: 'Carnet visto de frente',
    etiquetaIncorrecta: 'Carnet inclinado, visto de lado',
  ),
];

const pautasDeSelfie = [
  ConsejoDeFoto(
    titulo: 'De frente y centrado',
    texto:
        'Mirá a la cámara con la cabeza recta y la cara dentro del encuadre.',
    correcta: EscenaDePauta.rostroFrontal,
    incorrecta: EscenaDePauta.rostroLadeado,
    etiquetaCorrecta: 'Rostro de frente y centrado',
    etiquetaIncorrecta: 'Rostro ladeado y fuera del centro',
  ),
  ConsejoDeFoto(
    titulo: 'Sin accesorios',
    texto:
        'Sacate los lentes oscuros, la gorra y el barbijo. Se tiene que ver la cara entera.',
    correcta: EscenaDePauta.rostroFrontal,
    incorrecta: EscenaDePauta.rostroConAccesorios,
    etiquetaCorrecta: 'Rostro descubierto',
    etiquetaIncorrecta: 'Rostro con lentes oscuros',
  ),
  ConsejoDeFoto(
    titulo: 'La luz, de frente',
    texto:
        'Que la luz te dé en la cara. Con una ventana o una lámpara detrás, la cara sale oscura.',
    correcta: EscenaDePauta.rostroFrontal,
    incorrecta: EscenaDePauta.rostroContraluz,
    etiquetaCorrecta: 'Rostro con luz de frente',
    etiquetaIncorrecta: 'Rostro a contraluz, oscuro',
  ),
];
