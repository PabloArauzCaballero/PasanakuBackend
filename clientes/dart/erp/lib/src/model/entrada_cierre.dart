//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_cierre.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCierre {
  /// Returns a new [EntradaCierre] instance.
  EntradaCierre({

    required  this.glosa,
  });

  @JsonKey(
    
    name: r'glosa',
    required: true,
    includeIfNull: false,
  )


  final String glosa;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCierre &&
      other.glosa == glosa;

    @override
    int get hashCode =>
        glosa.hashCode;

  factory EntradaCierre.fromJson(Map<String, dynamic> json) => _$EntradaCierreFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCierreToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

