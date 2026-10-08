//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_transferencia_destino.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaTransferenciaDestino {
  /// Returns a new [EntradaTransferenciaDestino] instance.
  EntradaTransferenciaDestino({

    required  this.tipo,

    required  this.valor,
  });

  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaTransferenciaDestinoTipoEnum tipo;



  @JsonKey(
    
    name: r'valor',
    required: true,
    includeIfNull: false,
  )


  final String valor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaTransferenciaDestino &&
      other.tipo == tipo &&
      other.valor == valor;

    @override
    int get hashCode =>
        tipo.hashCode +
        valor.hashCode;

  factory EntradaTransferenciaDestino.fromJson(Map<String, dynamic> json) => _$EntradaTransferenciaDestinoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaTransferenciaDestinoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaTransferenciaDestinoTipoEnum {
@JsonValue(r'ALIAS')
ALIAS(r'ALIAS'),
@JsonValue(r'GRUPO')
GRUPO(r'GRUPO');

const EntradaTransferenciaDestinoTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


