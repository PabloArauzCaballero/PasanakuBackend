# aportaya_cliente_tarifas.model.EntradaDevolucion

## Load the model package
```dart
import 'package:aportaya_cliente_tarifas/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**motivo** | **String** |  | 
**detalle** | **String** |  | 
**monto** | [**Dinero**](Dinero.md) |  | 
**forma** | **String** |  | 
**reclamoId** | **String** | obligatorio cuando nace de un reclamo favorable con monto (R-CON-04) | [optional] 
**autorizadaPor** | **String** | quien autoriza no es quien ejecuta (R-SEG-04) | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


