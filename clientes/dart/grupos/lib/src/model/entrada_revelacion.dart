//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_revelacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRevelacion {
  /// Returns a new [EntradaRevelacion] instance.
  EntradaRevelacion({

    required  this.sorteoId,

    required  this.semilla,

     this.entropias,
  });

  @JsonKey(
    
    name: r'sorteoId',
    required: true,
    includeIfNull: false,
  )


  final String sorteoId;



      /// La que se comprometio. Se devuelve aca porque el compromiso la entrego una sola vez: guardarla del lado del servidor entre las dos fases haria posible cambiarla, que es justo lo que el protocolo evita.
  @JsonKey(
    
    name: r'semilla',
    required: true,
    includeIfNull: false,
  )


  final String semilla;



  @JsonKey(
    
    name: r'entropias',
    required: false,
    includeIfNull: false,
  )


  final List<String>? entropias;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRevelacion &&
      other.sorteoId == sorteoId &&
      other.semilla == semilla &&
      other.entropias == entropias;

    @override
    int get hashCode =>
        sorteoId.hashCode +
        semilla.hashCode +
        entropias.hashCode;

  factory EntradaRevelacion.fromJson(Map<String, dynamic> json) => _$EntradaRevelacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRevelacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

