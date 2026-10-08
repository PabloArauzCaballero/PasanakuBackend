//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_certificado.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCertificado {
  /// Returns a new [EntradaCertificado] instance.
  EntradaCertificado({

    required  this.usuarioId,

    required  this.snapshotId,

    required  this.disponibles,

    required  this.incluir,

     this.vigenciaDias = 90,

    required  this.identidadVerificada,
  });

  @JsonKey(
    
    name: r'usuarioId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioId;



  @JsonKey(
    
    name: r'snapshotId',
    required: true,
    includeIfNull: false,
  )


  final String snapshotId;



      /// Todo lo que se PODRIA mostrar, ya resuelto desde la foto.
  @JsonKey(
    
    name: r'disponibles',
    required: true,
    includeIfNull: false,
  )


  final Map<String, String> disponibles;



      /// Lo que el titular eligio. Lo demas no entra ni al hash.
  @JsonKey(
    
    name: r'incluir',
    required: true,
    includeIfNull: false,
  )


  final List<String> incluir;



          // minimum: 7
          // maximum: 180
  @JsonKey(
    defaultValue: 90,
    name: r'vigenciaDias',
    required: true,
    includeIfNull: false,
  )


  final int vigenciaDias;



  @JsonKey(
    
    name: r'identidadVerificada',
    required: true,
    includeIfNull: false,
  )


  final bool identidadVerificada;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCertificado &&
      other.usuarioId == usuarioId &&
      other.snapshotId == snapshotId &&
      other.disponibles == disponibles &&
      other.incluir == incluir &&
      other.vigenciaDias == vigenciaDias &&
      other.identidadVerificada == identidadVerificada;

    @override
    int get hashCode =>
        usuarioId.hashCode +
        snapshotId.hashCode +
        disponibles.hashCode +
        incluir.hashCode +
        vigenciaDias.hashCode +
        identidadVerificada.hashCode;

  factory EntradaCertificado.fromJson(Map<String, dynamic> json) => _$EntradaCertificadoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCertificadoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

