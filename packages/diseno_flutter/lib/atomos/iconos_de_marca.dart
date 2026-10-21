import 'package:flutter/widgets.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

/// **Los íconos de la app, por lo que significan** (ADR-053: Lucide, trazo uniforme).
///
/// Una pantalla pide `IconosDeMarca.aportar`, no `LucideIcons.handCoins`: si mañana el
/// ícono de «aportar» cambia, cambia acá y en ningún otro lado. Las pantallas no importan
/// `lucide_icons_flutter` directo.
abstract final class IconosDeMarca {
  // Navegación
  static const IconData inicio = LucideIcons.house;
  static const IconData grupos = LucideIcons.users;
  static const IconData escanear = LucideIcons.scanLine;
  static const IconData movimientos = LucideIcons.arrowLeftRight;
  static const IconData perfil = LucideIcons.user;
  static const IconData ayuda = LucideIcons.lifeBuoy;

  // Acciones de dinero
  static const IconData recargar = LucideIcons.plus;
  static const IconData enviar = LucideIcons.arrowUpRight;
  static const IconData aportar = LucideIcons.handCoins;
  static const IconData retirar = LucideIcons.landmark;
  static const IconData qr = LucideIcons.qrCode;

  // Saldo y confianza
  static const IconData mostrarSaldo = LucideIcons.eye;
  static const IconData ocultarSaldo = LucideIcons.eyeOff;
  static const IconData custodia = LucideIcons.shieldCheck;
  static const IconData subida = LucideIcons.trendingUp;
  static const IconData vencimiento = LucideIcons.calendarClock;

  // Movimientos
  static const IconData entra = LucideIcons.arrowDownLeft;
  static const IconData sale = LucideIcons.arrowUpRight;
  static const IconData billetera = LucideIcons.wallet;

  // Interfaz
  static const IconData avisos = LucideIcons.bell;
  static const IconData seguir = LucideIcons.chevronRight;
  static const IconData listo = LucideIcons.circleCheck;
}
