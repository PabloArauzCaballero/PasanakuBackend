# aportaya_cliente_entregas.model.EntradaRegistroCuenta

## Load the model package
```dart
import 'package:aportaya_cliente_entregas/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**tipoCuenta** | **String** |  | 
**entidadFinanciera** | **String** |  | 
**numeroCuenta** | **String** | El numero en claro, y **nunca se persiste asi** (CU-18, R-SEG-01). Entra solo para derivar el enmascarado que se muestra y el hash con el que se detecta la cuenta repetida; las dos derivaciones ocurren ANTES de tocar la base, asi que el numero no llega a ninguna consulta. | 
**numeroCifrado** | **String** | ya cifrado por el almacen de llaves. **El numero en claro nunca viaja en este campo ni se persiste** (R-SEG-01) | 
**titularNombre** | **String** |  | 
**titularDocumento** | **String** |  | 
**moneda** | **String** |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


