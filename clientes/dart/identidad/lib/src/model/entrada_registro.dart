//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_identidad/src/model/documento.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_registro.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRegistro {
  /// Returns a new [EntradaRegistro] instance.
  EntradaRegistro({

    required  this.telefonoE164,

    required  this.nombres,

    required  this.apellidos,

    required  this.fechaNacimiento,

    required  this.contrasena,

     this.correo,

     this.canalVerificacion,

    required  this.documento,

    required  this.aceptaContratos,
  });

  @JsonKey(
    
    name: r'telefonoE164',
    required: true,
    includeIfNull: false,
  )


  final String telefonoE164;



  @JsonKey(
    
    name: r'nombres',
    required: true,
    includeIfNull: false,
  )


  final String nombres;



  @JsonKey(
    
    name: r'apellidos',
    required: true,
    includeIfNull: false,
  )


  final String apellidos;



  @JsonKey(
    
    name: r'fechaNacimiento',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaNacimiento;



      /// La clave con la que se va a ingresar. Es obligatoria: el alta no creaba credencial y despues no habia con que entrar — se completaban los ocho pasos y el formulario de ingreso pedia una contrasena que nunca se eligio. El minimo de ocho es el de `PoliticaDeClave`; el resto de la politica —no derivada del telefono ni del documento— la aplica el servicio, que es el unico que puede.
  @JsonKey(
    
    name: r'contrasena',
    required: true,
    includeIfNull: false,
  )


  final String contrasena;



      /// Opcional en el contrato y pedido por la app. Una billetera manda extractos y comprobantes, y ese es su destino; ademas es lo que permite elegir el correo como canal de verificacion en vez del SMS.
  @JsonKey(
    
    name: r'correo',
    required: false,
    includeIfNull: false,
  )


  final String? correo;



      /// Por donde quiere recibir la verificacion quien se registra. No son dos formas de lo mismo: `SMS` verifica el TELEFONO y `CORREO` verifica el CORREO —dos propositos distintos del catalogo de tokens, con su propia vigencia y sus propios canales permitidos—. Elegir `CORREO` exige `correo`.
  @JsonKey(
    
    name: r'canalVerificacion',
    required: false,
    includeIfNull: false,
  )


  final EntradaRegistroCanalVerificacionEnum? canalVerificacion;



  @JsonKey(
    
    name: r'documento',
    required: true,
    includeIfNull: false,
  )


  final Documento documento;



  @JsonKey(
    
    name: r'aceptaContratos',
    required: true,
    includeIfNull: false,
  )


  final List<String> aceptaContratos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRegistro &&
      other.telefonoE164 == telefonoE164 &&
      other.nombres == nombres &&
      other.apellidos == apellidos &&
      other.fechaNacimiento == fechaNacimiento &&
      other.contrasena == contrasena &&
      other.correo == correo &&
      other.canalVerificacion == canalVerificacion &&
      other.documento == documento &&
      other.aceptaContratos == aceptaContratos;

    @override
    int get hashCode =>
        telefonoE164.hashCode +
        nombres.hashCode +
        apellidos.hashCode +
        fechaNacimiento.hashCode +
        contrasena.hashCode +
        correo.hashCode +
        canalVerificacion.hashCode +
        documento.hashCode +
        aceptaContratos.hashCode;

  factory EntradaRegistro.fromJson(Map<String, dynamic> json) => _$EntradaRegistroFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRegistroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Por donde quiere recibir la verificacion quien se registra. No son dos formas de lo mismo: `SMS` verifica el TELEFONO y `CORREO` verifica el CORREO —dos propositos distintos del catalogo de tokens, con su propia vigencia y sus propios canales permitidos—. Elegir `CORREO` exige `correo`.
enum EntradaRegistroCanalVerificacionEnum {
    /// Por donde quiere recibir la verificacion quien se registra. No son dos formas de lo mismo: `SMS` verifica el TELEFONO y `CORREO` verifica el CORREO —dos propositos distintos del catalogo de tokens, con su propia vigencia y sus propios canales permitidos—. Elegir `CORREO` exige `correo`.
@JsonValue(r'SMS')
SMS(r'SMS'),
    /// Por donde quiere recibir la verificacion quien se registra. No son dos formas de lo mismo: `SMS` verifica el TELEFONO y `CORREO` verifica el CORREO —dos propositos distintos del catalogo de tokens, con su propia vigencia y sus propios canales permitidos—. Elegir `CORREO` exige `correo`.
@JsonValue(r'CORREO')
CORREO(r'CORREO');

const EntradaRegistroCanalVerificacionEnum(this.value);

final String value;

@override
String toString() => value;
}


