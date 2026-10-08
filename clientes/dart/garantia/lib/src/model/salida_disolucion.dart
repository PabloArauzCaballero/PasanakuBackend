//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/liquidacion_de_participante.dart';
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_disolucion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaDisolucion {
  /// Returns a new [SalidaDisolucion] instance.
  SalidaDisolucion({

    required  this.disolucionId,

    required  this.estado,

    required  this.masaARepartir,

    required  this.totalADevolver,

    required  this.totalACobrar,

    required  this.liquidaciones,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'disolucionId',
    required: true,
    includeIfNull: false,
  )


  final String disolucionId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final String estado;



  @JsonKey(
    
    name: r'masaARepartir',
    required: true,
    includeIfNull: false,
  )


  final Dinero masaARepartir;



  @JsonKey(
    
    name: r'totalADevolver',
    required: true,
    includeIfNull: false,
  )


  final Dinero totalADevolver;



  @JsonKey(
    
    name: r'totalACobrar',
    required: true,
    includeIfNull: false,
  )


  final Dinero totalACobrar;



  @JsonKey(
    
    name: r'liquidaciones',
    required: true,
    includeIfNull: false,
  )


  final List<LiquidacionDeParticipante> liquidaciones;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaDisolucion &&
      other.disolucionId == disolucionId &&
      other.estado == estado &&
      other.masaARepartir == masaARepartir &&
      other.totalADevolver == totalADevolver &&
      other.totalACobrar == totalACobrar &&
      other.liquidaciones == liquidaciones &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        disolucionId.hashCode +
        estado.hashCode +
        masaARepartir.hashCode +
        totalADevolver.hashCode +
        totalACobrar.hashCode +
        liquidaciones.hashCode +
        esNueva.hashCode;

  factory SalidaDisolucion.fromJson(Map<String, dynamic> json) => _$SalidaDisolucionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaDisolucionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

