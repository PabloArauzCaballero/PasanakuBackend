//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_ejecucion_aval.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaEjecucionAval {
  /// Returns a new [SalidaEjecucionAval] instance.
  SalidaEjecucionAval({

     this.ejecucionId,

     this.subrogacionId,

    required  this.montoEjecutado,

     this.avalistaUsuarioId,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'ejecucionId',
    required: false,
    includeIfNull: false,
  )


  final String? ejecucionId;



  @JsonKey(
    
    name: r'subrogacionId',
    required: false,
    includeIfNull: false,
  )


  final String? subrogacionId;



  @JsonKey(
    
    name: r'montoEjecutado',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoEjecutado;



  @JsonKey(
    
    name: r'avalistaUsuarioId',
    required: false,
    includeIfNull: false,
  )


  final String? avalistaUsuarioId;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaEjecucionAval &&
      other.ejecucionId == ejecucionId &&
      other.subrogacionId == subrogacionId &&
      other.montoEjecutado == montoEjecutado &&
      other.avalistaUsuarioId == avalistaUsuarioId &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        (ejecucionId == null ? 0 : ejecucionId.hashCode) +
        (subrogacionId == null ? 0 : subrogacionId.hashCode) +
        montoEjecutado.hashCode +
        (avalistaUsuarioId == null ? 0 : avalistaUsuarioId.hashCode) +
        esNueva.hashCode;

  factory SalidaEjecucionAval.fromJson(Map<String, dynamic> json) => _$SalidaEjecucionAvalFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaEjecucionAvalToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

