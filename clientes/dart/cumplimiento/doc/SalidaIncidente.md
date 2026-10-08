# aportaya_cliente_cumplimiento.model.SalidaIncidente

## Load the model package
```dart
import 'package:aportaya_cliente_cumplimiento/api.dart';
```

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**incidenteId** | **String** |  |
**codigo** | **String** | Derivado del hecho. Dos avisos del mismo incidente, el del monitoreo y el de la persona que lo vio, producen el mismo codigo y el segundo choca contra el UNIQUE, en vez de abrir dos expedientes con dos relojes para el mismo hecho. |
**plazoReporte** | [**DateTime**](DateTime.md) | Guardado, no recalculado. Es el plazo que regia el dia del incidente. |
**requiereNotificarTitulares** | **bool** |  |
**plazoNotificacion** | [**DateTime**](DateTime.md) | Ausente cuando no hay datos personales afectados. | [optional]
**contratoTerceroId** | **String** | El contrato del proveedor, resuelto por el activo afectado. Presente cuando el incidente ocurrio en un tercero: la responsabilidad frente al cliente no se terceriza, pero el contrato hay que poder mirarlo. | [optional]
**registradoEn** | [**DateTime**](DateTime.md) |  |

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


