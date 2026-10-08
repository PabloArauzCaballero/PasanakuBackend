# aportaya_cliente_identidad.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_identidad/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**autenticar**](DefaultApi.md#autenticar) | **POST** /sesiones | Autenticar con MFA y registrar dispositivo
[**buscarPorTelefono**](DefaultApi.md#buscarportelefono) | **GET** /usuarios/por-telefono | Si ese telefono ya tiene cuenta
[**emitirTokenDeInvitacion**](DefaultApi.md#emitirtokendeinvitacion) | **POST** /usuarios/tokens/invitacion | El enlace de un solo uso con el que se invita a alguien a un grupo
[**listarVerificaciones**](DefaultApi.md#listarverificaciones) | **GET** /identidad/verificaciones | La cola de expedientes esperando una decision humana
[**registrarUsuario**](DefaultApi.md#registrarusuario) | **POST** /usuarios | Registro y apertura de billetera
[**resolverVerificacion**](DefaultApi.md#resolververificacion) | **POST** /identidad/verificaciones/{verificacionId}/decision | Aprobar o rechazar la verificacion, a mano
[**subirDocumento**](DefaultApi.md#subirdocumento) | **POST** /usuarios/{usuarioId}/documentos | Sube una foto del documento o la prueba de vida
[**verContenidoDeFoto**](DefaultApi.md#vercontenidodefoto) | **GET** /identidad/verificaciones/{verificacionId}/fotos/{cara}/contenido | Los bytes de una foto del expediente, servidos por el servicio dueno
[**verFotoDelExpediente**](DefaultApi.md#verfotodelexpediente) | **GET** /identidad/verificaciones/{verificacionId}/fotos/{cara} | Un enlace de vigencia corta para mirar una foto del expediente
[**verificarTitularidad**](DefaultApi.md#verificartitularidad) | **POST** /usuarios/{usuarioId}/titularidad/verificacion | Si el nombre y el documento declarados son los del titular


# **autenticar**
> SalidaAutenticacion autenticar(entradaAutenticacion)

Autenticar con MFA y registrar dispositivo

Ruta publica: es el momento en que todavia no hay sesion. NO lleva Idempotency-Key a proposito — cada intento es un hecho distinto y queda registrado en intento_autenticacion, incluidos los fallidos.

### Example
```dart
import 'package:aportaya_cliente_identidad/api.dart';

final api = AportayaClienteIdentidad().getDefaultApi();
final EntradaAutenticacion entradaAutenticacion = ; // EntradaAutenticacion | 

try {
    final response = api.autenticar(entradaAutenticacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->autenticar: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entradaAutenticacion** | [**EntradaAutenticacion**](EntradaAutenticacion.md)|  | 

### Return type

[**SalidaAutenticacion**](SalidaAutenticacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **buscarPorTelefono**
> UsuarioEncontrado buscarPorTelefono(telefono)

Si ese telefono ya tiene cuenta

La pregunta `grupos` antes de invitar (CU-69): invitar a quien ya es participante es ruido, y para saberlo hay que poder ir del telefono al usuario. Devuelve el identificador y nada mas —ni nombre, ni estado, ni cuando entro—. Exige permiso: una ruta que dice si un telefono tiene cuenta es, sin permiso, una forma de enumerar clientes.

### Example
```dart
import 'package:aportaya_cliente_identidad/api.dart';

final api = AportayaClienteIdentidad().getDefaultApi();
final String telefono = telefono_example; // String | 

try {
    final response = api.buscarPorTelefono(telefono);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->buscarPorTelefono: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **telefono** | **String**|  | 

### Return type

[**UsuarioEncontrado**](UsuarioEncontrado.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **emitirTokenDeInvitacion**
> SalidaTokenDeInvitacion emitirTokenDeInvitacion(idempotencyKey, entradaTokenDeInvitacion)

El enlace de un solo uso con el que se invita a alguien a un grupo

`token_verificacion` es de identidad y su politica —vigencia, canales, cuantos reenvios— esta sembrada bajo el proposito INVITACION_GRUPO. `grupos` no puede escribir esa tabla (invariante 11): pide el token y guarda su identificador. Se guarda el hash, nunca el token: quien tenga la base no puede reconstruir los enlaces vivos. El valor en claro se devuelve una sola vez, a quien lo va a enviar.

### Example
```dart
import 'package:aportaya_cliente_identidad/api.dart';

final api = AportayaClienteIdentidad().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaTokenDeInvitacion entradaTokenDeInvitacion = ; // EntradaTokenDeInvitacion | 

try {
    final response = api.emitirTokenDeInvitacion(idempotencyKey, entradaTokenDeInvitacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->emitirTokenDeInvitacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaTokenDeInvitacion** | [**EntradaTokenDeInvitacion**](EntradaTokenDeInvitacion.md)|  | 

### Return type

[**SalidaTokenDeInvitacion**](SalidaTokenDeInvitacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listarVerificaciones**
> List<ExpedienteEnRevision> listarVerificaciones(estado)

La cola de expedientes esperando una decision humana

El portal de riesgo. Devuelve quien espera, desde cuando y que fotos tiene cargadas — nunca las fotos en si, que se piden una por una y quedan en la bitacora como cualquier lectura de dato personal.

### Example
```dart
import 'package:aportaya_cliente_identidad/api.dart';

final api = AportayaClienteIdentidad().getDefaultApi();
final String estado = estado_example; // String | 

try {
    final response = api.listarVerificaciones(estado);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->listarVerificaciones: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **estado** | **String**|  | [optional] 

### Return type

[**List&lt;ExpedienteEnRevision&gt;**](ExpedienteEnRevision.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **registrarUsuario**
> SalidaRegistro registrarUsuario(idempotencyKey, entradaRegistro)

Registro y apertura de billetera

Una transaccion: usuario, KYC, debida diligencia, calificacion de riesgo, expediente y cuenta de billetera. Ruta publica: es el unico momento en que todavia no hay sesion.

### Example
```dart
import 'package:aportaya_cliente_identidad/api.dart';

final api = AportayaClienteIdentidad().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaRegistro entradaRegistro = ; // EntradaRegistro | 

try {
    final response = api.registrarUsuario(idempotencyKey, entradaRegistro);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->registrarUsuario: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaRegistro** | [**EntradaRegistro**](EntradaRegistro.md)|  | 

### Return type

[**SalidaRegistro**](SalidaRegistro.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **resolverVerificacion**
> ExpedienteEnRevision resolverVerificacion(verificacionId, idempotencyKey, decisionDeVerificacion)

Aprobar o rechazar la verificacion, a mano

La decision es de una persona y queda con su nombre: `revisada_por` guarda quien decidio y `motivo_rechazo` por que. Rechazar sin motivo no se admite — a quien le rechazan la cuenta tiene derecho a saber por que.

### Example
```dart
import 'package:aportaya_cliente_identidad/api.dart';

final api = AportayaClienteIdentidad().getDefaultApi();
final String verificacionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final DecisionDeVerificacion decisionDeVerificacion = ; // DecisionDeVerificacion | 

try {
    final response = api.resolverVerificacion(verificacionId, idempotencyKey, decisionDeVerificacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->resolverVerificacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **verificacionId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **decisionDeVerificacion** | [**DecisionDeVerificacion**](DecisionDeVerificacion.md)|  | 

### Return type

[**ExpedienteEnRevision**](ExpedienteEnRevision.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **subirDocumento**
> ArchivoDelExpediente subirDocumento(usuarioId, idempotencyKey, cara, archivo)

Sube una foto del documento o la prueba de vida

La foto NO se guarda en el disco del servicio: va al servidor de archivos y en la fila queda una clave de objeto (ADR-034). El tipo se decide leyendo los primeros bytes, no por la extension ni por el Content-Type que manda el cliente. Devuelve la clave y el SHA-256 para que el expediente quede trazable.

### Example
```dart
import 'package:aportaya_cliente_identidad/api.dart';

final api = AportayaClienteIdentidad().getDefaultApi();
final String usuarioId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String cara = cara_example; // String | Que parte del expediente es esta foto.
final MultipartFile archivo = BINARY_DATA_HERE; // MultipartFile | 

try {
    final response = api.subirDocumento(usuarioId, idempotencyKey, cara, archivo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->subirDocumento: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **usuarioId** | **String**|  | 
 **idempotencyKey** | **String**|  | 
 **cara** | **String**| Que parte del expediente es esta foto. | 
 **archivo** | **MultipartFile**|  | 

### Return type

[**ArchivoDelExpediente**](ArchivoDelExpediente.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **verContenidoDeFoto**
> MultipartFile verContenidoDeFoto(verificacionId, cara)

Los bytes de una foto del expediente, servidos por el servicio dueno

El binario, no un enlace. `verFotoDelExpediente` devuelve una URL prefirmada contra el almacen, y el almacen NO es publico: vive en la red interna, sin puerto publicado ni dominio, que es como lo quiere el ADR-034 —«el binario no se sirve nunca directo»—. Esa URL sirve entre servicios y es inservible en un navegador, que no resuelve `minio:9000`. Aca lo entrega el servicio dueno de la tabla, que valida sesion y permiso antes de leer un solo byte, y la lectura queda en la bitacora como cualquier otra lectura de dato personal. Son fotos de cedula de a una, no megabytes de video: proxearlas cuesta menos que abrir el almacen al mundo.

### Example
```dart
import 'package:aportaya_cliente_identidad/api.dart';

final api = AportayaClienteIdentidad().getDefaultApi();
final String verificacionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String cara = cara_example; // String | 

try {
    final response = api.verContenidoDeFoto(verificacionId, cara);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->verContenidoDeFoto: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **verificacionId** | **String**|  | 
 **cara** | **String**|  | 

### Return type

[**MultipartFile**](MultipartFile.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/octet-stream, application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **verFotoDelExpediente**
> EnlaceDeFoto verFotoDelExpediente(verificacionId, cara)

Un enlace de vigencia corta para mirar una foto del expediente

Devuelve una URL temporal, no el binario: asi el servidor no proxea megabytes y el enlace muere solo. Cada llamada es una lectura de dato personal y queda registrada.

### Example
```dart
import 'package:aportaya_cliente_identidad/api.dart';

final api = AportayaClienteIdentidad().getDefaultApi();
final String verificacionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String cara = cara_example; // String | 

try {
    final response = api.verFotoDelExpediente(verificacionId, cara);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->verFotoDelExpediente: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **verificacionId** | **String**|  | 
 **cara** | **String**|  | 

### Return type

[**EnlaceDeFoto**](EnlaceDeFoto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **verificarTitularidad**
> SalidaTitularidad verificarTitularidad(usuarioId, entradaTitularidad)

Si el nombre y el documento declarados son los del titular

Contesta si coinciden; NO dice cuales son. Devolver el nombre y el documento guardados convertiria esta ruta en un directorio de clientes para cualquier servicio comprometido. El documento ademas se guarda como hash con pimienta: lo que se comparan son dos hashes, nunca dos numeros. La usa `entregas` antes de registrar una cuenta bancaria — R-SEG-02 exige que la cuenta sea de quien la registra, y ese dato vive aca.

### Example
```dart
import 'package:aportaya_cliente_identidad/api.dart';

final api = AportayaClienteIdentidad().getDefaultApi();
final String usuarioId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaTitularidad entradaTitularidad = ; // EntradaTitularidad | 

try {
    final response = api.verificarTitularidad(usuarioId, entradaTitularidad);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->verificarTitularidad: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **usuarioId** | **String**|  | 
 **entradaTitularidad** | [**EntradaTitularidad**](EntradaTitularidad.md)|  | 

### Return type

[**SalidaTitularidad**](SalidaTitularidad.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

