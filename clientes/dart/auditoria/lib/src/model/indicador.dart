//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_auditoria/src/model/indicador_serie_inner.dart';
import 'package:json_annotation/json_annotation.dart';

part 'indicador.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Indicador {
  /// Returns a new [Indicador] instance.
  Indicador({

    required  this.codigo,

    required  this.nombre,

     this.valor,

    required  this.unidad,

     this.meta,

     this.cumpleMeta,

     this.variacionPeriodoAnterior,

     this.serie,

    required  this.familia,

    required  this.duenoFamilia,

    required  this.definicionVersion,

    required  this.suprimidoPorPrivacidad,

     this.casos,

     this.minimoCasos,

    required  this.calculadoEn,
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



      /// Cadena decimal, con la escala que guardo el calculo. No se redondea al publicar: el numero del tablero tiene que ser el numero de la tabla. AUSENTE cuando se suprimio por privacidad. La serie tambien viene vacia en ese caso: publicarla seria dar el numero por la puerta de atras.
  @JsonKey(
    
    name: r'valor',
    required: false,
    includeIfNull: false,
  )


  final String? valor;



  @JsonKey(
    
    name: r'unidad',
    required: true,
    includeIfNull: false,
  )


  final String unidad;



      /// Ausente cuando no hay meta fijada para el periodo. Sin meta no hay semaforo, y ponerla despues de ver el resultado no es medir.
  @JsonKey(
    
    name: r'meta',
    required: false,
    includeIfNull: false,
  )


  final String? meta;



      /// Ausente cuando no hay meta.
  @JsonKey(
    
    name: r'cumpleMeta',
    required: false,
    includeIfNull: false,
  )


  final bool? cumpleMeta;



      /// Ausente en un indicador sin historia. Sin periodo anterior no hay comparacion, y un cero fingido afirmaria que se mantuvo igual.
  @JsonKey(
    
    name: r'variacionPeriodoAnterior',
    required: false,
    includeIfNull: false,
  )


  final String? variacionPeriodoAnterior;



      /// Los periodos anteriores, del mas viejo al mas nuevo.
  @JsonKey(
    
    name: r'serie',
    required: false,
    includeIfNull: false,
  )


  final List<IndicadorSerieInner>? serie;



      /// Sale del catalogo de definiciones del servicio, no de la tabla: `indicador_kpi` no tiene columna de familia (hueco declarado).
  @JsonKey(
    
    name: r'familia',
    required: true,
    includeIfNull: false,
  )


  final IndicadorFamiliaEnum familia;



      /// Quien responde cuando el indicador esta en rojo.
  @JsonKey(
    
    name: r'duenoFamilia',
    required: true,
    includeIfNull: false,
  )


  final String duenoFamilia;



      /// Con que version de la definicion se calculo. Es lo que permite que un numero de hace un ano vuelva a salir igual.
  @JsonKey(
    
    name: r'definicionVersion',
    required: true,
    includeIfNull: false,
  )


  final String definicionVersion;



      /// Verdadero cuando la muestra no alcanza el minimo de la definicion. Un promedio de tres personas identifica a las tres (R-SEG-03).
  @JsonKey(
    
    name: r'suprimidoPorPrivacidad',
    required: true,
    includeIfNull: false,
  )


  final bool suprimidoPorPrivacidad;



      /// Tamano de la muestra, para poder explicar la supresion.
  @JsonKey(
    
    name: r'casos',
    required: false,
    includeIfNull: false,
  )


  final int? casos;



      /// El piso que fija la definicion del indicador.
  @JsonKey(
    
    name: r'minimoCasos',
    required: false,
    includeIfNull: false,
  )


  final int? minimoCasos;



  @JsonKey(
    
    name: r'calculadoEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime calculadoEn;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Indicador &&
      other.codigo == codigo &&
      other.nombre == nombre &&
      other.valor == valor &&
      other.unidad == unidad &&
      other.meta == meta &&
      other.cumpleMeta == cumpleMeta &&
      other.variacionPeriodoAnterior == variacionPeriodoAnterior &&
      other.serie == serie &&
      other.familia == familia &&
      other.duenoFamilia == duenoFamilia &&
      other.definicionVersion == definicionVersion &&
      other.suprimidoPorPrivacidad == suprimidoPorPrivacidad &&
      other.casos == casos &&
      other.minimoCasos == minimoCasos &&
      other.calculadoEn == calculadoEn;

    @override
    int get hashCode =>
        codigo.hashCode +
        nombre.hashCode +
        valor.hashCode +
        unidad.hashCode +
        meta.hashCode +
        cumpleMeta.hashCode +
        variacionPeriodoAnterior.hashCode +
        serie.hashCode +
        familia.hashCode +
        duenoFamilia.hashCode +
        definicionVersion.hashCode +
        suprimidoPorPrivacidad.hashCode +
        casos.hashCode +
        minimoCasos.hashCode +
        calculadoEn.hashCode;

  factory Indicador.fromJson(Map<String, dynamic> json) => _$IndicadorFromJson(json);

  Map<String, dynamic> toJson() => _$IndicadorToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Sale del catalogo de definiciones del servicio, no de la tabla: `indicador_kpi` no tiene columna de familia (hueco declarado).
enum IndicadorFamiliaEnum {
    /// Sale del catalogo de definiciones del servicio, no de la tabla: `indicador_kpi` no tiene columna de familia (hueco declarado).
@JsonValue(r'NEGOCIO')
NEGOCIO(r'NEGOCIO'),
    /// Sale del catalogo de definiciones del servicio, no de la tabla: `indicador_kpi` no tiene columna de familia (hueco declarado).
@JsonValue(r'RIESGO')
RIESGO(r'RIESGO'),
    /// Sale del catalogo de definiciones del servicio, no de la tabla: `indicador_kpi` no tiene columna de familia (hueco declarado).
@JsonValue(r'CUMPLIMIENTO')
CUMPLIMIENTO(r'CUMPLIMIENTO'),
    /// Sale del catalogo de definiciones del servicio, no de la tabla: `indicador_kpi` no tiene columna de familia (hueco declarado).
@JsonValue(r'OPERACION')
OPERACION(r'OPERACION'),
    /// Sale del catalogo de definiciones del servicio, no de la tabla: `indicador_kpi` no tiene columna de familia (hueco declarado).
@JsonValue(r'FINANZAS')
FINANZAS(r'FINANZAS');

const IndicadorFamiliaEnum(this.value);

final String value;

@override
String toString() => value;
}


