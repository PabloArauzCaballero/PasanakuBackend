import Flutter
import UIKit

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  private let proteccionPantalla = ProteccionPantalla()

  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)
    if let registrar = engineBridge.pluginRegistry.registrar(forPlugin: "ProteccionPantalla") {
      proteccionPantalla.registrar(con: registrar.messenger())
    }
  }
}

/// Atiende `bo.aportaya/proteccion_pantalla`, el mismo canal que `MainActivity.kt`
/// resuelve con `FLAG_SECURE` en Android.
///
/// iOS no tiene `FLAG_SECURE`. Lo que tapa el contenido en capturas y grabaciones es
/// el lienzo de un `UITextField` con `isSecureTextEntry`: la capa de la ventana se
/// cuelga UNA vez de ese lienzo, y después `activar`/`desactivar` solo cambian
/// `isSecureTextEntry`, sin volver a mover capas. Es un comportamiento de UIKit que
/// Apple no documenta como API de privacidad; por eso el grado en
/// `capacidades.dart` es `degradado`.
final class ProteccionPantalla {
  private var canal: FlutterMethodChannel?
  private var campoSeguro: UITextField?

  func registrar(con mensajero: FlutterBinaryMessenger) {
    let canal = FlutterMethodChannel(
      name: "bo.aportaya/proteccion_pantalla", binaryMessenger: mensajero)
    canal.setMethodCallHandler { [weak self] llamada, resultado in
      guard let self else { return resultado(nil) }
      switch llamada.method {
      case "activar": self.proteger(true, resultado)
      case "desactivar": self.proteger(false, resultado)
      default: resultado(FlutterMethodNotImplemented)
      }
    }
    self.canal = canal
  }

  private func proteger(_ protegida: Bool, _ resultado: FlutterResult) {
    guard let campo = campoSeguro ?? prepararCampo() else {
      // Sin ventana todavía no hay nada que mostrar ni que capturar.
      return resultado(
        FlutterError(code: "SIN_VENTANA", message: "No hay ventana activa", details: nil))
    }
    campo.isSecureTextEntry = protegida
    resultado(nil)
  }

  private func prepararCampo() -> UITextField? {
    guard let ventana = ventanaActiva(), let contenedor = ventana.layer.superlayer else {
      return nil
    }
    let campo = UITextField(frame: ventana.bounds)
    campo.isUserInteractionEnabled = false
    campo.isSecureTextEntry = true
    ventana.addSubview(campo)
    contenedor.addSublayer(campo.layer)
    // El lienzo seguro es la primera subcapa del campo hasta iOS 16 y la última
    // desde iOS 17.
    let lienzo: CALayer?
    if #available(iOS 17.0, *) {
      lienzo = campo.layer.sublayers?.last
    } else {
      lienzo = campo.layer.sublayers?.first
    }
    guard let lienzo else {
      campo.layer.removeFromSuperlayer()
      campo.removeFromSuperview()
      return nil
    }
    lienzo.addSublayer(ventana.layer)
    campoSeguro = campo
    return campo
  }

  private func ventanaActiva() -> UIWindow? {
    let escenas = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
    let ventanas = escenas.flatMap { $0.windows }
    return ventanas.first { $0.isKeyWindow } ?? ventanas.first
  }
}
