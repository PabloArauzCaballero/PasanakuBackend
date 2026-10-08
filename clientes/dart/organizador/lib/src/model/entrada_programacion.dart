//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_programacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaProgramacion {
  /// Returns a new [EntradaProgramacion] instance.
  EntradaProgramacion({

    required  this.reglaId,

    required  this.grupoId,

    required  this.programadaPara,
  });

  @JsonKey(
    
    name: r'reglaId',
    required: true,
    includeIfNull: false,
  )


  final String reglaId;



  @JsonKey(
    
    name: r'grupoId',
    required: true,
    includeIfNull: false,
  )


  final String grupoId;



  @JsonKey(
    
    name: r'programadaPara',
    required: true,
    includeIfNull: false,
  )


  final DateTime programadaPara;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaProgramacion &&
      other.reglaId == reglaId &&
      other.grupoId == grupoId &&
      other.programadaPara == programadaPara;

    @override
    int get hashCode =>
        reglaId.hashCode +
        grupoId.hashCode +
        programadaPara.hashCode;

  factory EntradaProgramacion.fromJson(Map<String, dynamic> json) => _$EntradaProgramacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaProgramacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

