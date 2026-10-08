# aportaya_cliente_nucleofinanciero.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**acreditarRecarga**](DefaultApi.md#acreditarrecarga) | **POST** /billetera/recargas/{ordenId}/acreditacion | Acredita una orden que el proveedor confirmo
[**bloquearPorAutoridad**](DefaultApi.md#bloquearporautoridad) | **POST** /billetera/bloqueos | Registrar un bloqueo por orden de autoridad
[**cerrarRetencion**](DefaultApi.md#cerrarretencion) | **POST** /billetera/retenciones/{retencionId}/cierre | Libera o ejecuta una retencion
[**consultarSaldo**](DefaultApi.md#consultarsaldo) | **GET** /billetera/{cuentaId}/saldo | Saldo de una cuenta de billetera
[**emitirExtracto**](DefaultApi.md#emitirextracto) | **GET** /billetera/{cuentaId}/extracto | Emitir el extracto de un periodo
[**retenerSaldo**](DefaultApi.md#retenersaldo) | **POST** /billetera/retenciones | Aparta saldo sin sacarlo de la cuenta
[**reversarTransaccion**](DefaultApi.md#reversartransaccion) | **POST** /billetera/reversos | Reversar una transaccion aplicada
[**solicitarCierreBilletera**](DefaultApi.md#solicitarcierrebilletera) | **POST** /billetera/cierres | Solicitar el cierre de la billetera
[**solicitarRecarga**](DefaultApi.md#solicitarrecarga) | **POST** /billetera/recargas | Abre una orden de recarga y devuelve cuanto se va a acreditar
[**solicitarRetiro**](DefaultApi.md#solicitarretiro) | **POST** /billetera/retiros | Solicitar un retiro hacia una cuenta de destino
[**transferirSaldo**](DefaultApi.md#transferirsaldo) | **POST** /billetera/transferencias | Transferir saldo entre billeteras


# **acreditarRecarga**
> SalidaAcreditacion acreditarRecarga(ordenId, idempotencyKey)

Acredita una orden que el proveedor confirmo

[CONTRATO] La confirmacion del proveedor. Es idempotente por la orden: si dos confirmaciones llegan juntas, una acredita y la otra encuentra la orden ya acreditada — la red duplica, y eso no puede duplicar el saldo.

### Example
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';

final api = AportayaClienteNucleofinanciero().getDefaultApi();
final String ordenId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).

try {
    final response = api.acreditarRecarga(ordenId, idempotencyKey);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->acreditarRecarga: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **ordenId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 

### Return type

[**SalidaAcreditacion**](SalidaAcreditacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **bloquearPorAutoridad**
> SalidaBloqueo bloquearPorAutoridad(idempotencyKey, entradaBloqueo)

Registrar un bloqueo por orden de autoridad

El unico caso en que una retencion no tiene fecha de fin: la levanta la misma autoridad que la puso, y ponerle vencimiento propio seria decidir por un juez cuando se acaba su orden. El documento de respaldo es obligatorio — inmovilizar la plata de alguien sin poder mostrar el papel es indefendible.

### Example
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';

final api = AportayaClienteNucleofinanciero().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaBloqueo entradaBloqueo = ; // EntradaBloqueo | 

try {
    final response = api.bloquearPorAutoridad(idempotencyKey, entradaBloqueo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->bloquearPorAutoridad: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaBloqueo** | [**EntradaBloqueo**](EntradaBloqueo.md)|  | 

### Return type

[**SalidaBloqueo**](SalidaBloqueo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **cerrarRetencion**
> SalidaCierreRetencion cerrarRetencion(retencionId, idempotencyKey, entradaCierreRetencion)

Libera o ejecuta una retencion

[CONTRATO] Una retencion se cierra una sola vez. Si otra operacion la cerro primero, la segunda no la vuelve a cerrar: se responde `RETENCION_YA_EJECUTADA`, que no es una falla sino concurrencia sana.

### Example
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';

final api = AportayaClienteNucleofinanciero().getDefaultApi();
final String retencionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaCierreRetencion entradaCierreRetencion = ; // EntradaCierreRetencion | 

try {
    final response = api.cerrarRetencion(retencionId, idempotencyKey, entradaCierreRetencion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->cerrarRetencion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **retencionId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaCierreRetencion** | [**EntradaCierreRetencion**](EntradaCierreRetencion.md)|  | 

### Return type

[**SalidaCierreRetencion**](SalidaCierreRetencion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **consultarSaldo**
> SaldoBilletera consultarSaldo(cuentaId)

Saldo de una cuenta de billetera

El saldo NO se guarda: se deriva del libro. Esta operacion existe en el borrador porque los trece servicios necesitan saber que la forma del saldo es Dinero, no un number.

### Example
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';

final api = AportayaClienteNucleofinanciero().getDefaultApi();
final String cuentaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.consultarSaldo(cuentaId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->consultarSaldo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **cuentaId** | **String**|  | 

### Return type

[**SaldoBilletera**](SaldoBilletera.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **emitirExtracto**
> SalidaExtracto emitirExtracto(cuentaId, desde, hasta)

Emitir el extracto de un periodo

Si el extracto no cuadra con el cierre diario, NO se emite: uno con una cifra que no coincide es peor que no tenerlo — la persona lo usa para un tramite, se lo rechazan, y descubre el error cuando ya le costo algo. El descuadre queda registrado igual, que es lo que pide el criterio.

### Example
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';

final api = AportayaClienteNucleofinanciero().getDefaultApi();
final String cuentaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final DateTime desde = 2013-10-20; // DateTime | 
final DateTime hasta = 2013-10-20; // DateTime | 

try {
    final response = api.emitirExtracto(cuentaId, desde, hasta);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->emitirExtracto: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **cuentaId** | **String**|  | 
 **desde** | **DateTime**|  | 
 **hasta** | **DateTime**|  | 

### Return type

[**SalidaExtracto**](SalidaExtracto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **retenerSaldo**
> SalidaRetencion retenerSaldo(idempotencyKey, entradaRetencion)

Aparta saldo sin sacarlo de la cuenta

[CONTRATO] Retener no es debitar: el dinero sigue siendo del titular y por eso se ve como retenido y no desaparece. Lo que cambia es que deja de estar disponible mientras dure el motivo.

### Example
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';

final api = AportayaClienteNucleofinanciero().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaRetencion entradaRetencion = ; // EntradaRetencion | 

try {
    final response = api.retenerSaldo(idempotencyKey, entradaRetencion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->retenerSaldo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaRetencion** | [**EntradaRetencion**](EntradaRetencion.md)|  | 

### Return type

[**SalidaRetencion**](SalidaRetencion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **reversarTransaccion**
> SalidaReverso reversarTransaccion(idempotencyKey, entradaReverso)

Reversar una transaccion aplicada

Reversar es escribir el espejo, no borrar el original. El libro es append-only y la transaccion original conserva sus movimientos sin un solo cambio: el extracto tiene que poder contar la historia completa —el cobro y su devolucion—, no un cobro que se desvanecio. Cuatro ojos: quien autoriza no puede ser quien solicita.

### Example
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';

final api = AportayaClienteNucleofinanciero().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaReverso entradaReverso = ; // EntradaReverso | 

try {
    final response = api.reversarTransaccion(idempotencyKey, entradaReverso);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->reversarTransaccion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaReverso** | [**EntradaReverso**](EntradaReverso.md)|  | 

### Return type

[**SalidaReverso**](SalidaReverso.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **solicitarCierreBilletera**
> SalidaCierreBilletera solicitarCierreBilletera(idempotencyKey, entradaCierre)

Solicitar el cierre de la billetera

Cerrar NO borra: la cuenta queda CERRADA y sus movimientos siguen disponibles todo el plazo de conservacion. Cuatro puertas la frenan, y las cuatro protegen a terceros antes que a la empresa — un cierre que deje un aporte sin pagar traslada la perdida a los otros del pasanaku.

### Example
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';

final api = AportayaClienteNucleofinanciero().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaCierre entradaCierre = ; // EntradaCierre | 

try {
    final response = api.solicitarCierreBilletera(idempotencyKey, entradaCierre);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->solicitarCierreBilletera: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaCierre** | [**EntradaCierre**](EntradaCierre.md)|  | 

### Return type

[**SalidaCierreBilletera**](SalidaCierreBilletera.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **solicitarRecarga**
> SalidaRecarga solicitarRecarga(idempotencyKey, entradaRecarga)

Abre una orden de recarga y devuelve cuanto se va a acreditar

[CONTRATO] Derivada de la implementacion del carril 2A, que llego antes que su contrato. La orden NO acredita: reserva la intencion y vence. Acreditar es el paso siguiente y ocurre cuando el proveedor confirma, porque acreditar contra una promesa es acreditar plata que puede no llegar. `acreditara` viaja en la respuesta ya calculado: el cliente **no** resta el costo del proveedor por su cuenta. Un cliente que recalcula produce un numero que no es el del libro contable.

### Example
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';

final api = AportayaClienteNucleofinanciero().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaRecarga entradaRecarga = ; // EntradaRecarga | 

try {
    final response = api.solicitarRecarga(idempotencyKey, entradaRecarga);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->solicitarRecarga: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaRecarga** | [**EntradaRecarga**](EntradaRecarga.md)|  | 

### Return type

[**SalidaRecarga**](SalidaRecarga.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **solicitarRetiro**
> SalidaRetiro solicitarRetiro(idempotencyKey, entradaRetiro)

Solicitar un retiro hacia una cuenta de destino

Retencion primero, pago despues, nunca al reves. Si se instruyera el pago antes de retener, entre las dos cosas la persona podria gastar el mismo saldo en otra operacion y el retiro saldria contra un disponible que ya no existe. El proveedor se instruye FUERA de la transaccion (invariante 6).

### Example
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';

final api = AportayaClienteNucleofinanciero().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaRetiro entradaRetiro = ; // EntradaRetiro | 

try {
    final response = api.solicitarRetiro(idempotencyKey, entradaRetiro);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->solicitarRetiro: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaRetiro** | [**EntradaRetiro**](EntradaRetiro.md)|  | 

### Return type

[**SalidaRetiro**](SalidaRetiro.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **transferirSaldo**
> SalidaTransferencia transferirSaldo(idempotencyKey, entradaTransferencia)

Transferir saldo entre billeteras

La unica operacion donde el dinero no entra ni sale del sistema: cambia de bolsillo, y el saldo total permanece constante. Las dos cuentas se bloquean en orden de identificador — dos transferencias cruzadas que bloqueen cada una primero la suya se esperan para siempre.

### Example
```dart
import 'package:aportaya_cliente_nucleofinanciero/api.dart';

final api = AportayaClienteNucleofinanciero().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaTransferencia entradaTransferencia = ; // EntradaTransferencia | 

try {
    final response = api.transferirSaldo(idempotencyKey, entradaTransferencia);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->transferirSaldo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaTransferencia** | [**EntradaTransferencia**](EntradaTransferencia.md)|  | 

### Return type

[**SalidaTransferencia**](SalidaTransferencia.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

