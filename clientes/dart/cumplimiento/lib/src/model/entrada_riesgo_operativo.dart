//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_riesgo_operativo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRiesgoOperativo {
  /// Returns a new [EntradaRiesgoOperativo] instance.
  EntradaRiesgoOperativo({

    required  this.categoriaEvento,

    required  this.factorRiesgo,

    required  this.lineaNegocio,

    required  this.descripcion,

    required  this.fechaOcurrencia,

    required  this.fechaDeteccion,

    required  this.perdidaBruta,

     this.recuperacion,

    required  this.moneda,

     this.responsableId,

     this.accionComprometida,

     this.fechaCompromiso,
  });

      /// Las seis de la norma. No hay «otros» a proposito: una septima categoria cajon se lleva todo lo incomodo y el analisis por categoria deja de decir nada.
  @JsonKey(

    name: r'categoriaEvento',
    required: true,
    includeIfNull: false,
  )


  final EntradaRiesgoOperativoCategoriaEventoEnum categoriaEvento;



  @JsonKey(

    name: r'factorRiesgo',
    required: true,
    includeIfNull: false,
  )


  final EntradaRiesgoOperativoFactorRiesgoEnum factorRiesgo;



  @JsonKey(

    name: r'lineaNegocio',
    required: true,
    includeIfNull: false,
  )


  final String lineaNegocio;



  @JsonKey(

    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;



  @JsonKey(

    name: r'fechaOcurrencia',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaOcurrencia;



      /// Nunca anterior a la ocurrencia. Es el error de carga mas comun, y una fecha invertida rompe el unico indicador que de verdad importa: cuanto tardamos en darnos cuenta.
  @JsonKey(

    name: r'fechaDeteccion',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaDeteccion;



      /// CADENA decimal. Cero es valido: la casi-perdida se registra igual, porque la frecuencia tambien es informacion.
  @JsonKey(

    name: r'perdidaBruta',
    required: true,
    includeIfNull: false,
  )


  final String perdidaBruta;



      /// Lo ya recuperado al registrar. La recuperacion POSTERIOR no edita el evento: la tabla es append-only y la perdida bruta no se toca.
  @JsonKey(

    name: r'recuperacion',
    required: false,
    includeIfNull: false,
  )


  final String? recuperacion;



  @JsonKey(

    name: r'moneda',
    required: true,
    includeIfNull: false,
  )


  final EntradaRiesgoOperativoMonedaEnum moneda;



      /// Sin responsable no hay plan de accion, y el evento se registra igual. Es peor perder el registro que registrar sin plan.
  @JsonKey(

    name: r'responsableId',
    required: false,
    includeIfNull: false,
  )


  final String? responsableId;



  @JsonKey(

    name: r'accionComprometida',
    required: false,
    includeIfNull: false,
  )


  final String? accionComprometida;



  @JsonKey(

    name: r'fechaCompromiso',
    required: false,
    includeIfNull: false,
  )


  final DateTime? fechaCompromiso;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRiesgoOperativo &&
      other.categoriaEvento == categoriaEvento &&
      other.factorRiesgo == factorRiesgo &&
      other.lineaNegocio == lineaNegocio &&
      other.descripcion == descripcion &&
      other.fechaOcurrencia == fechaOcurrencia &&
      other.fechaDeteccion == fechaDeteccion &&
      other.perdidaBruta == perdidaBruta &&
      other.recuperacion == recuperacion &&
      other.moneda == moneda &&
      other.responsableId == responsableId &&
      other.accionComprometida == accionComprometida &&
      other.fechaCompromiso == fechaCompromiso;

    @override
    int get hashCode =>
        categoriaEvento.hashCode +
        factorRiesgo.hashCode +
        lineaNegocio.hashCode +
        descripcion.hashCode +
        fechaOcurrencia.hashCode +
        fechaDeteccion.hashCode +
        perdidaBruta.hashCode +
        recuperacion.hashCode +
        moneda.hashCode +
        responsableId.hashCode +
        accionComprometida.hashCode +
        fechaCompromiso.hashCode;

  factory EntradaRiesgoOperativo.fromJson(Map<String, dynamic> json) => _$EntradaRiesgoOperativoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRiesgoOperativoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Las seis de la norma. No hay «otros» a proposito: una septima categoria cajon se lleva todo lo incomodo y el analisis por categoria deja de decir nada.
enum EntradaRiesgoOperativoCategoriaEventoEnum {
    /// Las seis de la norma. No hay «otros» a proposito: una septima categoria cajon se lleva todo lo incomodo y el analisis por categoria deja de decir nada.
@JsonValue(r'FRAUDE_INTERNO')
FRAUDE_INTERNO(r'FRAUDE_INTERNO'),
    /// Las seis de la norma. No hay «otros» a proposito: una septima categoria cajon se lleva todo lo incomodo y el analisis por categoria deja de decir nada.
@JsonValue(r'FRAUDE_EXTERNO')
FRAUDE_EXTERNO(r'FRAUDE_EXTERNO'),
    /// Las seis de la norma. No hay «otros» a proposito: una septima categoria cajon se lleva todo lo incomodo y el analisis por categoria deja de decir nada.
@JsonValue(r'RELACIONES_LABORALES')
RELACIONES_LABORALES(r'RELACIONES_LABORALES'),
    /// Las seis de la norma. No hay «otros» a proposito: una septima categoria cajon se lleva todo lo incomodo y el analisis por categoria deja de decir nada.
@JsonValue(r'CLIENTES_PRODUCTOS_PRACTICAS')
CLIENTES_PRODUCTOS_PRACTICAS(r'CLIENTES_PRODUCTOS_PRACTICAS'),
    /// Las seis de la norma. No hay «otros» a proposito: una septima categoria cajon se lleva todo lo incomodo y el analisis por categoria deja de decir nada.
@JsonValue(r'DANOS_ACTIVOS')
DANOS_ACTIVOS(r'DANOS_ACTIVOS'),
    /// Las seis de la norma. No hay «otros» a proposito: una septima categoria cajon se lleva todo lo incomodo y el analisis por categoria deja de decir nada.
@JsonValue(r'FALLAS_SISTEMAS')
FALLAS_SISTEMAS(r'FALLAS_SISTEMAS');

const EntradaRiesgoOperativoCategoriaEventoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaRiesgoOperativoFactorRiesgoEnum {
@JsonValue(r'PROCESOS_INTERNOS')
PROCESOS_INTERNOS(r'PROCESOS_INTERNOS'),
@JsonValue(r'PERSONAS')
PERSONAS(r'PERSONAS'),
@JsonValue(r'TECNOLOGIA_INFORMACION')
TECNOLOGIA_INFORMACION(r'TECNOLOGIA_INFORMACION'),
@JsonValue(r'EVENTOS_EXTERNOS')
EVENTOS_EXTERNOS(r'EVENTOS_EXTERNOS'),
@JsonValue(r'INFRAESTRUCTURA')
INFRAESTRUCTURA(r'INFRAESTRUCTURA');

const EntradaRiesgoOperativoFactorRiesgoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaRiesgoOperativoMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const EntradaRiesgoOperativoMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}


