# aportaya_cliente_auditoria.model.SalidaTablero

## Load the model package
```dart
import 'package:aportaya_cliente_auditoria/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**periodo** | **String** |  | 
**provisorio** | **bool** | Verdadero si algun indicador del periodo se calculo sin que los cierres cuadraran. Es del tablero entero y no de cada tarjeta: si uno solo es provisorio, la lectura de todos queda condicionada. | 
**dimension** | **String** |  | 
**dimensionId** | **String** |  | [optional] 
**indicadores** | [**List&lt;Indicador&gt;**](Indicador.md) |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


