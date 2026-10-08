//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_tarifas/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_factura.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaFactura {
  /// Returns a new [EntradaFactura] instance.
  EntradaFactura({

    required  this.devengoId,

    required  this.montoIva,

     this.urlPdf,
  });

  @JsonKey(
    
    name: r'devengoId',
    required: true,
    includeIfNull: false,
  )


  final String devengoId;



  @JsonKey(
    
    name: r'montoIva',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoIva;



  @JsonKey(
    
    name: r'urlPdf',
    required: false,
    includeIfNull: false,
  )


  final String? urlPdf;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaFactura &&
      other.devengoId == devengoId &&
      other.montoIva == montoIva &&
      other.urlPdf == urlPdf;

    @override
    int get hashCode =>
        devengoId.hashCode +
        montoIva.hashCode +
        (urlPdf == null ? 0 : urlPdf.hashCode);

  factory EntradaFactura.fromJson(Map<String, dynamic> json) => _$EntradaFacturaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaFacturaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

