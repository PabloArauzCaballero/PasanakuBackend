//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'enlace_de_foto.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EnlaceDeFoto {
  /// Returns a new [EnlaceDeFoto] instance.
  EnlaceDeFoto({

    required  this.url,

    required  this.vigenteHasta,
  });

  @JsonKey(
    
    name: r'url',
    required: true,
    includeIfNull: false,
  )


  final String url;



  @JsonKey(
    
    name: r'vigenteHasta',
    required: true,
    includeIfNull: false,
  )


  final DateTime vigenteHasta;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EnlaceDeFoto &&
      other.url == url &&
      other.vigenteHasta == vigenteHasta;

    @override
    int get hashCode =>
        url.hashCode +
        vigenteHasta.hashCode;

  factory EnlaceDeFoto.fromJson(Map<String, dynamic> json) => _$EnlaceDeFotoFromJson(json);

  Map<String, dynamic> toJson() => _$EnlaceDeFotoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

