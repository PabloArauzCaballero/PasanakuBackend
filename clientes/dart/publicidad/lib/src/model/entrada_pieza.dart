//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_pieza.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaPieza {
  /// Returns a new [EntradaPieza] instance.
  EntradaPieza({

    required  this.anuncianteId,

    required  this.titulo,

     this.texto,

    required  this.urlRecurso,

    required  this.tipoRecurso,
  });

  @JsonKey(
    
    name: r'anuncianteId',
    required: true,
    includeIfNull: false,
  )


  final String anuncianteId;



  @JsonKey(
    
    name: r'titulo',
    required: true,
    includeIfNull: false,
  )


  final String titulo;



  @JsonKey(
    
    name: r'texto',
    required: false,
    includeIfNull: false,
  )


  final String? texto;



  @JsonKey(
    
    name: r'urlRecurso',
    required: true,
    includeIfNull: false,
  )


  final String urlRecurso;



  @JsonKey(
    
    name: r'tipoRecurso',
    required: true,
    includeIfNull: false,
  )


  final EntradaPiezaTipoRecursoEnum tipoRecurso;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaPieza &&
      other.anuncianteId == anuncianteId &&
      other.titulo == titulo &&
      other.texto == texto &&
      other.urlRecurso == urlRecurso &&
      other.tipoRecurso == tipoRecurso;

    @override
    int get hashCode =>
        anuncianteId.hashCode +
        titulo.hashCode +
        (texto == null ? 0 : texto.hashCode) +
        urlRecurso.hashCode +
        tipoRecurso.hashCode;

  factory EntradaPieza.fromJson(Map<String, dynamic> json) => _$EntradaPiezaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaPiezaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaPiezaTipoRecursoEnum {
@JsonValue(r'IMAGEN')
IMAGEN(r'IMAGEN'),
@JsonValue(r'VIDEO')
VIDEO(r'VIDEO');

const EntradaPiezaTipoRecursoEnum(this.value);

final String value;

@override
String toString() => value;
}


