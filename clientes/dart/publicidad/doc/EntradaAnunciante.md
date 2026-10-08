# aportaya_cliente_publicidad.model.EntradaAnunciante

## Load the model package
```dart
import 'package:aportaya_cliente_publicidad/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**tipo** | **String** |  | 
**organizadorId** | **String** | Obligatorio si tipo es ORGANIZADOR, y prohibido si no (R-PUB-01). | [optional] 
**socioComercialId** | **String** | Obligatorio si tipo es SOCIO_COMERCIAL, y prohibido si no. | [optional] 
**razonSocialFacturacion** | **String** |  | 
**limiteGastoMensual** | **String** | Importe como CADENA decimal. Un number JSON es un doble. | [optional] 
**moneda** | **String** |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


