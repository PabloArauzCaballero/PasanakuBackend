//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_regla.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRegla {
  /// Returns a new [EntradaRegla] instance.
  EntradaRegla({

    required  this.codigo,

    required  this.descripcion,

    required  this.disparador,

    required  this.expresionDisparo,

    required  this.condicion,

    required  this.accion,

     this.requiereConfirmacionHumana = false,

    required  this.prioridad,
  });

  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(
    
    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;



  @JsonKey(
    
    name: r'disparador',
    required: true,
    includeIfNull: false,
  )


  final EntradaReglaDisparadorEnum disparador;



  @JsonKey(
    
    name: r'expresionDisparo',
    required: true,
    includeIfNull: false,
  )


  final String expresionDisparo;



      /// obligatoria. Una regla sin condicion se dispara siempre
  @JsonKey(
    
    name: r'condicion',
    required: true,
    includeIfNull: false,
  )


  final String condicion;



  @JsonKey(
    
    name: r'accion',
    required: true,
    includeIfNull: false,
  )


  final EntradaReglaAccionEnum accion;



      /// obligatorio en true para APLICAR_MORA, EJECUTAR_ENTREGA, ESCALAR_COBRANZA y LIQUIDAR_PERIODO. La regla se rechaza si viene sin el, en vez de forzarlo callado: forzarlo deja a quien la escribio creyendo que definio otra cosa
  @JsonKey(
    defaultValue: false,
    name: r'requiereConfirmacionHumana',
    required: false,
    includeIfNull: false,
  )


  final bool? requiereConfirmacionHumana;



      /// unica entre las activas del mismo disparador
          // minimum: 1
          // maximum: 999
  @JsonKey(
    
    name: r'prioridad',
    required: true,
    includeIfNull: false,
  )


  final int prioridad;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRegla &&
      other.codigo == codigo &&
      other.descripcion == descripcion &&
      other.disparador == disparador &&
      other.expresionDisparo == expresionDisparo &&
      other.condicion == condicion &&
      other.accion == accion &&
      other.requiereConfirmacionHumana == requiereConfirmacionHumana &&
      other.prioridad == prioridad;

    @override
    int get hashCode =>
        codigo.hashCode +
        descripcion.hashCode +
        disparador.hashCode +
        expresionDisparo.hashCode +
        condicion.hashCode +
        accion.hashCode +
        requiereConfirmacionHumana.hashCode +
        prioridad.hashCode;

  factory EntradaRegla.fromJson(Map<String, dynamic> json) => _$EntradaReglaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaReglaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaReglaDisparadorEnum {
@JsonValue(r'CRON')
CRON(r'CRON'),
@JsonValue(r'EVENTO')
EVENTO(r'EVENTO');

const EntradaReglaDisparadorEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaReglaAccionEnum {
@JsonValue(r'APLICAR_MORA')
APLICAR_MORA(r'APLICAR_MORA'),
@JsonValue(r'EJECUTAR_ENTREGA')
EJECUTAR_ENTREGA(r'EJECUTAR_ENTREGA'),
@JsonValue(r'ENVIAR_RECORDATORIO')
ENVIAR_RECORDATORIO(r'ENVIAR_RECORDATORIO'),
@JsonValue(r'ESCALAR_COBRANZA')
ESCALAR_COBRANZA(r'ESCALAR_COBRANZA'),
@JsonValue(r'GENERAR_COBROS')
GENERAR_COBROS(r'GENERAR_COBROS'),
@JsonValue(r'LIQUIDAR_PERIODO')
LIQUIDAR_PERIODO(r'LIQUIDAR_PERIODO');

const EntradaReglaAccionEnum(this.value);

final String value;

@override
String toString() => value;
}


