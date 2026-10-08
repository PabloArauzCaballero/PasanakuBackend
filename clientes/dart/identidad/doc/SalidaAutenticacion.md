# aportaya_cliente_identidad.model.SalidaAutenticacion

## Load the model package
```dart
import 'package:aportaya_cliente_identidad/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**sesionId** | **String** |  | [optional] 
**expiraEn** | [**DateTime**](DateTime.md) |  | [optional] 
**requiereFactorAdicional** | **bool** |  | 
**dispositivoConfiable** | **bool** |  | 
**tokenAcceso** | **String** | El JWT firmado con RS256 que los otros trece servicios validan contra el JWKS de identidad (ADR-024). Ausente cuando la respuesta pide un segundo factor: recien se emite con la sesion abierta. | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


