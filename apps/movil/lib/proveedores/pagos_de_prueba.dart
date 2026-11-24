import 'package:flutter_riverpod/flutter_riverpod.dart';

/// FLUJO-QR-001: «Simular pago» acredita saldo sin dinero real. Antes solo lo apagaba
/// una propiedad del servidor; ahora además la build tiene que pedirlo explícitamente
/// con `--dart-define=PAGOS_DE_PRUEBA=true` (APK de prueba contra TEST). Sin ese
/// interruptor el botón no existe, aunque un servidor mal configurado mande un QR de
/// origen PRUEBA.
const bool _pagosDePrueba = bool.fromEnvironment('PAGOS_DE_PRUEBA');

final pagosDePruebaProvider = Provider<bool>((_) => _pagosDePrueba);
