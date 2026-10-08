//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_reporte.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaReporte {
  /// Returns a new [EntradaReporte] instance.
  EntradaReporte({

    required  this.parametros,

     this.formato,

     this.justificacion,
  });

      /// Se validan contra `parametros_esperados` de la definicion. Viajan como cadena a proposito: el tipo lo declara el reporte, no el llamador, y asi no hay dos opiniones sobre si «2026-01» es una fecha o un texto.
  @JsonKey(
    
    name: r'parametros',
    required: true,
    includeIfNull: false,
  )


  final Map<String, String> parametros;



      /// Ausente para ejecutar sin exportar. Los cuatro que admite `ck_exportacion_reporte_formato`.
  @JsonKey(
    
    name: r'formato',
    required: false,
    includeIfNull: false,
  )


  final EntradaReporteFormatoEnum? formato;



      /// Obligatoria cuando la definicion tiene `contiene_datos_sensibles`. El minimo de diez no es capricho: lo exige `ck_acceso_justificacion`, porque una justificacion de tres letras no se puede leer dentro de un ano.
  @JsonKey(
    
    name: r'justificacion',
    required: false,
    includeIfNull: false,
  )


  final String? justificacion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaReporte &&
      other.parametros == parametros &&
      other.formato == formato &&
      other.justificacion == justificacion;

    @override
    int get hashCode =>
        parametros.hashCode +
        formato.hashCode +
        justificacion.hashCode;

  factory EntradaReporte.fromJson(Map<String, dynamic> json) => _$EntradaReporteFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaReporteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Ausente para ejecutar sin exportar. Los cuatro que admite `ck_exportacion_reporte_formato`.
enum EntradaReporteFormatoEnum {
    /// Ausente para ejecutar sin exportar. Los cuatro que admite `ck_exportacion_reporte_formato`.
@JsonValue(r'CSV')
CSV(r'CSV'),
    /// Ausente para ejecutar sin exportar. Los cuatro que admite `ck_exportacion_reporte_formato`.
@JsonValue(r'JSON')
JSON(r'JSON'),
    /// Ausente para ejecutar sin exportar. Los cuatro que admite `ck_exportacion_reporte_formato`.
@JsonValue(r'PDF')
PDF(r'PDF'),
    /// Ausente para ejecutar sin exportar. Los cuatro que admite `ck_exportacion_reporte_formato`.
@JsonValue(r'XLSX')
XLSX(r'XLSX');

const EntradaReporteFormatoEnum(this.value);

final String value;

@override
String toString() => value;
}


