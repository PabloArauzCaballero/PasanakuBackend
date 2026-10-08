//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'metrica.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Metrica {
  /// Returns a new [Metrica] instance.
  Metrica({

    required  this.codigo,

    required  this.valor,

    required  this.meta,

    required  this.peso,

    required  this.mayorEsMejor,
  });

  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(
    
    name: r'valor',
    required: true,
    includeIfNull: false,
  )


  final String valor;



  @JsonKey(
    
    name: r'meta',
    required: true,
    includeIfNull: false,
  )


  final String meta;



  @JsonKey(
    
    name: r'peso',
    required: true,
    includeIfNull: false,
  )


  final String peso;



      /// false para morosidad y tiempo de respuesta. Sin esto, la cartera sana puntuaria igual que la rota
  @JsonKey(
    
    name: r'mayorEsMejor',
    required: true,
    includeIfNull: false,
  )


  final bool mayorEsMejor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Metrica &&
      other.codigo == codigo &&
      other.valor == valor &&
      other.meta == meta &&
      other.peso == peso &&
      other.mayorEsMejor == mayorEsMejor;

    @override
    int get hashCode =>
        codigo.hashCode +
        valor.hashCode +
        meta.hashCode +
        peso.hashCode +
        mayorEsMejor.hashCode;

  factory Metrica.fromJson(Map<String, dynamic> json) => _$MetricaFromJson(json);

  Map<String, dynamic> toJson() => _$MetricaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

