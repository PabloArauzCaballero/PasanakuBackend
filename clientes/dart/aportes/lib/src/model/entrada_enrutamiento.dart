//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_enrutamiento.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaEnrutamiento {
  /// Returns a new [EntradaEnrutamiento] instance.
  EntradaEnrutamiento({

    required  this.claveIdempotencia,

     this.yaIntentados,

     this.saludObservada,
  });

  @JsonKey(
    
    name: r'claveIdempotencia',
    required: true,
    includeIfNull: false,
  )


  final String claveIdempotencia;



      /// Ausente equivale a lista vacía. Sin `default` a propósito — el generador Dart (dart-dio) emite el valor por omisión sin `const`, y una lista no-const no es una expresión constante válida (rompe la compilación del cliente).
  @JsonKey(
    
    name: r'yaIntentados',
    required: false,
    includeIfNull: false,
  )


  final List<String>? yaIntentados;



      /// Salud por codigo de proveedor, de quien la mide. No hay tabla de salud en aportes; un proveedor sin medicion se trata como sano porque acaba de pasar contrato y pruebas. Hueco declarado en planes/informes/carril-3A.md.
  @JsonKey(
    
    name: r'saludObservada',
    required: false,
    includeIfNull: false,
  )


  final Map<String, int>? saludObservada;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaEnrutamiento &&
      other.claveIdempotencia == claveIdempotencia &&
      other.yaIntentados == yaIntentados &&
      other.saludObservada == saludObservada;

    @override
    int get hashCode =>
        claveIdempotencia.hashCode +
        yaIntentados.hashCode +
        saludObservada.hashCode;

  factory EntradaEnrutamiento.fromJson(Map<String, dynamic> json) => _$EntradaEnrutamientoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaEnrutamientoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

