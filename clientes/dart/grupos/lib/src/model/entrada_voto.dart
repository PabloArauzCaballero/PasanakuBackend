//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_voto.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaVoto {
  /// Returns a new [EntradaVoto] instance.
  EntradaVoto({

    required  this.voto,
  });

  @JsonKey(
    
    name: r'voto',
    required: true,
    includeIfNull: false,
  )


  final EntradaVotoVotoEnum voto;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaVoto &&
      other.voto == voto;

    @override
    int get hashCode =>
        voto.hashCode;

  factory EntradaVoto.fromJson(Map<String, dynamic> json) => _$EntradaVotoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaVotoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaVotoVotoEnum {
@JsonValue(r'A_FAVOR')
A_FAVOR(r'A_FAVOR'),
@JsonValue(r'EN_CONTRA')
EN_CONTRA(r'EN_CONTRA'),
@JsonValue(r'ABSTENCION')
ABSTENCION(r'ABSTENCION');

const EntradaVotoVotoEnum(this.value);

final String value;

@override
String toString() => value;
}


