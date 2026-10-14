import 'package:aportaya_movil/dominio/errores.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  // Recorrido del contrato identidad.yaml. Los códigos de enrutamiento o cierre
  // no se muestran como error: AP-CU04-03 pide MFA y AP-SES-01 cierra la sesión.
  const mensajesEsperados = {
    'AP-CU01-01':
        'Todavía no podemos abrir tu cuenta: falta verificar tus datos.',
    'AP-CU01-02':
        'No pudimos completar la verificación de identidad. Contactá a soporte para revisar tu caso.',
    'AP-CU01-03':
        'Ya hay una cuenta con ese celular o ese documento. Si es tuya, iniciá sesión.',
    'AP-CU01-04': 'Para continuar, aceptá los contratos vigentes.',
    'AP-CU01-05':
        'Todavía no podemos abrir cuentas en tu zona. Te avisamos apenas se pueda.',
    'AP-CU01-06':
        'Esa contraseña no sirve: es muy corta o usa tu celular o tu documento. Elegí otra.',
    'AP-CU01-07':
        'Tu carnet está vencido. Para abrir la cuenta hace falta uno vigente.',
    'AP-CU01-08':
        'Este expediente ya se resolvió. Actualizá la bandeja antes de intentar otra vez.',
    'AP-CU01-09':
        'Faltan fotos del documento o de la prueba de vida. Completá la carga antes de aprobar.',
    'AP-CU01-10':
        'El documento no indica una fecha de vencimiento. Revisá la imagen y volvé a cargarla.',
    'AP-CU01-11': 'El documento venció y no se puede aprobar.',
    'AP-CU04-01': 'El celular o la contraseña no coinciden.',
    'AP-CU04-02':
        'Tu cuenta está bloqueada un rato por seguridad. Probá más tarde.',
    'AP-CU04-04': 'Ese código no sirve: revisalo o pedí uno nuevo.',
    'AP-CU04-05':
        'Demasiados intentos. Esperá unos minutos antes de volver a probar.',
    'AP-CU04-06': 'Tu segundo factor no está activado. Pedí ayuda a soporte.',
    'AP-CU04-07':
        'Ese tipo de segundo factor no sirve para tu cuenta. Pedí ayuda a soporte.',
    'AP-CU04-08': 'No pudimos mantener tu sesión. Volvé a entrar.',
  };
  const generico =
      'Algo salió mal de nuestro lado. Probá de nuevo en un momento.';

  for (final entrada in mensajesEsperados.entries) {
    test('${entrada.key} usa el texto definido para el contrato', () {
      expect(mensajeDe(entrada.key), entrada.value);
      expect(mensajeDe(entrada.key), isNot(generico));
    });
  }

  test(
    'los errores de infraestructura conservan un mensaje no culpabilizante',
    () {
      expect(
        mensajeDe('NO_EXISTE', estado: 503),
        contains('No es tu conexión'),
      );
      expect(mensajeDe('NO_EXISTE', estado: 503), contains('probá de nuevo'));
    },
  );

  test('solo los códigos que identifican un campo se mapean a un campo', () {
    expect(
      ErrorDeApi(codigo: 'AP-CU01-06', trazaId: 't1', estado: 422).campo,
      CampoDeErrorApi.contrasena,
    );
    expect(
      ErrorDeApi(codigo: 'AP-CU01-07', trazaId: 't2', estado: 422).campo,
      CampoDeErrorApi.fechaExpiracion,
    );
    // CU01-03 es ambiguo: el contrato usa el mismo código para teléfono y documento.
    expect(
      ErrorDeApi(codigo: 'AP-CU01-03', trazaId: 't3', estado: 409).campo,
      isNull,
    );
  });
}
