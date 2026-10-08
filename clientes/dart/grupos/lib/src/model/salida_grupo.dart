//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_grupo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaGrupo {
  /// Returns a new [SalidaGrupo] instance.
  SalidaGrupo({

    required  this.grupoId,

    required  this.fondoPorPeriodo,
  });

  @JsonKey(
    
    name: r'grupoId',
    required: true,
    includeIfNull: false,
  )


  final String grupoId;



  @JsonKey(
    
    name: r'fondoPorPeriodo',
    required: true,
    includeIfNull: false,
  )


  final String fondoPorPeriodo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaGrupo &&
      other.grupoId == grupoId &&
      other.fondoPorPeriodo == fondoPorPeriodo;

    @override
    int get hashCode =>
        grupoId.hashCode +
        fondoPorPeriodo.hashCode;

  factory SalidaGrupo.fromJson(Map<String, dynamic> json) => _$SalidaGrupoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaGrupoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

