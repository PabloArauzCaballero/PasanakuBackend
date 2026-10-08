//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'paquete_del_sorteo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PaqueteDelSorteo {
  /// Returns a new [PaqueteDelSorteo] instance.
  PaqueteDelSorteo({

    required  this.sorteoId,

    required  this.hashComprometido,

     this.semillaRevelada,

     this.entropias,

    required  this.metodo,

    required  this.cuposEnOrdenOriginal,

    required  this.ordenPublicado,
  });

  @JsonKey(
    
    name: r'sorteoId',
    required: true,
    includeIfNull: false,
  )


  final String sorteoId;



  @JsonKey(
    
    name: r'hashComprometido',
    required: true,
    includeIfNull: false,
  )


  final String hashComprometido;



  @JsonKey(
    
    name: r'semillaRevelada',
    required: false,
    includeIfNull: false,
  )


  final String? semillaRevelada;



  @JsonKey(
    
    name: r'entropias',
    required: false,
    includeIfNull: false,
  )


  final List<String>? entropias;



  @JsonKey(
    
    name: r'metodo',
    required: true,
    includeIfNull: false,
  )


  final String metodo;



  @JsonKey(
    
    name: r'cuposEnOrdenOriginal',
    required: true,
    includeIfNull: false,
  )


  final List<int> cuposEnOrdenOriginal;



  @JsonKey(
    
    name: r'ordenPublicado',
    required: true,
    includeIfNull: false,
  )


  final List<int> ordenPublicado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PaqueteDelSorteo &&
      other.sorteoId == sorteoId &&
      other.hashComprometido == hashComprometido &&
      other.semillaRevelada == semillaRevelada &&
      other.entropias == entropias &&
      other.metodo == metodo &&
      other.cuposEnOrdenOriginal == cuposEnOrdenOriginal &&
      other.ordenPublicado == ordenPublicado;

    @override
    int get hashCode =>
        sorteoId.hashCode +
        hashComprometido.hashCode +
        (semillaRevelada == null ? 0 : semillaRevelada.hashCode) +
        entropias.hashCode +
        metodo.hashCode +
        cuposEnOrdenOriginal.hashCode +
        ordenPublicado.hashCode;

  factory PaqueteDelSorteo.fromJson(Map<String, dynamic> json) => _$PaqueteDelSorteoFromJson(json);

  Map<String, dynamic> toJson() => _$PaqueteDelSorteoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

