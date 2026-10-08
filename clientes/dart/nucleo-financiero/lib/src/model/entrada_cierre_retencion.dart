//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_cierre_retencion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCierreRetencion {
  /// Returns a new [EntradaCierreRetencion] instance.
  EntradaCierreRetencion({

    required  this.desenlace,
  });

      /// Liberar devuelve el saldo a disponible; ejecutar lo convierte en un movimiento del libro. Son dos finales distintos y el llamador dice cual.
  @JsonKey(
    
    name: r'desenlace',
    required: true,
    includeIfNull: false,
  )


  final EntradaCierreRetencionDesenlaceEnum desenlace;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCierreRetencion &&
      other.desenlace == desenlace;

    @override
    int get hashCode =>
        desenlace.hashCode;

  factory EntradaCierreRetencion.fromJson(Map<String, dynamic> json) => _$EntradaCierreRetencionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCierreRetencionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Liberar devuelve el saldo a disponible; ejecutar lo convierte en un movimiento del libro. Son dos finales distintos y el llamador dice cual.
enum EntradaCierreRetencionDesenlaceEnum {
    /// Liberar devuelve el saldo a disponible; ejecutar lo convierte en un movimiento del libro. Son dos finales distintos y el llamador dice cual.
@JsonValue(r'LIBERADA')
LIBERADA(r'LIBERADA'),
    /// Liberar devuelve el saldo a disponible; ejecutar lo convierte en un movimiento del libro. Son dos finales distintos y el llamador dice cual.
@JsonValue(r'EJECUTADA')
EJECUTADA(r'EJECUTADA');

const EntradaCierreRetencionDesenlaceEnum(this.value);

final String value;

@override
String toString() => value;
}


