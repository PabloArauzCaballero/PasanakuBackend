//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'indicador_serie_inner.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class IndicadorSerieInner {
  /// Returns a new [IndicadorSerieInner] instance.
  IndicadorSerieInner({

    required  this.periodo,

    required  this.valor,
  });

  @JsonKey(
    
    name: r'periodo',
    required: true,
    includeIfNull: false,
  )


  final String periodo;



  @JsonKey(
    
    name: r'valor',
    required: true,
    includeIfNull: false,
  )


  final String valor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is IndicadorSerieInner &&
      other.periodo == periodo &&
      other.valor == valor;

    @override
    int get hashCode =>
        periodo.hashCode +
        valor.hashCode;

  factory IndicadorSerieInner.fromJson(Map<String, dynamic> json) => _$IndicadorSerieInnerFromJson(json);

  Map<String, dynamic> toJson() => _$IndicadorSerieInnerToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

