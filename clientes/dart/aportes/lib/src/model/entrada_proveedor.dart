//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_proveedor.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaProveedor {
  /// Returns a new [EntradaProveedor] instance.
  EntradaProveedor({

    required  this.codigo,

    required  this.nombre,

    required  this.tipo,

    required  this.urlBase,

    required  this.referenciaCredenciales,

    required  this.comisionFija,

    required  this.comisionPorcentual,

     this.soportaWebhook = false,

     this.soportaConsultaEstado = false,

    required  this.prioridad,

    required  this.tieneContratoVigente,

    required  this.pruebasCompletas,
  });

  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(
    
    name: r'nombre',
    required: true,
    includeIfNull: false,
  )


  final String nombre;



  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaProveedorTipoEnum tipo;



  @JsonKey(
    
    name: r'urlBase',
    required: true,
    includeIfNull: false,
  )


  final String urlBase;



      /// REFERENCIA al almacen de secretos (vault://...). Enviar la credencial en claro se rechaza con AP-CU99-05 antes de tocar la base.
  @JsonKey(
    
    name: r'referenciaCredenciales',
    required: true,
    includeIfNull: false,
  )


  final String referenciaCredenciales;



  @JsonKey(
    
    name: r'comisionFija',
    required: true,
    includeIfNull: false,
  )


  final String comisionFija;



  @JsonKey(
    
    name: r'comisionPorcentual',
    required: true,
    includeIfNull: false,
  )


  final String comisionPorcentual;



  @JsonKey(
    defaultValue: false,
    name: r'soportaWebhook',
    required: false,
    includeIfNull: false,
  )


  final bool? soportaWebhook;



  @JsonKey(
    defaultValue: false,
    name: r'soportaConsultaEstado',
    required: false,
    includeIfNull: false,
  )


  final bool? soportaConsultaEstado;



          // minimum: 1
          // maximum: 999
  @JsonKey(
    
    name: r'prioridad',
    required: true,
    includeIfNull: false,
  )


  final int prioridad;



  @JsonKey(
    
    name: r'tieneContratoVigente',
    required: true,
    includeIfNull: false,
  )


  final bool tieneContratoVigente;



  @JsonKey(
    
    name: r'pruebasCompletas',
    required: true,
    includeIfNull: false,
  )


  final bool pruebasCompletas;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaProveedor &&
      other.codigo == codigo &&
      other.nombre == nombre &&
      other.tipo == tipo &&
      other.urlBase == urlBase &&
      other.referenciaCredenciales == referenciaCredenciales &&
      other.comisionFija == comisionFija &&
      other.comisionPorcentual == comisionPorcentual &&
      other.soportaWebhook == soportaWebhook &&
      other.soportaConsultaEstado == soportaConsultaEstado &&
      other.prioridad == prioridad &&
      other.tieneContratoVigente == tieneContratoVigente &&
      other.pruebasCompletas == pruebasCompletas;

    @override
    int get hashCode =>
        codigo.hashCode +
        nombre.hashCode +
        tipo.hashCode +
        urlBase.hashCode +
        referenciaCredenciales.hashCode +
        comisionFija.hashCode +
        comisionPorcentual.hashCode +
        soportaWebhook.hashCode +
        soportaConsultaEstado.hashCode +
        prioridad.hashCode +
        tieneContratoVigente.hashCode +
        pruebasCompletas.hashCode;

  factory EntradaProveedor.fromJson(Map<String, dynamic> json) => _$EntradaProveedorFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaProveedorToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaProveedorTipoEnum {
@JsonValue(r'BANCO')
BANCO(r'BANCO'),
@JsonValue(r'BILLETERA')
BILLETERA(r'BILLETERA'),
@JsonValue(r'PASARELA')
PASARELA(r'PASARELA');

const EntradaProveedorTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


