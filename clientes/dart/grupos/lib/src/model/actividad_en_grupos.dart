//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'actividad_en_grupos.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ActividadEnGrupos {
  /// Returns a new [ActividadEnGrupos] instance.
  ActividadEnGrupos({

    required  this.gruposActivos,
  });

  @JsonKey(
    
    name: r'gruposActivos',
    required: true,
    includeIfNull: false,
  )


  final int gruposActivos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ActividadEnGrupos &&
      other.gruposActivos == gruposActivos;

    @override
    int get hashCode =>
        gruposActivos.hashCode;

  factory ActividadEnGrupos.fromJson(Map<String, dynamic> json) => _$ActividadEnGruposFromJson(json);

  Map<String, dynamic> toJson() => _$ActividadEnGruposToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

