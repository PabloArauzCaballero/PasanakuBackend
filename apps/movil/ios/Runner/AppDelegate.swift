import Flutter
import UIKit
import Vision

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  private let proteccionPantalla = ProteccionPantalla()
  private let lectorDeRostro = LectorDeRostroVision()

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
    if let registrar = engineBridge.pluginRegistry.registrar(forPlugin: "LectorDeRostroVision") {
      lectorDeRostro.registrar(con: registrar.messenger())
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

/// Atiende `bo.aportaya/lector_de_rostro`: lee la cara de un cuadro BGRA de la cámara con
/// Vision de Apple, en el teléfono. Reemplaza a ML Kit en iOS (sus pods no tienen slice
/// arm64 de simulador). Devuelve, por cara, el centro y el ancho normalizados en el cuadro
/// ya rotado (origen arriba a la izquierda) y `yaw`/`pitch` en grados. Vision no da la
/// probabilidad de ojos abiertos: el juez de pose la acepta ausente.
final class LectorDeRostroVision {
  private let cola = DispatchQueue(label: "bo.aportaya.lector_de_rostro", qos: .userInitiated)

  func registrar(con mensajero: FlutterBinaryMessenger) {
    let canal = FlutterMethodChannel(
      name: "bo.aportaya/lector_de_rostro", binaryMessenger: mensajero)
    canal.setMethodCallHandler { [weak self] llamada, resultado in
      guard let self, llamada.method == "leer" else { return resultado(FlutterMethodNotImplemented) }
      guard let a = llamada.arguments as? [String: Any],
        let datos = a["bytes"] as? FlutterStandardTypedData,
        let ancho = a["ancho"] as? Int, let alto = a["alto"] as? Int,
        let bytesPorFila = a["bytesPorFila"] as? Int,
        let grados = a["rotacion"] as? Int
      else {
        return resultado(FlutterError(code: "ARGUMENTOS", message: "Cuadro incompleto", details: nil))
      }
      self.cola.async {
        let caras = self.leer(datos.data, ancho, alto, bytesPorFila, grados)
        DispatchQueue.main.async { resultado(caras) }
      }
    }
  }

  private func leer(_ datos: Data, _ ancho: Int, _ alto: Int, _ bytesPorFila: Int, _ grados: Int)
    -> [[String: Double]]?
  {
    guard let cuadro = Self.buffer(datos, ancho, alto, bytesPorFila) else { return nil }
    let pedido = VNDetectFaceRectanglesRequest()
    // La revisión 3 (iOS 15) es la que da `yaw` y `pitch` continuos.
    pedido.revision = VNDetectFaceRectanglesRequestRevision3
    let manejador = VNImageRequestHandler(
      cvPixelBuffer: cuadro, orientation: Self.orientacion(grados), options: [:])
    guard (try? manejador.perform([pedido])) != nil else { return nil }
    return (pedido.results ?? []).map { cara in
      let caja = cara.boundingBox
      return [
        "x": Double(caja.midX),
        // Vision mide desde abajo; el dominio, desde arriba.
        "y": Double(1 - caja.midY),
        "ancho": Double(caja.width),
        "yaw": (cara.yaw?.doubleValue ?? 0) * 180 / .pi,
        "pitch": (cara.pitch?.doubleValue ?? 0) * 180 / .pi,
      ]
    }
  }

  /// La rotación del sensor (la misma que se le daba a ML Kit) como orientación de Vision.
  private static func orientacion(_ grados: Int) -> CGImagePropertyOrientation {
    switch grados {
    case 90: return .right
    case 180: return .down
    case 270: return .left
    default: return .up
    }
  }

  private static func buffer(_ datos: Data, _ ancho: Int, _ alto: Int, _ bytesPorFila: Int)
    -> CVPixelBuffer?
  {
    guard datos.count >= bytesPorFila * alto else { return nil }
    var salida: CVPixelBuffer?
    let estado = CVPixelBufferCreate(
      kCFAllocatorDefault, ancho, alto, kCVPixelFormatType_32BGRA, nil, &salida)
    guard estado == kCVReturnSuccess, let buffer = salida else { return nil }
    CVPixelBufferLockBaseAddress(buffer, [])
    defer { CVPixelBufferUnlockBaseAddress(buffer, []) }
    guard let destino = CVPixelBufferGetBaseAddress(buffer) else { return nil }
    let filaDestino = CVPixelBufferGetBytesPerRow(buffer)
    datos.withUnsafeBytes { origen in
      guard let base = origen.baseAddress else { return }
      for fila in 0..<alto {
        memcpy(
          destino.advanced(by: fila * filaDestino), base.advanced(by: fila * bytesPorFila),
          min(filaDestino, bytesPorFila))
      }
    }
    return buffer
  }
}
