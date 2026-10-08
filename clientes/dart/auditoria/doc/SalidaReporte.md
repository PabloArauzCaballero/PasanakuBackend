# aportaya_cliente_auditoria.model.SalidaReporte

## Load the model package
```dart
import 'package:aportaya_cliente_auditoria/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**ejecucionId** | **String** |  | 
**estado** | **String** |  | 
**filasGeneradas** | **int** | Cero es un resultado valido, no un error. | 
**hashResultado** | **String** | SHA-256 de la forma canonica del resultado. Es lo que permite probar que el archivo que alguien presenta es el que salio de aca. | 
**exportacionId** | **String** | Presente solo si se pidio formato. El archivo se cifra y caduca cuando la definicion tiene datos sensibles; no lo elige quien exporta. | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


