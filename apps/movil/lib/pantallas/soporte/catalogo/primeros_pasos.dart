import '../../../dominio/tutoriales/modelo.dart';

/// La categoría con la que se agrupan en el centro de ayuda.
const String primerosPasos = 'Primeros pasos';

/// Los tutoriales que le sirven a cualquiera, tenga o no un pasanaku andando.
/// Son DATOS: ninguno importa un widget ni sabe cómo se dibuja un globo.
const TutorialDefinicion introALaApp = TutorialDefinicion(
  id: 'intro-app',
  version: '1.0.0',
  titulo: 'Qué es AportaYa',
  descripcion:
      'El pasanaku de siempre, sin el cuaderno. Qué ves en la pantalla de inicio y qué podés hacer.',
  categoria: primerosPasos,
  ruta: '/billetera/inicio',
  minutos: 3,
  dificultad: Dificultad.inicial,
  obligatorio: true,
  siguiente: 'navegacion-app',
  pasos: [
    PasoDeTutorial(
      id: 'saldo',
      titulo: 'Acá está tu plata',
      descripcion:
          'Este es el saldo de tu billetera. Lo que ves es lo que hay: AportaYa no cobra nada que no te haya mostrado antes.',
      ancla: 'billetera.saldo',
      ruta: '/billetera/inicio',
    ),
    PasoDeTutorial(
      id: 'acciones',
      titulo: 'Tres cosas para hacer',
      descripcion:
          'Aportar es lo que mantiene vivo tu pasanaku, por eso es el botón naranja. Recargar mete plata a la billetera; retirar la saca.',
      ancla: 'billetera.acciones',
      posicion: PosicionDeGlobo.arriba,
    ),
    PasoDeTutorial(
      id: 'pestanas',
      titulo: 'Y todo lo demás vive acá abajo',
      descripcion:
          'Inicio, Grupos, Movimientos, Perfil y Ayuda. Siempre están: no hace falta volver atrás para cambiar de lugar.',
      ancla: 'shell.pestanas',
      posicion: PosicionDeGlobo.arriba,
    ),
  ],
);

const TutorialDefinicion navegacionDeLaApp = TutorialDefinicion(
  id: 'navegacion-app',
  version: '1.0.0',
  titulo: 'Moverse por la app',
  descripcion: 'Qué hay en cada pestaña y cómo volver a donde estabas.',
  categoria: primerosPasos,
  ruta: '/billetera/inicio',
  minutos: 2,
  dificultad: Dificultad.inicial,
  requisitos: ['intro-app'],
  siguiente: 'usar-ayuda',
  pasos: [
    PasoDeTutorial(
      id: 'barra',
      titulo: 'La barra de abajo es el mapa',
      descripcion:
          'Cada pestaña recuerda dónde estabas: si salís de Grupos y volvés, volvés al mismo lugar.',
      ancla: 'shell.pestanas',
      posicion: PosicionDeGlobo.arriba,
      ruta: '/billetera/inicio',
    ),
    PasoDeTutorial(
      id: 'grupos',
      titulo: 'Probá ir a Grupos',
      descripcion:
          'Ahí está el estado de tus pasanakus: qué te toca aportar y cuándo es tu turno. Tocá «Grupos» para seguir.',
      ancla: 'shell.pestanas',
      posicion: PosicionDeGlobo.arriba,
      accion: AccionEsperada.navegar('/pasanaku/mi-estado'),
      ayudaSiFalla: 'Tocá «Grupos» en la barra de abajo para seguir.',
    ),
    PasoDeTutorial(
      id: 'volver',
      titulo: 'Y volver siempre es posible',
      descripcion:
          'Tocás «Inicio» y estás de nuevo en tu saldo. Nada de lo que estabas mirando se pierde.',
      ancla: 'pasanaku.mi-estado',
      ruta: '/pasanaku/mi-estado',
    ),
  ],
);

const TutorialDefinicion usarElCentroDeAyuda = TutorialDefinicion(
  id: 'usar-ayuda',
  version: '1.0.0',
  titulo: 'Usar el centro de ayuda',
  descripcion:
      'Buscar un tutorial, ver cuánto llevás hecho y repetir el que quieras.',
  categoria: primerosPasos,
  ruta: '/soporte/ayuda',
  minutos: 2,
  dificultad: Dificultad.inicial,
  pasos: [
    PasoDeTutorial(
      id: 'avance',
      titulo: 'Tu avance, de un vistazo',
      descripcion: 'Cuántos tutoriales completaste de los que hay para vos.',
      ancla: 'ayuda.avance',
      ruta: '/soporte/ayuda',
    ),
    PasoDeTutorial(
      id: 'buscar',
      titulo: 'Buscá lo que necesitás',
      descripcion:
          'Escribí «aportar», «turno», «retirar». La lista se recorta mientras escribís.',
      ancla: 'ayuda.buscador',
    ),
    PasoDeTutorial(
      id: 'repetir',
      titulo: 'Ninguno se gasta',
      descripcion:
          'Podés repetir cualquier tutorial las veces que quieras. «Reiniciar» lo deja como el primer día.',
      ancla: 'ayuda.lista',
      posicion: PosicionDeGlobo.arriba,
    ),
  ],
);
