//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_reverso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaReverso {
  /// Returns a new [SalidaReverso] instance.
  SalidaReverso({

    required  this.reversoId,

     this.transaccionReversoId,

    required  this.generaObligacionDeRestitucion,
  });

  @JsonKey(
    
    name: r'reversoId',
    required: true,
    includeIfNull: false,
  )


  final String reversoId;



  @JsonKey(
    
    name: r'transaccionReversoId',
    required: false,
    includeIfNull: false,
  )


  final String? transaccionReversoId;



  @JsonKey(
    
    name: r'generaObligacionDeRestitucion',
    required: true,
    includeIfNull: false,
  )


  final bool generaObligacionDeRestitucion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaReverso &&
      other.reversoId == reversoId &&
      other.transaccionReversoId == transaccionReversoId &&
      other.generaObligacionDeRestitucion == generaObligacionDeRestitucion;

    @override
    int get hashCode =>
        reversoId.hashCode +
        transaccionReversoId.hashCode +
        generaObligacionDeRestitucion.hashCode;

  factory SalidaReverso.fromJson(Map<String, dynamic> json) => _$SalidaReversoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaReversoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

