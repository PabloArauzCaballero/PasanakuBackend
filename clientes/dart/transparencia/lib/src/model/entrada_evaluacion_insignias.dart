//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_transparencia/src/model/hechos_del_usuario.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_evaluacion_insignias.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaEvaluacionInsignias {
  /// Returns a new [EntradaEvaluacionInsignias] instance.
  EntradaEvaluacionInsignias({

    required  this.usuarioId,

     this.tipoDeEvento,

     this.codigosAEvaluar,

    required  this.hechos,
  });

  @JsonKey(
    
    name: r'usuarioId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioId;



  @JsonKey(
    
    name: r'tipoDeEvento',
    required: false,
    includeIfNull: false,
  )


  final String? tipoDeEvento;



  @JsonKey(
    
    name: r'codigosAEvaluar',
    required: false,
    includeIfNull: false,
  )


  final List<String>? codigosAEvaluar;



  @JsonKey(
    
    name: r'hechos',
    required: true,
    includeIfNull: false,
  )


  final HechosDelUsuario hechos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaEvaluacionInsignias &&
      other.usuarioId == usuarioId &&
      other.tipoDeEvento == tipoDeEvento &&
      other.codigosAEvaluar == codigosAEvaluar &&
      other.hechos == hechos;

    @override
    int get hashCode =>
        usuarioId.hashCode +
        (tipoDeEvento == null ? 0 : tipoDeEvento.hashCode) +
        codigosAEvaluar.hashCode +
        hechos.hashCode;

  factory EntradaEvaluacionInsignias.fromJson(Map<String, dynamic> json) => _$EntradaEvaluacionInsigniasFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaEvaluacionInsigniasToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

