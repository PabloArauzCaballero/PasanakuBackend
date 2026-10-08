//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'bloque_publicado.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class BloquePublicado {
  /// Returns a new [BloquePublicado] instance.
  BloquePublicado({

    required  this.numeroBloque,

    required  this.hashAnterior,

    required  this.raizMerkle,

    required  this.hashBloque,

    required  this.desde,

    required  this.hasta,

    required  this.cantidadEventos,
  });

  @JsonKey(
    
    name: r'numeroBloque',
    required: true,
    includeIfNull: false,
  )


  final int numeroBloque;



      /// SHA-256 en hexadecimal minuscula.
  @JsonKey(
    
    name: r'hashAnterior',
    required: true,
    includeIfNull: false,
  )


  final String hashAnterior;



      /// SHA-256 en hexadecimal minuscula.
  @JsonKey(
    
    name: r'raizMerkle',
    required: true,
    includeIfNull: false,
  )


  final String raizMerkle;



      /// SHA-256 en hexadecimal minuscula.
  @JsonKey(
    
    name: r'hashBloque',
    required: true,
    includeIfNull: false,
  )


  final String hashBloque;



  @JsonKey(
    
    name: r'desde',
    required: true,
    includeIfNull: false,
  )


  final DateTime desde;



  @JsonKey(
    
    name: r'hasta',
    required: true,
    includeIfNull: false,
  )


  final DateTime hasta;



          // minimum: 0
  @JsonKey(
    
    name: r'cantidadEventos',
    required: true,
    includeIfNull: false,
  )


  final int cantidadEventos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is BloquePublicado &&
      other.numeroBloque == numeroBloque &&
      other.hashAnterior == hashAnterior &&
      other.raizMerkle == raizMerkle &&
      other.hashBloque == hashBloque &&
      other.desde == desde &&
      other.hasta == hasta &&
      other.cantidadEventos == cantidadEventos;

    @override
    int get hashCode =>
        numeroBloque.hashCode +
        hashAnterior.hashCode +
        raizMerkle.hashCode +
        hashBloque.hashCode +
        desde.hashCode +
        hasta.hashCode +
        cantidadEventos.hashCode;

  factory BloquePublicado.fromJson(Map<String, dynamic> json) => _$BloquePublicadoFromJson(json);

  Map<String, dynamic> toJson() => _$BloquePublicadoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

