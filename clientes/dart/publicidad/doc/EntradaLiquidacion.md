# aportaya_cliente_publicidad.model.EntradaLiquidacion

## Load the model package
```dart
import 'package:aportaya_cliente_publicidad/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**periodo** | **String** |  | 
**facturaElectronicaId** | **String** | El comprobante fiscal, ya emitido por modulo 11. Sin el, la factura nace GENERADA y no puede avanzar: la tabla es append-only. | [optional] 
**cuentaPorCobrarId** | **String** | La cuenta por cobrar, ya abierta por modulo 13 (CU-104). | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


