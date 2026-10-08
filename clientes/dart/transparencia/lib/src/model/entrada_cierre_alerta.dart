//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_cierre_alerta.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCierreAlerta {
  /// Returns a new [EntradaCierreAlerta] instance.
  EntradaCierreAlerta({

    required  this.desenlace,
  });

      /// Sin desenlace la alerta no ensena nada y el modelo repite el error.
  @JsonKey(
    
    name: r'desenlace',
    required: true,
    includeIfNull: false,
  )


  final EntradaCierreAlertaDesenlaceEnum desenlace;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCierreAlerta &&
      other.desenlace == desenlace;

    @override
    int get hashCode =>
        desenlace.hashCode;

  factory EntradaCierreAlerta.fromJson(Map<String, dynamic> json) => _$EntradaCierreAlertaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCierreAlertaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Sin desenlace la alerta no ensena nada y el modelo repite el error.
enum EntradaCierreAlertaDesenlaceEnum {
    /// Sin desenlace la alerta no ensena nada y el modelo repite el error.
@JsonValue(r'CONFIRMADA')
CONFIRMADA(r'CONFIRMADA'),
    /// Sin desenlace la alerta no ensena nada y el modelo repite el error.
@JsonValue(r'DESCARTADA')
DESCARTADA(r'DESCARTADA');

const EntradaCierreAlertaDesenlaceEnum(this.value);

final String value;

@override
String toString() => value;
}


