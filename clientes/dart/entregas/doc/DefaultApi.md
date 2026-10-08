# aportaya_cliente_entregas.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_entregas/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**anotarRespuestaDesembolso**](DefaultApi.md#anotarrespuestadesembolso) | **POST** /desembolsos/{ordenId}/respuesta | Registrar lo que contesto el proveedor
[**autorizarEntrega**](DefaultApi.md#autorizarentrega) | **POST** /entregas/{entregaId}/autorizacion | Autorizar la entrega
[**consultarDisponibilidad**](DefaultApi.md#consultardisponibilidad) | **GET** /cuentas-bancarias/{cuentaId}/disponibilidad | Si la cuenta puede recibir plata ahora, y si no, cuanto falta
[**designarCuentaPrincipal**](DefaultApi.md#designarcuentaprincipal) | **POST** /cuentas-bancarias/{cuentaId}/principal | Designar la cuenta principal
[**ejecutarEntrega**](DefaultApi.md#ejecutarentrega) | **POST** /entregas/{entregaId}/ejecucion | Ejecutar la entrega
[**emitirOrdenDesembolso**](DefaultApi.md#emitirordendesembolso) | **POST** /desembolsos | Emitir la orden de desembolso
[**liquidarEntrega**](DefaultApi.md#liquidarentrega) | **POST** /entregas | Liquidar la entrega del turno
[**registrarCuentaDestino**](DefaultApi.md#registrarcuentadestino) | **POST** /cuentas-bancarias | Registrar una cuenta bancaria de destino
[**verificarCuentaDestino**](DefaultApi.md#verificarcuentadestino) | **POST** /cuentas-bancarias/{cuentaId}/verificacion | Verificar la titularidad de la cuenta


# **anotarRespuestaDesembolso**
> SalidaIntentoDesembolso anotarRespuestaDesembolso(ordenId, entradaRespuestaDesembolso)

Registrar lo que contesto el proveedor

La llamada al proveedor la hace quien orquesta, FUERA de esta transaccion (invariante 6). Un error definitivo NO se reintenta: insistir contra una cuenta que no existe solo demora el momento en que alguien mira el caso, mientras la plata del beneficiario sigue retenida.

### Example
```dart
import 'package:aportaya_cliente_entregas/api.dart';

final api = AportayaClienteEntregas().getDefaultApi();
final String ordenId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaRespuestaDesembolso entradaRespuestaDesembolso = ; // EntradaRespuestaDesembolso | 

try {
    final response = api.anotarRespuestaDesembolso(ordenId, entradaRespuestaDesembolso);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->anotarRespuestaDesembolso: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **ordenId** | **String**|  | 
 **entradaRespuestaDesembolso** | [**EntradaRespuestaDesembolso**](EntradaRespuestaDesembolso.md)|  | 

### Return type

[**SalidaIntentoDesembolso**](SalidaIntentoDesembolso.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **autorizarEntrega**
> SalidaAutorizacion autorizarEntrega(entregaId)

Autorizar la entrega

Una validacion bloqueante sin resolver la frena. Saltearlas para «no demorar la entrega» es como se entrega mal.

### Example
```dart
import 'package:aportaya_cliente_entregas/api.dart';

final api = AportayaClienteEntregas().getDefaultApi();
final String entregaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.autorizarEntrega(entregaId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->autorizarEntrega: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entregaId** | **String**|  | 

### Return type

[**SalidaAutorizacion**](SalidaAutorizacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **consultarDisponibilidad**
> Disponibilidad consultarDisponibilidad(cuentaId)

Si la cuenta puede recibir plata ahora, y si no, cuanto falta

Se dice CUANTO falta, no solo que no se puede: negar sin explicar convierte una medida de seguridad en una falla del sistema.

### Example
```dart
import 'package:aportaya_cliente_entregas/api.dart';

final api = AportayaClienteEntregas().getDefaultApi();
final String cuentaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.consultarDisponibilidad(cuentaId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->consultarDisponibilidad: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **cuentaId** | **String**|  | 

### Return type

[**Disponibilidad**](Disponibilidad.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **designarCuentaPrincipal**
> DesignarCuentaPrincipal200Response designarCuentaPrincipal(cuentaId)

Designar la cuenta principal

Una sola principal por usuario (R-BIL-17). Dos harian que el destino de un retiro dependiera del orden en que la base devuelve las filas.

### Example
```dart
import 'package:aportaya_cliente_entregas/api.dart';

final api = AportayaClienteEntregas().getDefaultApi();
final String cuentaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.designarCuentaPrincipal(cuentaId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->designarCuentaPrincipal: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **cuentaId** | **String**|  | 

### Return type

[**DesignarCuentaPrincipal200Response**](DesignarCuentaPrincipal200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ejecutarEntrega**
> SalidaEjecucionEntrega ejecutarEntrega(entregaId, ejecutarEntregaRequest)

Ejecutar la entrega

Quien autoriza NO ejecuta (R-SEG-04). Una sola persona que hace las dos cosas puede sacar el fondo entero de un grupo sin que nadie mas lo vea pasar.

### Example
```dart
import 'package:aportaya_cliente_entregas/api.dart';

final api = AportayaClienteEntregas().getDefaultApi();
final String entregaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EjecutarEntregaRequest ejecutarEntregaRequest = ; // EjecutarEntregaRequest | 

try {
    final response = api.ejecutarEntrega(entregaId, ejecutarEntregaRequest);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->ejecutarEntrega: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entregaId** | **String**|  | 
 **ejecutarEntregaRequest** | [**EjecutarEntregaRequest**](EjecutarEntregaRequest.md)|  | 

### Return type

[**SalidaEjecucionEntrega**](SalidaEjecucionEntrega.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **emitirOrdenDesembolso**
> SalidaOrdenDesembolso emitirOrdenDesembolso(idempotencyKey, entradaOrdenDesembolso)

Emitir la orden de desembolso

Una orden VIVA por entrega (R-DES-01) y ninguna a cuenta sin verificar (R-DES-02). Dos ordenes de la misma entrega es pagar dos veces, y no se descubre hasta que alguien concilia el extracto.

### Example
```dart
import 'package:aportaya_cliente_entregas/api.dart';

final api = AportayaClienteEntregas().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaOrdenDesembolso entradaOrdenDesembolso = ; // EntradaOrdenDesembolso | 

try {
    final response = api.emitirOrdenDesembolso(idempotencyKey, entradaOrdenDesembolso);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->emitirOrdenDesembolso: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaOrdenDesembolso** | [**EntradaOrdenDesembolso**](EntradaOrdenDesembolso.md)|  | 

### Return type

[**SalidaOrdenDesembolso**](SalidaOrdenDesembolso.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **liquidarEntrega**
> SalidaLiquidacion liquidarEntrega(idempotencyKey, entradaLiquidacion)

Liquidar la entrega del turno

Una entrega por turno (R-GRP-01) y el neto NUNCA negativo: que las deducciones superen la bolsa significa que el beneficiario terminaria debiendo por cobrar su turno. Cada deduccion viaja con su tipo y su origen.

### Example
```dart
import 'package:aportaya_cliente_entregas/api.dart';

final api = AportayaClienteEntregas().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaLiquidacion entradaLiquidacion = ; // EntradaLiquidacion | 

try {
    final response = api.liquidarEntrega(idempotencyKey, entradaLiquidacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->liquidarEntrega: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaLiquidacion** | [**EntradaLiquidacion**](EntradaLiquidacion.md)|  | 

### Return type

[**SalidaLiquidacion**](SalidaLiquidacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **registrarCuentaDestino**
> SalidaRegistroCuenta registrarCuentaDestino(entradaRegistroCuenta)

Registrar una cuenta bancaria de destino

El numero NUNCA se guarda en claro (R-SEG-01): la fila lleva el cifrado, un hash con pimienta para buscar, y una version enmascarada. Y la cuenta esta a nombre de quien la registra (R-SEG-02): una cuenta de destino ajena es la forma mas simple de sacar plata de un grupo hacia afuera.

### Example
```dart
import 'package:aportaya_cliente_entregas/api.dart';

final api = AportayaClienteEntregas().getDefaultApi();
final EntradaRegistroCuenta entradaRegistroCuenta = ; // EntradaRegistroCuenta | 

try {
    final response = api.registrarCuentaDestino(entradaRegistroCuenta);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->registrarCuentaDestino: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entradaRegistroCuenta** | [**EntradaRegistroCuenta**](EntradaRegistroCuenta.md)|  | 

### Return type

[**SalidaRegistroCuenta**](SalidaRegistroCuenta.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **verificarCuentaDestino**
> SalidaVerificacionCuenta verificarCuentaDestino(cuentaId, verificarCuentaDestinoRequest)

Verificar la titularidad de la cuenta

Verificada no es utilizable: empieza la ventana de enfriamiento y su fin se GUARDA. Si alguien toma una cuenta ajena, lo primero que hace es cambiar el destino y retirar; la ventana le da al titular real el tiempo de enterarse.

### Example
```dart
import 'package:aportaya_cliente_entregas/api.dart';

final api = AportayaClienteEntregas().getDefaultApi();
final String cuentaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final VerificarCuentaDestinoRequest verificarCuentaDestinoRequest = ; // VerificarCuentaDestinoRequest | 

try {
    final response = api.verificarCuentaDestino(cuentaId, verificarCuentaDestinoRequest);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->verificarCuentaDestino: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **cuentaId** | **String**|  | 
 **verificarCuentaDestinoRequest** | [**VerificarCuentaDestinoRequest**](VerificarCuentaDestinoRequest.md)|  | 

### Return type

[**SalidaVerificacionCuenta**](SalidaVerificacionCuenta.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

