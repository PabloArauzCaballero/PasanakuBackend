import '../../../dominio/tutoriales/modelo.dart';

const String tuPlata = 'Tu plata';
const String tuPasanaku = 'Tu pasanaku';
const String tuCuenta = 'Tu cuenta';

const TutorialDefinicion entenderTuSaldo = TutorialDefinicion(
  id: 'billetera-saldo',
  version: '1.0.0',
  titulo: 'Entender tu saldo',
  descripcion: 'Qué es disponible, qué es retenido y por qué no son lo mismo.',
  categoria: tuPlata,
  ruta: '/billetera/inicio',
  minutos: 3,
  dificultad: Dificultad.inicial,
  requisitos: ['intro-app'],
  requiere: {Capacidad.sesion},
  siguiente: 'billetera-movimientos',
  pasos: [
    PasoDeTutorial(
      id: 'disponible',
      titulo: 'Disponible es lo que podés usar hoy',
      descripcion:
          'Con eso aportás, transferís o retirás. Es plata tuya, en custodia, y siempre podés sacarla.',
      ancla: 'billetera.saldo',
      ruta: '/billetera/inicio',
    ),
    PasoDeTutorial(
      id: 'retenido',
      titulo: 'Retenido también es tuyo',
      descripcion:
          'Es plata comprometida: un aporte en curso, una garantía del grupo. No la perdiste; está reservada para algo que ya aceptaste.',
      ancla: 'billetera.saldo',
    ),
    PasoDeTutorial(
      id: 'recargar',
      titulo: 'Para que entre plata, recargás',
      descripcion:
          'Desde acá. Y para sacarla, retirás a tu cuenta bancaria. El tutorial no hace ninguna de las dos: eso lo decidís vos.',
      ancla: 'billetera.acciones',
      posicion: PosicionDeGlobo.arriba,
    ),
  ],
);

const TutorialDefinicion leerTusMovimientos = TutorialDefinicion(
  id: 'billetera-movimientos',
  version: '1.0.0',
  titulo: 'Leer tus movimientos',
  descripcion:
      'Dónde ver todo lo que entró y salió, y cómo entender cada línea.',
  categoria: tuPlata,
  ruta: '/billetera/extracto',
  minutos: 3,
  dificultad: Dificultad.inicial,
  requisitos: ['billetera-saldo'],
  requiere: {Capacidad.sesion},
  pasos: [
    PasoDeTutorial(
      id: 'extracto',
      titulo: 'Este es tu extracto',
      descripcion:
          'Cada línea es un movimiento con su fecha y su monto. Nada se borra: si algo se revierte, aparece como un movimiento nuevo.',
      ancla: 'billetera.extracto',
      ruta: '/billetera/extracto',
    ),
    PasoDeTutorial(
      id: 'dudas',
      titulo: 'Si un movimiento no te cierra',
      descripcion:
          'Anotá la fecha y el monto y abrí un reclamo desde Perfil. Cada movimiento tiene un comprobante detrás.',
      ancla: 'billetera.extracto',
    ),
  ],
);

const TutorialDefinicion seguirTuPasanaku = TutorialDefinicion(
  id: 'pasanaku-estado',
  version: '1.0.0',
  titulo: 'Seguir tu pasanaku',
  descripcion:
      'Qué te toca aportar, cuándo vence y cuándo es tu turno de cobrar.',
  categoria: tuPasanaku,
  ruta: '/pasanaku/mi-estado',
  minutos: 4,
  dificultad: Dificultad.inicial,
  obligatorio: true,
  requisitos: ['intro-app'],
  requiere: {Capacidad.sesion},
  pasos: [
    PasoDeTutorial(
      id: 'mi-estado',
      titulo: 'Acá está tu situación en el grupo',
      descripcion:
          'Lo que debés, lo que ya pagaste y qué pasa si te atrasás. Es la misma información que ve el resto del grupo sobre vos.',
      ancla: 'pasanaku.mi-estado',
      ruta: '/pasanaku/mi-estado',
    ),
    PasoDeTutorial(
      id: 'mora',
      titulo: 'Atrasarse tiene consecuencias, y se avisan antes',
      descripcion:
          'La mora no aparece de sorpresa: primero hay un aviso, después un plazo, y recién ahí un cargo. Todo está escrito en el reglamento del grupo.',
      ancla: 'pasanaku.mi-estado',
    ),
  ],
);

const TutorialDefinicion cuidarTuCuenta = TutorialDefinicion(
  id: 'perfil-seguridad',
  version: '1.0.0',
  titulo: 'Cuidar tu cuenta',
  descripcion: 'Dónde están tus datos, tus dispositivos y el segundo factor.',
  categoria: tuCuenta,
  ruta: '/identidad/perfil',
  minutos: 3,
  dificultad: Dificultad.inicial,
  requisitos: ['intro-app'],
  requiere: {Capacidad.sesion},
  pasos: [
    PasoDeTutorial(
      id: 'perfil',
      titulo: 'Tu perfil',
      descripcion:
          'Desde acá cambiás tu contraseña, mirás qué teléfonos tienen tu sesión abierta y podés dar de baja la cuenta.',
      ancla: 'identidad.perfil',
      ruta: '/identidad/perfil',
    ),
    PasoDeTutorial(
      id: 'segundo-factor',
      titulo: 'El segundo factor no es un trámite',
      descripcion:
          'Es lo que impide que alguien con tu contraseña entre a tu plata. Si cambiás de teléfono, revisá tus dispositivos.',
      ancla: 'identidad.perfil',
    ),
  ],
);
