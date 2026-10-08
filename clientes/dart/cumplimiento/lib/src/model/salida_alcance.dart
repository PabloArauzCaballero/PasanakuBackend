//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_cumplimiento/src/model/salida_alcance_limites_sandbox.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_alcance.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaAlcance {
  /// Returns a new [SalidaAlcance] instance.
  SalidaAlcance({

    required  this.habilitado,

    required  this.via,

     this.limitesSandbox,

     this.motivo,
  });

  @JsonKey(

    name: r'habilitado',
    required: true,
    includeIfNull: false,
  )


  final bool habilitado;



  @JsonKey(

    name: r'via',
    required: true,
    includeIfNull: false,
  )


  final SalidaAlcanceViaEnum via;



  @JsonKey(

    name: r'limitesSandbox',
    required: false,
    includeIfNull: false,
  )


  final SalidaAlcanceLimitesSandbox? limitesSandbox;



  @JsonKey(

    name: r'motivo',
    required: false,
    includeIfNull: false,
  )


  final String? motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaAlcance &&
      other.habilitado == habilitado &&
      other.via == via &&
      other.limitesSandbox == limitesSandbox &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        habilitado.hashCode +
        via.hashCode +
        limitesSandbox.hashCode +
        motivo.hashCode;

  factory SalidaAlcance.fromJson(Map<String, dynamic> json) => _$SalidaAlcanceFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaAlcanceToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaAlcanceViaEnum {
@JsonValue(r'LICENCIA')
LICENCIA(r'LICENCIA'),
@JsonValue(r'SANDBOX')
SANDBOX(r'SANDBOX'),
@JsonValue(r'NINGUNA')
NINGUNA(r'NINGUNA');

const SalidaAlcanceViaEnum(this.value);

final String value;

@override
String toString() => value;
}


