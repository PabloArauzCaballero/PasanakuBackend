//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_extracto.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaExtracto {
  /// Returns a new [SalidaExtracto] instance.
  SalidaExtracto({

    required  this.cuentaBilleteraId,

    required  this.desde,

    required  this.hasta,

    required  this.saldoFinal,

     this.cantidadMovimientos,

     this.hashArchivo,

    required  this.emitido,

     this.motivoDelBloqueo,
  });

  @JsonKey(
    
    name: r'cuentaBilleteraId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaBilleteraId;



  @JsonKey(
    
    name: r'desde',
    required: true,
    includeIfNull: false,
  )


  final DateTime desde;



  @JsonKey(
    
    name: r'hasta',
    required: true,
    includeIfNull: false,
  )


  final DateTime hasta;



  @JsonKey(
    
    name: r'saldoFinal',
    required: true,
    includeIfNull: false,
  )


  final Dinero saldoFinal;



  @JsonKey(
    
    name: r'cantidadMovimientos',
    required: false,
    includeIfNull: false,
  )


  final int? cantidadMovimientos;



  @JsonKey(
    
    name: r'hashArchivo',
    required: false,
    includeIfNull: false,
  )


  final String? hashArchivo;



  @JsonKey(
    
    name: r'emitido',
    required: true,
    includeIfNull: false,
  )


  final bool emitido;



  @JsonKey(
    
    name: r'motivoDelBloqueo',
    required: false,
    includeIfNull: false,
  )


  final String? motivoDelBloqueo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaExtracto &&
      other.cuentaBilleteraId == cuentaBilleteraId &&
      other.desde == desde &&
      other.hasta == hasta &&
      other.saldoFinal == saldoFinal &&
      other.cantidadMovimientos == cantidadMovimientos &&
      other.hashArchivo == hashArchivo &&
      other.emitido == emitido &&
      other.motivoDelBloqueo == motivoDelBloqueo;

    @override
    int get hashCode =>
        cuentaBilleteraId.hashCode +
        desde.hashCode +
        hasta.hashCode +
        saldoFinal.hashCode +
        cantidadMovimientos.hashCode +
        hashArchivo.hashCode +
        emitido.hashCode +
        motivoDelBloqueo.hashCode;

  factory SalidaExtracto.fromJson(Map<String, dynamic> json) => _$SalidaExtractoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaExtractoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

