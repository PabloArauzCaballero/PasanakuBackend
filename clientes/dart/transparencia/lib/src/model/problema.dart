//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'problema.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Problema {
  /// Returns a new [Problema] instance.
  Problema({

    required  this.tipo,

    required  this.titulo,

    required  this.estado,

    required  this.codigo,

     this.detalle,
  });

  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final String tipo;



  @JsonKey(
    
    name: r'titulo',
    required: true,
    includeIfNull: false,
  )


  final String titulo;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final int estado;



  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(
    
    name: r'detalle',
    required: false,
    includeIfNull: false,
  )


  final String? detalle;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Problema &&
      other.tipo == tipo &&
      other.titulo == titulo &&
      other.estado == estado &&
      other.codigo == codigo &&
      other.detalle == detalle;

    @override
    int get hashCode =>
        tipo.hashCode +
        titulo.hashCode +
        estado.hashCode +
        codigo.hashCode +
        (detalle == null ? 0 : detalle.hashCode);

  factory Problema.fromJson(Map<String, dynamic> json) => _$ProblemaFromJson(json);

  Map<String, dynamic> toJson() => _$ProblemaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

