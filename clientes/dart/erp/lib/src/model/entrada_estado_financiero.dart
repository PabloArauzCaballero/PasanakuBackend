//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_estado_financiero.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaEstadoFinanciero {
  /// Returns a new [EntradaEstadoFinanciero] instance.
  EntradaEstadoFinanciero({

    required  this.tipo,
  });

  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaEstadoFinancieroTipoEnum tipo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaEstadoFinanciero &&
      other.tipo == tipo;

    @override
    int get hashCode =>
        tipo.hashCode;

  factory EntradaEstadoFinanciero.fromJson(Map<String, dynamic> json) => _$EntradaEstadoFinancieroFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaEstadoFinancieroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaEstadoFinancieroTipoEnum {
@JsonValue(r'BALANCE_GENERAL')
BALANCE_GENERAL(r'BALANCE_GENERAL'),
@JsonValue(r'ESTADO_RESULTADOS')
ESTADO_RESULTADOS(r'ESTADO_RESULTADOS');

const EntradaEstadoFinancieroTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


