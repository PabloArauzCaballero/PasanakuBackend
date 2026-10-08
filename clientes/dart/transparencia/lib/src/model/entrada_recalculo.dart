//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_transparencia/src/model/indicadores.dart';
import 'package:aportaya_cliente_transparencia/src/model/medicion.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_recalculo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRecalculo {
  /// Returns a new [EntradaRecalculo] instance.
  EntradaRecalculo({

    required  this.usuarioId,

    required  this.mediciones,

     this.medicionesAnteriores,

    required  this.indicadores,
  });

  @JsonKey(
    
    name: r'usuarioId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioId;



  @JsonKey(
    
    name: r'mediciones',
    required: true,
    includeIfNull: false,
  )


  final List<Medicion> mediciones;



      /// Para la tendencia. Alguien con 60 subiendo esta mejor que alguien con 65 cayendo.
  @JsonKey(
    
    name: r'medicionesAnteriores',
    required: false,
    includeIfNull: false,
  )


  final List<Medicion>? medicionesAnteriores;



  @JsonKey(
    
    name: r'indicadores',
    required: true,
    includeIfNull: false,
  )


  final Indicadores indicadores;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRecalculo &&
      other.usuarioId == usuarioId &&
      other.mediciones == mediciones &&
      other.medicionesAnteriores == medicionesAnteriores &&
      other.indicadores == indicadores;

    @override
    int get hashCode =>
        usuarioId.hashCode +
        mediciones.hashCode +
        medicionesAnteriores.hashCode +
        indicadores.hashCode;

  factory EntradaRecalculo.fromJson(Map<String, dynamic> json) => _$EntradaRecalculoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRecalculoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

