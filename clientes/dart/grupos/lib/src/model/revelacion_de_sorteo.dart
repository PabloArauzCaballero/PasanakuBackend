//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'revelacion_de_sorteo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class RevelacionDeSorteo {
  /// Returns a new [RevelacionDeSorteo] instance.
  RevelacionDeSorteo({

    required  this.sorteoId,

    required  this.verificado,

     this.semilla,

    required  this.cuposEnOrden,
  });

  @JsonKey(
    
    name: r'sorteoId',
    required: true,
    includeIfNull: false,
  )


  final String sorteoId;



  @JsonKey(
    
    name: r'verificado',
    required: true,
    includeIfNull: false,
  )


  final bool verificado;



  @JsonKey(
    
    name: r'semilla',
    required: false,
    includeIfNull: false,
  )


  final String? semilla;



  @JsonKey(
    
    name: r'cuposEnOrden',
    required: true,
    includeIfNull: false,
  )


  final List<String> cuposEnOrden;





    @override
    bool operator ==(Object other) => identical(this, other) || other is RevelacionDeSorteo &&
      other.sorteoId == sorteoId &&
      other.verificado == verificado &&
      other.semilla == semilla &&
      other.cuposEnOrden == cuposEnOrden;

    @override
    int get hashCode =>
        sorteoId.hashCode +
        verificado.hashCode +
        semilla.hashCode +
        cuposEnOrden.hashCode;

  factory RevelacionDeSorteo.fromJson(Map<String, dynamic> json) => _$RevelacionDeSorteoFromJson(json);

  Map<String, dynamic> toJson() => _$RevelacionDeSorteoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

