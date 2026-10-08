//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_publicacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaPublicacion {
  /// Returns a new [EntradaPublicacion] instance.
  EntradaPublicacion({

    required  this.tarifarioBaseId,

    required  this.nombre,

    required  this.tipoCambio,

    required  this.diasPreaviso,

    required  this.aprobadoPor,

    required  this.actaComite,

    required  this.urlPublicacion,

    required  this.hashDocumento,

    required  this.canalAviso,

     this.permiteRescisionSinCosto = false,

     this.deltaIngresoEstimado,

     this.usuariosImpactados,
  });

  @JsonKey(
    
    name: r'tarifarioBaseId',
    required: true,
    includeIfNull: false,
  )


  final String tarifarioBaseId;



  @JsonKey(
    
    name: r'nombre',
    required: true,
    includeIfNull: false,
  )


  final String nombre;



  @JsonKey(
    
    name: r'tipoCambio',
    required: true,
    includeIfNull: false,
  )


  final EntradaPublicacionTipoCambioEnum tipoCambio;



          // minimum: 0
          // maximum: 90
  @JsonKey(
    
    name: r'diasPreaviso',
    required: true,
    includeIfNull: false,
  )


  final int diasPreaviso;



  @JsonKey(
    
    name: r'aprobadoPor',
    required: true,
    includeIfNull: false,
  )


  final String aprobadoPor;



  @JsonKey(
    
    name: r'actaComite',
    required: true,
    includeIfNull: false,
  )


  final String actaComite;



  @JsonKey(
    
    name: r'urlPublicacion',
    required: true,
    includeIfNull: false,
  )


  final String urlPublicacion;



  @JsonKey(
    
    name: r'hashDocumento',
    required: true,
    includeIfNull: false,
  )


  final String hashDocumento;



  @JsonKey(
    
    name: r'canalAviso',
    required: true,
    includeIfNull: false,
  )


  final String canalAviso;



  @JsonKey(
    defaultValue: false,
    name: r'permiteRescisionSinCosto',
    required: false,
    includeIfNull: false,
  )


  final bool? permiteRescisionSinCosto;



  @JsonKey(
    
    name: r'deltaIngresoEstimado',
    required: false,
    includeIfNull: false,
  )


  final String? deltaIngresoEstimado;



          // minimum: 0
  @JsonKey(
    
    name: r'usuariosImpactados',
    required: false,
    includeIfNull: false,
  )


  final int? usuariosImpactados;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaPublicacion &&
      other.tarifarioBaseId == tarifarioBaseId &&
      other.nombre == nombre &&
      other.tipoCambio == tipoCambio &&
      other.diasPreaviso == diasPreaviso &&
      other.aprobadoPor == aprobadoPor &&
      other.actaComite == actaComite &&
      other.urlPublicacion == urlPublicacion &&
      other.hashDocumento == hashDocumento &&
      other.canalAviso == canalAviso &&
      other.permiteRescisionSinCosto == permiteRescisionSinCosto &&
      other.deltaIngresoEstimado == deltaIngresoEstimado &&
      other.usuariosImpactados == usuariosImpactados;

    @override
    int get hashCode =>
        tarifarioBaseId.hashCode +
        nombre.hashCode +
        tipoCambio.hashCode +
        diasPreaviso.hashCode +
        aprobadoPor.hashCode +
        actaComite.hashCode +
        urlPublicacion.hashCode +
        hashDocumento.hashCode +
        canalAviso.hashCode +
        permiteRescisionSinCosto.hashCode +
        deltaIngresoEstimado.hashCode +
        usuariosImpactados.hashCode;

  factory EntradaPublicacion.fromJson(Map<String, dynamic> json) => _$EntradaPublicacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaPublicacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaPublicacionTipoCambioEnum {
@JsonValue(r'INCREMENTO')
INCREMENTO(r'INCREMENTO'),
@JsonValue(r'REDUCCION')
REDUCCION(r'REDUCCION'),
@JsonValue(r'NUEVO_CONCEPTO')
NUEVO_CONCEPTO(r'NUEVO_CONCEPTO'),
@JsonValue(r'ELIMINACION')
ELIMINACION(r'ELIMINACION');

const EntradaPublicacionTipoCambioEnum(this.value);

final String value;

@override
String toString() => value;
}


