# aportaya_cliente_tarifas.model.EntradaCotizacion

## Load the model package
```dart
import 'package:aportaya_cliente_tarifas/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**codigoTarifario** | **String** |  | 
**hechoGenerador** | **String** |  | 
**referenciaTipo** | **String** |  | 
**referenciaId** | **String** |  | 
**montoBase** | [**Dinero**](Dinero.md) |  | 
**grupoId** | **String** | si viene y el grupo tiene tarifa congelada, manda el snapshot (R-TAR-07) | [optional] 
**descuento** | [**Dinero**](Dinero.md) |  | [optional] 
**segmentoCodigo** | **String** | el segmento ya resuelto por CU-36. Queda en el desglose: cotizacion_comision no tiene columna para el y no se invento una | [optional] 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


