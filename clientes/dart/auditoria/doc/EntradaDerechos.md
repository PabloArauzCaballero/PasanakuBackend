# aportaya_cliente_auditoria.model.EntradaDerechos

## Load the model package
```dart
import 'package:aportaya_cliente_auditoria/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**usuarioId** | **String** |  | 
**tipo** | **String** | Los cinco que admite la tabla. CU-07 llama «supresion» a CANCELACION y el servicio acepta las dos palabras; el modelo guarda CANCELACION. | 
**descripcion** | **String** |  | 
**ultimaActividad** | [**DateTime**](DateTime.md) | Desde cuando corre la retencion. Lo aporta quien atiende porque la ultima operacion del titular vive en `nucleo-financiero` y este servicio no la puede consultar. Hueco declarado: deja de ser parametro cuando el contrato de la billetera exponga la fecha. | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


