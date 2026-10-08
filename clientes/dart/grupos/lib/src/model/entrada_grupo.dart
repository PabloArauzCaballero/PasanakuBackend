//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_grupos/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_grupo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaGrupo {
  /// Returns a new [EntradaGrupo] instance.
  EntradaGrupo({

    required  this.nombre,

    required  this.montoAporte,

    required  this.periodicidad,

    required  this.cupos,

    required  this.diaCobro,

    required  this.modalidadTurnos,

    required  this.fechaDeInicio,

     this.organizadorId,

     this.permitePermutaDeTurnos = false,
  });

  @JsonKey(
    
    name: r'nombre',
    required: true,
    includeIfNull: false,
  )


  final String nombre;



  @JsonKey(
    
    name: r'montoAporte',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoAporte;



  @JsonKey(
    
    name: r'periodicidad',
    required: true,
    includeIfNull: false,
  )


  final EntradaGrupoPeriodicidadEnum periodicidad;



          // minimum: 3
          // maximum: 40
  @JsonKey(
    
    name: r'cupos',
    required: true,
    includeIfNull: false,
  )


  final int cupos;



          // minimum: 1
          // maximum: 28
  @JsonKey(
    
    name: r'diaCobro',
    required: true,
    includeIfNull: false,
  )


  final int diaCobro;



  @JsonKey(
    
    name: r'modalidadTurnos',
    required: true,
    includeIfNull: false,
  )


  final EntradaGrupoModalidadTurnosEnum modalidadTurnos;



  @JsonKey(
    
    name: r'fechaDeInicio',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaDeInicio;



      /// Quien lo administra, cuando lo administra alguien. Un grupo sin organizador lo lleva la plataforma, y entonces nadie cobra por administrarlo (RN-18).
  @JsonKey(
    
    name: r'organizadorId',
    required: false,
    includeIfNull: false,
  )


  final String? organizadorId;



  @JsonKey(
    defaultValue: false,
    name: r'permitePermutaDeTurnos',
    required: false,
    includeIfNull: false,
  )


  final bool? permitePermutaDeTurnos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaGrupo &&
      other.nombre == nombre &&
      other.montoAporte == montoAporte &&
      other.periodicidad == periodicidad &&
      other.cupos == cupos &&
      other.diaCobro == diaCobro &&
      other.modalidadTurnos == modalidadTurnos &&
      other.fechaDeInicio == fechaDeInicio &&
      other.organizadorId == organizadorId &&
      other.permitePermutaDeTurnos == permitePermutaDeTurnos;

    @override
    int get hashCode =>
        nombre.hashCode +
        montoAporte.hashCode +
        periodicidad.hashCode +
        cupos.hashCode +
        diaCobro.hashCode +
        modalidadTurnos.hashCode +
        fechaDeInicio.hashCode +
        (organizadorId == null ? 0 : organizadorId.hashCode) +
        permitePermutaDeTurnos.hashCode;

  factory EntradaGrupo.fromJson(Map<String, dynamic> json) => _$EntradaGrupoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaGrupoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaGrupoPeriodicidadEnum {
@JsonValue(r'SEMANAL')
SEMANAL(r'SEMANAL'),
@JsonValue(r'QUINCENAL')
QUINCENAL(r'QUINCENAL'),
@JsonValue(r'MENSUAL')
MENSUAL(r'MENSUAL'),
@JsonValue(r'BIMENSUAL')
BIMENSUAL(r'BIMENSUAL');

const EntradaGrupoPeriodicidadEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaGrupoModalidadTurnosEnum {
@JsonValue(r'SORTEO_ALEATORIO')
SORTEO_ALEATORIO(r'SORTEO_ALEATORIO'),
@JsonValue(r'ORDEN_DE_INGRESO')
ORDEN_DE_INGRESO(r'ORDEN_DE_INGRESO'),
@JsonValue(r'ACUERDO_MANUAL')
ACUERDO_MANUAL(r'ACUERDO_MANUAL');

const EntradaGrupoModalidadTurnosEnum(this.value);

final String value;

@override
String toString() => value;
}


