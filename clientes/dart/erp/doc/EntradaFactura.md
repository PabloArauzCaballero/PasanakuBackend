# aportaya_cliente_erp.model.EntradaFactura

## Load the model package
```dart
import 'package:aportaya_cliente_erp/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**terceroComercialId** | **String** |  | 
**ordenCompraId** | **String** |  | [optional] 
**centroCostoId** | **String** |  | [optional] 
**numeroFactura** | **String** |  | 
**fechaEmision** | [**DateTime**](DateTime.md) |  | 
**fechaVencimiento** | [**DateTime**](DateTime.md) |  | 
**monto** | **String** | Importe como CADENA decimal. Un number JSON es un doble. | 
**moneda** | **String** |  | 
**cuentaGastoId** | **String** |  | [optional] 
**aprobadaPor** | **String** | Quien aprueba, en el alta. La tabla es append-only: no hay aprobacion posterior. | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


