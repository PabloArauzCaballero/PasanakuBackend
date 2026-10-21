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
  static const IconData periodo = LucideIcons.calendarRange;
  static const IconData reputacion = LucideIcons.award;
  static const IconData recienEmpiezas = LucideIcons.sprout;

  // Movimientos: el sentido, cuando el contrato no dice más…
  static const IconData entra = LucideIcons.arrowDownLeft;
  static const IconData sale = LucideIcons.arrowUpRight;
  static const IconData billetera = LucideIcons.wallet;

  // …y qué pasó, cuando sí lo dice (`TipoDeMovimiento`).
  static const IconData ingresoDesdeBanco = LucideIcons.banknoteArrowDown;
  static const IconData salidaAlBanco = LucideIcons.banknoteArrowUp;
  static const IconData entregaDelFondo = LucideIcons.piggyBank;
  static const IconData comision = LucideIcons.receiptText;
  static const IconData reverso = LucideIcons.undo2;
  static const IconData rechazado = LucideIcons.ban;
  static const IconData bono = LucideIcons.partyPopper;

  // Estados vacíos (`MotivoVacio`)
  static const IconData sinDatos = LucideIcons.inbox;
  static const IconData porFiltro = LucideIcons.filterX;
  static const IconData porPermiso = LucideIcons.lock;

  // Canales de pago
  static const IconData canalBilletera = LucideIcons.wallet;
  static const IconData canalQr = LucideIcons.qrCode;
  static const IconData canalBanco = LucideIcons.landmark;
  static const IconData canalTarjeta = LucideIcons.creditCard;

  // Trámites (estado de una verificación)
  static const IconData enEspera = LucideIcons.hourglass;
  static const IconData tramiteListo = LucideIcons.circleCheck;
  static const IconData tramiteFallido = LucideIcons.circleX;

  // Cuenta
  static const IconData verificada = LucideIcons.badgeCheck;
  static const IconData contrasena = LucideIcons.keyRound;
  static const IconData guia = LucideIcons.bookOpen;
  static const IconData cerrarSesion = LucideIcons.logOut;
  static const IconData darDeBaja = LucideIcons.userX;
  static const IconData expediente = LucideIcons.fileCheck;

  // Interfaz
  static const IconData avisos = LucideIcons.bell;
  static const IconData seguir = LucideIcons.chevronRight;
  static const IconData listo = LucideIcons.circleCheck;
}
