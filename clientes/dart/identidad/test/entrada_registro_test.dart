import 'package:test/test.dart';
import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';

// tests for EntradaRegistro
void main() {
  final EntradaRegistro? instance = /* EntradaRegistro(...) */ null;
  // TODO add properties to the entity

  group(EntradaRegistro, () {
    // String telefonoE164
    test('to test the property `telefonoE164`', () async {
      // TODO
    });

    // String nombres
    test('to test the property `nombres`', () async {
      // TODO
    });

    // String apellidos
    test('to test the property `apellidos`', () async {
      // TODO
    });

    // DateTime fechaNacimiento
    test('to test the property `fechaNacimiento`', () async {
      // TODO
    });

    // La clave con la que se va a ingresar. Es obligatoria: el alta no creaba credencial y despues no habia con que entrar — se completaban los ocho pasos y el formulario de ingreso pedia una contrasena que nunca se eligio. El minimo de ocho es el de `PoliticaDeClave`; el resto de la politica —no derivada del telefono ni del documento— la aplica el servicio, que es el unico que puede.
    // String contrasena
    test('to test the property `contrasena`', () async {
      // TODO
    });

    // Opcional en el contrato y pedido por la app. Una billetera manda extractos y comprobantes, y ese es su destino; ademas es lo que permite elegir el correo como canal de verificacion en vez del SMS.
    // String correo
    test('to test the property `correo`', () async {
      // TODO
    });

    // Por donde quiere recibir la verificacion quien se registra. No son dos formas de lo mismo: `SMS` verifica el TELEFONO y `CORREO` verifica el CORREO —dos propositos distintos del catalogo de tokens, con su propia vigencia y sus propios canales permitidos—. Elegir `CORREO` exige `correo`.
    // String canalVerificacion
    test('to test the property `canalVerificacion`', () async {
      // TODO
    });

    // Documento documento
    test('to test the property `documento`', () async {
      // TODO
    });

    // List<String> aceptaContratos
    test('to test the property `aceptaContratos`', () async {
      // TODO
    });

  });
}
