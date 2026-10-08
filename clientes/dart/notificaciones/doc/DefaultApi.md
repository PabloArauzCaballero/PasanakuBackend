# aportaya_cliente_notificaciones.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_notificaciones/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**consultarSupresion**](DefaultApi.md#consultarsupresion) | **GET** /notificaciones/supresion | Si un destino pidio no recibir mas
[**emitirNotificacion**](DefaultApi.md#emitirnotificacion) | **POST** /notificaciones | Emite un aviso por los canales habilitados del destinatario
[**procesarRespuestaEntrante**](DefaultApi.md#procesarrespuestaentrante) | **POST** /notificaciones/webhooks/{proveedor} | Procesar una respuesta entrante


# **consultarSupresion**
> Supresion consultarSupresion(identificador, categoria)

Si un destino pidio no recibir mas

La pregunta CU-69 de `grupos` antes de invitar: a quien se dio de baja no se le escribe, aunque quien invita no lo sepa. La lista de supresion es de este servicio y nadie mas la puede leer (invariante 11). Se mira en los tres canales encendidos y alcanza con que uno este suprimido: quien pidio no recibir por el canal que usariamos, pidio no recibir.

### Example
```dart
import 'package:aportaya_cliente_notificaciones/api.dart';

final api = AportayaClienteNotificaciones().getDefaultApi();
final String identificador = identificador_example; // String | 
final String categoria = categoria_example; // String | 

try {
    final response = api.consultarSupresion(identificador, categoria);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->consultarSupresion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **identificador** | **String**|  | 
 **categoria** | **String**|  | 

### Return type

[**Supresion**](Supresion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **emitirNotificacion**
> SalidaNotificacion emitirNotificacion(idempotencyKey, entradaNotificacion)

Emite un aviso por los canales habilitados del destinatario

Los canales por defecto son IN_APP, PUSH y CORREO (ADR-035). WhatsApp, SMS y voz estan APAGADOS: no se usan en un flujo nuevo. El servicio decide el canal segun consentimiento y supresion; el emisor no lo elige.

### Example
```dart
import 'package:aportaya_cliente_notificaciones/api.dart';

final api = AportayaClienteNotificaciones().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaNotificacion entradaNotificacion = ; // EntradaNotificacion | 

try {
    final response = api.emitirNotificacion(idempotencyKey, entradaNotificacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->emitirNotificacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaNotificacion** | [**EntradaNotificacion**](EntradaNotificacion.md)|  | 

### Return type

[**SalidaNotificacion**](SalidaNotificacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **procesarRespuestaEntrante**
> SalidaRespuesta procesarRespuestaEntrante(proveedor, entradaRespuesta)

Procesar una respuesta entrante

La unica puerta que abre alguien de afuera. Verifica la firma ANTES de tocar la base, y no revela nada: un remitente desconocido recibe la misma respuesta que un mensaje duplicado. Contestar «ese numero no existe» convertiria este webhook en un directorio de clientes.

### Example
```dart
import 'package:aportaya_cliente_notificaciones/api.dart';

final api = AportayaClienteNotificaciones().getDefaultApi();
final String proveedor = proveedor_example; // String | 
final EntradaRespuesta entradaRespuesta = ; // EntradaRespuesta | 

try {
    final response = api.procesarRespuestaEntrante(proveedor, entradaRespuesta);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->procesarRespuestaEntrante: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **proveedor** | **String**|  | 
 **entradaRespuesta** | [**EntradaRespuesta**](EntradaRespuesta.md)|  | 

### Return type

[**SalidaRespuesta**](SalidaRespuesta.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

