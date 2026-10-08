# aportaya_cliente_transparencia.model.SalidaSellado

## Load the model package
```dart
import 'package:aportaya_cliente_transparencia/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**bloqueId** | **String** |  | 
**numeroBloque** | **int** |  | 
**hashAnterior** | **String** | 64 ceros en el primer bloque del grupo. Nunca nulo, porque la columna es NOT NULL. | 
**raizMerkle** | **String** | SHA-256 en hexadecimal minuscula. | 
**hashBloque** | **String** | SHA-256 en hexadecimal minuscula. | 
**entidadesSelladas** | **int** |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


