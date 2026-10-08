# aportaya_cliente_aportes.model.EntradaProveedor

## Load the model package
```dart
import 'package:aportaya_cliente_aportes/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**codigo** | **String** |  | 
**nombre** | **String** |  | 
**tipo** | **String** |  | 
**urlBase** | **String** |  | 
**referenciaCredenciales** | **String** | REFERENCIA al almacen de secretos (vault://...). Enviar la credencial en claro se rechaza con AP-CU99-05 antes de tocar la base. | 
**comisionFija** | **String** |  | 
**comisionPorcentual** | **String** |  | 
**soportaWebhook** | **bool** |  | [optional] [default to false]
**soportaConsultaEstado** | **bool** |  | [optional] [default to false]
**prioridad** | **int** |  | 
**tieneContratoVigente** | **bool** |  | 
**pruebasCompletas** | **bool** |  | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


