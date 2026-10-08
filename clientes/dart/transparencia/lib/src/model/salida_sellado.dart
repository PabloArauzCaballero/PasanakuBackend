//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_sellado.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaSellado {
  /// Returns a new [SalidaSellado] instance.
  SalidaSellado({

    required  this.bloqueId,

    required  this.numeroBloque,

    required  this.hashAnterior,

    required  this.raizMerkle,

    required  this.hashBloque,

    required  this.entidadesSelladas,
  });

  @JsonKey(
    
    name: r'bloqueId',
    required: true,
    includeIfNull: false,
  )


  final String bloqueId;



  @JsonKey(
    
    name: r'numeroBloque',
    required: true,
    includeIfNull: false,
  )


  final int numeroBloque;



      /// 64 ceros en el primer bloque del grupo. Nunca nulo, porque la columna es NOT NULL.
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



          // minimum: 0
  @JsonKey(
    
    name: r'entidadesSelladas',
    required: true,
    includeIfNull: false,
  )


  final int entidadesSelladas;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaSellado &&
      other.bloqueId == bloqueId &&
      other.numeroBloque == numeroBloque &&
      other.hashAnterior == hashAnterior &&
      other.raizMerkle == raizMerkle &&
      other.hashBloque == hashBloque &&
      other.entidadesSelladas == entidadesSelladas;

    @override
    int get hashCode =>
        bloqueId.hashCode +
        numeroBloque.hashCode +
        hashAnterior.hashCode +
        raizMerkle.hashCode +
        hashBloque.hashCode +
        entidadesSelladas.hashCode;

  factory SalidaSellado.fromJson(Map<String, dynamic> json) => _$SalidaSelladoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaSelladoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

