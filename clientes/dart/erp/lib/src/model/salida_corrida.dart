//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_erp/src/model/salida_depreciacion.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_corrida.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaCorrida {
  /// Returns a new [SalidaCorrida] instance.
  SalidaCorrida({

    required  this.periodoId,

    required  this.depreciaciones,

    required  this.totalDepreciado,

    required  this.yaCorridos,

    required  this.totalmenteDepreciados,
  });

  @JsonKey(
    
    name: r'periodoId',
    required: true,
    includeIfNull: false,
  )


  final String periodoId;



  @JsonKey(
    
    name: r'depreciaciones',
    required: true,
    includeIfNull: false,
  )


  final List<SalidaDepreciacion> depreciaciones;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'totalDepreciado',
    required: true,
    includeIfNull: false,
  )


  final String totalDepreciado;



  @JsonKey(
    
    name: r'yaCorridos',
    required: true,
    includeIfNull: false,
  )


  final int yaCorridos;



  @JsonKey(
    
    name: r'totalmenteDepreciados',
    required: true,
    includeIfNull: false,
  )


  final int totalmenteDepreciados;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCorrida &&
      other.periodoId == periodoId &&
      other.depreciaciones == depreciaciones &&
      other.totalDepreciado == totalDepreciado &&
      other.yaCorridos == yaCorridos &&
      other.totalmenteDepreciados == totalmenteDepreciados;

    @override
    int get hashCode =>
        periodoId.hashCode +
        depreciaciones.hashCode +
        totalDepreciado.hashCode +
        yaCorridos.hashCode +
        totalmenteDepreciados.hashCode;

  factory SalidaCorrida.fromJson(Map<String, dynamic> json) => _$SalidaCorridaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCorridaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

