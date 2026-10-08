//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_sancion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaSancion {
  /// Returns a new [SalidaSancion] instance.
  SalidaSancion({

    required  this.sancionId,

    required  this.tipo,

     this.estadoDelOrganizador,

    required  this.puedeApelarHasta,
  });

  @JsonKey(
    
    name: r'sancionId',
    required: true,
    includeIfNull: false,
  )


  final String sancionId;



  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final String tipo;



  @JsonKey(
    
    name: r'estadoDelOrganizador',
    required: false,
    includeIfNull: false,
  )


  final String? estadoDelOrganizador;



      /// sale del momento en que se aplico la sancion; no se recalcula
  @JsonKey(
    
    name: r'puedeApelarHasta',
    required: true,
    includeIfNull: false,
  )


  final DateTime puedeApelarHasta;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaSancion &&
      other.sancionId == sancionId &&
      other.tipo == tipo &&
      other.estadoDelOrganizador == estadoDelOrganizador &&
      other.puedeApelarHasta == puedeApelarHasta;

    @override
    int get hashCode =>
        sancionId.hashCode +
        tipo.hashCode +
        (estadoDelOrganizador == null ? 0 : estadoDelOrganizador.hashCode) +
        puedeApelarHasta.hashCode;

  factory SalidaSancion.fromJson(Map<String, dynamic> json) => _$SalidaSancionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaSancionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

