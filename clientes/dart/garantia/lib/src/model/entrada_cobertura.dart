//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_cobertura.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCobertura {
  /// Returns a new [EntradaCobertura] instance.
  EntradaCobertura({

    required  this.montoSolicitado,

    required  this.diasMora,

     this.aprobadaPor,
  });

  @JsonKey(
    
    name: r'montoSolicitado',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoSolicitado;



          // minimum: 0
  @JsonKey(
    
    name: r'diasMora',
    required: true,
    includeIfNull: false,
  )


  final int diasMora;



      /// obligatorio por encima del umbral de la politica. Automatizar la cobertura de montos grandes es como se vacia un fondo sin que nadie mire
  @JsonKey(
    
    name: r'aprobadaPor',
    required: false,
    includeIfNull: false,
  )


  final String? aprobadaPor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCobertura &&
      other.montoSolicitado == montoSolicitado &&
      other.diasMora == diasMora &&
      other.aprobadaPor == aprobadaPor;

    @override
    int get hashCode =>
        montoSolicitado.hashCode +
        diasMora.hashCode +
        (aprobadaPor == null ? 0 : aprobadaPor.hashCode);

  factory EntradaCobertura.fromJson(Map<String, dynamic> json) => _$EntradaCoberturaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCoberturaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

