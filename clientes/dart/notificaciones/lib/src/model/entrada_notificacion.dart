//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_notificacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaNotificacion {
  /// Returns a new [EntradaNotificacion] instance.
  EntradaNotificacion({

    required  this.destinatarioId,

    required  this.evento,

    required  this.datos,
  });

  @JsonKey(
    
    name: r'destinatarioId',
    required: true,
    includeIfNull: false,
  )


  final String destinatarioId;



      /// Codigo de evento_notificable; la plantilla la resuelve el servicio
  @JsonKey(
    
    name: r'evento',
    required: true,
    includeIfNull: false,
  )


  final String evento;



      /// Variables de la plantilla, ya resueltas por el emisor
  @JsonKey(
    
    name: r'datos',
    required: true,
    includeIfNull: false,
  )


  final Object datos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaNotificacion &&
      other.destinatarioId == destinatarioId &&
      other.evento == evento &&
      other.datos == datos;

    @override
    int get hashCode =>
        destinatarioId.hashCode +
        evento.hashCode +
        datos.hashCode;

  factory EntradaNotificacion.fromJson(Map<String, dynamic> json) => _$EntradaNotificacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaNotificacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

