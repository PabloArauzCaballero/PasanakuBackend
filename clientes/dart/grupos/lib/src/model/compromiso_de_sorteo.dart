//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'compromiso_de_sorteo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class CompromisoDeSorteo {
  /// Returns a new [CompromisoDeSorteo] instance.
  CompromisoDeSorteo({

    required  this.sorteoId,

    required  this.hashSemilla,

    required  this.algoritmo,
  });

  @JsonKey(
    
    name: r'sorteoId',
    required: true,
    includeIfNull: false,
  )


  final String sorteoId;



  @JsonKey(
    
    name: r'hashSemilla',
    required: true,
    includeIfNull: false,
  )


  final String hashSemilla;



  @JsonKey(
    
    name: r'algoritmo',
    required: true,
    includeIfNull: false,
  )


  final CompromisoDeSorteoAlgoritmoEnum algoritmo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is CompromisoDeSorteo &&
      other.sorteoId == sorteoId &&
      other.hashSemilla == hashSemilla &&
      other.algoritmo == algoritmo;

    @override
    int get hashCode =>
        sorteoId.hashCode +
        hashSemilla.hashCode +
        algoritmo.hashCode;

  factory CompromisoDeSorteo.fromJson(Map<String, dynamic> json) => _$CompromisoDeSorteoFromJson(json);

  Map<String, dynamic> toJson() => _$CompromisoDeSorteoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum CompromisoDeSorteoAlgoritmoEnum {
@JsonValue(r'FISHER_YATES_SHA256')
fISHERYATESSHA256(r'FISHER_YATES_SHA256');

const CompromisoDeSorteoAlgoritmoEnum(this.value);

final String value;

@override
String toString() => value;
}


