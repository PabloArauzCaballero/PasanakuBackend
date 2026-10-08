//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_conversion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaConversion {
  /// Returns a new [EntradaConversion] instance.
  EntradaConversion({

     this.clicId,

     this.impresionId,

    required  this.tipo,

     this.referenciaId,
  });

  @JsonKey(
    
    name: r'clicId',
    required: false,
    includeIfNull: false,
  )


  final String? clicId;



  @JsonKey(
    
    name: r'impresionId',
    required: false,
    includeIfNull: false,
  )


  final String? impresionId;



  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaConversionTipoEnum tipo;



  @JsonKey(
    
    name: r'referenciaId',
    required: false,
    includeIfNull: false,
  )


  final String? referenciaId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaConversion &&
      other.clicId == clicId &&
      other.impresionId == impresionId &&
      other.tipo == tipo &&
      other.referenciaId == referenciaId;

    @override
    int get hashCode =>
        (clicId == null ? 0 : clicId.hashCode) +
        (impresionId == null ? 0 : impresionId.hashCode) +
        tipo.hashCode +
        (referenciaId == null ? 0 : referenciaId.hashCode);

  factory EntradaConversion.fromJson(Map<String, dynamic> json) => _$EntradaConversionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaConversionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaConversionTipoEnum {
@JsonValue(r'POSTULACION_GRUPO')
POSTULACION_GRUPO(r'POSTULACION_GRUPO'),
@JsonValue(r'REGISTRO')
REGISTRO(r'REGISTRO'),
@JsonValue(r'DESCARGA_APP')
DESCARGA_APP(r'DESCARGA_APP');

const EntradaConversionTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


