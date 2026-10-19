import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_consultar_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_de_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/seguimiento_del_alta.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

const _usuario = 'uuuuuuuu-0000-4000-8000-000000000123';
const _token = 'token-de-alta-sintetico';

/// Doble de la consulta del expediente: registra con qué token se llamó.
class _ConsultaQueAnota extends ConsultarVerificacion {
  _ConsultaQueAnota(super.ref);

  final tokens = <String?>[];

  @override
  Future<EstadoDeVerificacion> ejecutar(
    String usuarioId, {
    String? tokenDeAlta,
  }) async {
    tokens.add(tokenDeAlta);
    return EstadoDeVerificacion(
      verificacionId: 'a11ce001-0000-4000-8000-000000000123',
      estado: EstadoDeVerificacionEstadoEnum.EN_REVISION,
      fotos: const [],
    );
  }
}

void main() {
  test(
    'al terminar de subir las fotos, el token del alta se conserva',
    () async {
      final c = ProviderContainer();
      addTearDown(c.dispose);
      c
          .read(seguimientoDelAltaProvider.notifier)
          .fijar(_usuario, const CapturasDelExpediente(), tokenDeAlta: _token);

      await c.read(seguimientoDelAltaProvider.notifier).liberarCapturas();

      final s = c.read(seguimientoDelAltaProvider);
      expect(s?.usuarioId, _usuario);
      expect(s?.tokenDeAlta, _token);
      expect(s?.capturas.porCara, isEmpty);
    },
  );

  test(
    'la consulta de estado tras subir las fotos viaja con el token del alta',
    () async {
      late _ConsultaQueAnota consulta;
      final c = ProviderContainer(
        overrides: [
          consultarVerificacionProvider.overrideWith(
            (ref) => consulta = _ConsultaQueAnota(ref),
          ),
        ],
      );
      addTearDown(c.dispose);
      c
          .read(seguimientoDelAltaProvider.notifier)
          .fijar(_usuario, const CapturasDelExpediente(), tokenDeAlta: _token);
      await c.read(seguimientoDelAltaProvider.notifier).liberarCapturas();
      c.listen(verificacionProvider, (_, _) {});

      c.read(verificacionProvider.notifier).iniciar(_usuario);
      await c.read(verificacionProvider.future);

      expect(consulta.tokens, isNotEmpty);
      expect(consulta.tokens, everyElement(_token));
    },
  );

  test('antes de saber el usuario la pantalla queda cargando, no en error', () {
    final c = ProviderContainer();
    addTearDown(c.dispose);
    c.listen(verificacionProvider, (_, _) {});

    final estado = c.read(verificacionProvider);

    expect(estado.isLoading, isTrue);
    expect(estado.hasError, isFalse);
  });

  test(
    'si el provider se reconstruye, sigue consultando al mismo usuario',
    () async {
      final c = ProviderContainer(
        overrides: [
          consultarVerificacionProvider.overrideWith(_ConsultaQueAnota.new),
        ],
      );
      addTearDown(c.dispose);
      c.listen(verificacionProvider, (_, _) {});
      c.read(verificacionProvider.notifier).iniciar(_usuario);
      await c.read(verificacionProvider.future);

      c.invalidate(verificacionProvider);
      final tras = await c.read(verificacionProvider.future);

      expect(tras.estado, EstadoDeVerificacionEstadoEnum.EN_REVISION);
      expect(c.read(verificacionProvider).hasError, isFalse);
    },
  );
}
