//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_anuncio.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaAnuncio {
  /// Returns a new [EntradaAnuncio] instance.
  EntradaAnuncio({

    required  this.conjuntoAnunciosId,

    required  this.piezaCreativaId,
  });

  @JsonKey(
    
    name: r'conjuntoAnunciosId',
    required: true,
    includeIfNull: false,
  )


  final String conjuntoAnunciosId;



  @JsonKey(
    
    name: r'piezaCreativaId',
    required: true,
    includeIfNull: false,
  )


  final String piezaCreativaId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaAnuncio &&
      other.conjuntoAnunciosId == conjuntoAnunciosId &&
      other.piezaCreativaId == piezaCreativaId;

    @override
    int get hashCode =>
        conjuntoAnunciosId.hashCode +
        piezaCreativaId.hashCode;

  factory EntradaAnuncio.fromJson(Map<String, dynamic> json) => _$EntradaAnuncioFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaAnuncioToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

