# aportaya_cliente_organizador.model.EntradaRegla

## Load the model package
```dart
import 'package:aportaya_cliente_organizador/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**codigo** | **String** |  | 
**descripcion** | **String** |  | 
**disparador** | **String** |  | 
**expresionDisparo** | **String** |  | 
**condicion** | **String** | obligatoria. Una regla sin condicion se dispara siempre | 
**accion** | **String** |  | 
**requiereConfirmacionHumana** | **bool** | obligatorio en true para APLICAR_MORA, EJECUTAR_ENTREGA, ESCALAR_COBRANZA y LIQUIDAR_PERIODO. La regla se rechaza si viene sin el, en vez de forzarlo callado: forzarlo deja a quien la escribio creyendo que definio otra cosa | [optional] [default to false]
**prioridad** | **int** | unica entre las activas del mismo disparador | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


