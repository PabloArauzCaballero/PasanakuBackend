# aportaya_cliente_entregas.model.EntradaRespuestaDesembolso

## Load the model package
```dart
import 'package:aportaya_cliente_entregas/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**iniciado** | [**DateTime**](DateTime.md) |  | 
**exitoso** | **bool** |  | 
**referenciaProveedor** | **String** | obligatoria si fue exitoso — es contra lo que cruza el extracto | [optional] 
**codigoError** | **String** | CUENTA_INEXISTENTE, CUENTA_CERRADA, TITULAR_NO_COINCIDE y MONEDA_INCOMPATIBLE son definitivos: no se reintentan | [optional] 
**mensajeProveedor** | **String** |  | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


