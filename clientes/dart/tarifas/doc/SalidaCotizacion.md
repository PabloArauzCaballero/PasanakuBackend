# aportaya_cliente_tarifas.model.SalidaCotizacion

## Load the model package
```dart
import 'package:aportaya_cliente_tarifas/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**cotizacionId** | **String** |  | 
**montoComision** | [**Dinero**](Dinero.md) |  | 
**montoImpuesto** | [**Dinero**](Dinero.md) |  | 
**montoTotal** | [**Dinero**](Dinero.md) |  | 
**desglose** | [**List&lt;LineaDesglose&gt;**](LineaDesglose.md) |  | 
**validaHasta** | [**DateTime**](DateTime.md) |  | 
**esNueva** | **bool** | false cuando la respuesta viene de un reintento con la misma clave | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


