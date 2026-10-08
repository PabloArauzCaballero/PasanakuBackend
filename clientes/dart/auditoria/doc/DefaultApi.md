# aportaya_cliente_auditoria.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_auditoria/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**descargarExportacion**](DefaultApi.md#descargarexportacion) | **POST** /reportes/exportaciones/{exportacionId}/descargas | Cuenta una descarga del archivo y dice si todavia se puede entregar
[**ejecutarReporte**](DefaultApi.md#ejecutarreporte) | **POST** /reportes/{definicionId}/ejecuciones | Ejecuta un reporte definido, deja huella y opcionalmente lo exporta
[**ejercerDerechos**](DefaultApi.md#ejercerderechos) | **POST** /auditoria/derechos | Atiende un derecho del titular sobre sus datos personales
[**publicarTablero**](DefaultApi.md#publicartablero) | **GET** /indicadores | Los indicadores del periodo, con su meta, su variacion y su serie


# **descargarExportacion**
> descargarExportacion(exportacionId, idempotencyKey)

Cuenta una descarga del archivo y dice si todavia se puede entregar

No devuelve el archivo: lo autoriza. La descarga se cuenta ANTES de entregar porque contar despues significa no contar la vez que fallo a mitad, y el tope deja de ser un tope. Un enlace que se descarga indefinidamente deja de ser una entrega y pasa a ser una copia publicada. Agotado el tope, hay que volver a pedir el reporte, con justificacion nueva — que es exactamente el rastro que una auditoria busca.

### Example
```dart
import 'package:aportaya_cliente_auditoria/api.dart';

final api = AportayaClienteAuditoria().getDefaultApi();
final String exportacionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).

try {
    api.descargarExportacion(exportacionId, idempotencyKey);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->descargarExportacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **exportacionId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 

### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ejecutarReporte**
> SalidaReporte ejecutarReporte(definicionId, idempotencyKey, entradaReporte)

Ejecuta un reporte definido, deja huella y opcionalmente lo exporta

Que sacar informacion del sistema sea una operacion con permiso, huella y vencimiento, y no una consulta suelta que alguien corre contra produccion y manda por correo. Los parametros van por LISTA BLANCA contra `parametros_esperados`: lo que no esta declarado no entra, y el valor viaja como ligadura — nunca concatenado en la consulta. La alternativa, aceptar lo que venga y filtrar lo peligroso, es como se escriben las inyecciones: siempre falta un caso. El `hashResultado` es la huella canonica del resultado. Dos ejecuciones con los mismos parametros y los mismos datos dan el mismo hash, y eso permite probar que un reporte entregado no fue alterado despues. Un reporte sin filas se entrega igual, con cero: «no hubo» es una respuesta, y para los regulatorios es la obligatoria (`R-UIF-06`). El estado terminal exitoso es COMPLETADA, que es lo que admite `ck_ejecucion_reporte_estado`. Donde el texto del caso de uso dice LISTA y la tabla dice COMPLETADA, manda la tabla: es la que rechaza.

### Example
```dart
import 'package:aportaya_cliente_auditoria/api.dart';

final api = AportayaClienteAuditoria().getDefaultApi();
final String definicionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaReporte entradaReporte = ; // EntradaReporte | 

try {
    final response = api.ejecutarReporte(definicionId, idempotencyKey, entradaReporte);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->ejecutarReporte: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **definicionId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaReporte** | [**EntradaReporte**](EntradaReporte.md)|  | 

### Return type

[**SalidaReporte**](SalidaReporte.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ejercerDerechos**
> SalidaDerechos ejercerDerechos(idempotencyKey, entradaDerechos)

Atiende un derecho del titular sobre sus datos personales

Acceso, rectificacion, oposicion, portabilidad y cancelacion. El plazo legal se calcula al abrir y SE GUARDA: recalcularlo al consultar lo haria moverse solo, y basta que alguien declare un feriado despues para que el vencimiento cambie sin que nadie lo decida. La cancelacion NO elige entre el derecho y la obligacion de conservar: reparte. Borra lo que ya vencio, seudonimiza lo que la ley obliga a guardar, y devuelve la lista de lo que quedo con su base legal. Responder «no se puede borrar nada» seria mentirle al titular; borrar todo seria un incumplimiento. Por eso el estado puede ser PARCIAL: decir ATENDIDA sobre una solicitud que no se cumplio entera es la forma mas rapida de que el reclamo vuelva con la razon del otro lado.

### Example
```dart
import 'package:aportaya_cliente_auditoria/api.dart';

final api = AportayaClienteAuditoria().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaDerechos entradaDerechos = ; // EntradaDerechos | 

try {
    final response = api.ejercerDerechos(idempotencyKey, entradaDerechos);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->ejercerDerechos: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaDerechos** | [**EntradaDerechos**](EntradaDerechos.md)|  | 

### Return type

[**SalidaDerechos**](SalidaDerechos.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicarTablero**
> SalidaTablero publicarTablero(periodo, dimension, dimensionId, periodosDeSerie)

Los indicadores del periodo, con su meta, su variacion y su serie

[CONTRATO] Que la direccion mire siempre los mismos numeros, calculados de la misma forma: un indicador es una DEFINICION, no una consulta, y si dos lugares lo recalculan ya hay dos indicadores. Se lee de `auditoria.indicador_kpi`, que es donde el calculo los deja. Esta operacion NO calcula: publica. La `dimension` es la del modelo —GLOBAL, POR_GRUPO, POR_ORGANIZADOR— y no la que enumera el texto de CU-98. Donde el caso de uso y la tabla no coinciden, manda la tabla: es la que rechaza.

### Example
```dart
import 'package:aportaya_cliente_auditoria/api.dart';

final api = AportayaClienteAuditoria().getDefaultApi();
final String periodo = periodo_example; // String | Formato AAAA, AAAA-MM o AAAA-MM-DD, segun la granularidad
final String dimension = dimension_example; // String | 
final String dimensionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Obligatorio salvo en GLOBAL, donde no corresponde
final int periodosDeSerie = 56; // int | Cuantos periodos anteriores traer para el grafico. Cero trae solo el actual.

try {
    final response = api.publicarTablero(periodo, dimension, dimensionId, periodosDeSerie);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->publicarTablero: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **periodo** | **String**| Formato AAAA, AAAA-MM o AAAA-MM-DD, segun la granularidad | 
 **dimension** | **String**|  | 
 **dimensionId** | **String**| Obligatorio salvo en GLOBAL, donde no corresponde | [optional] 
 **periodosDeSerie** | **int**| Cuantos periodos anteriores traer para el grafico. Cero trae solo el actual. | [optional] [default to 6]

### Return type

[**SalidaTablero**](SalidaTablero.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

