/**
 * Aqui vive el adaptador de la pasarela REAL (hoy no existe: P-15, sin proveedor contratado). Debe implementar
 * {@code dominio.PasarelaDePagos} e {@code dominio.InterpreteDeWebhook}, activarse con
 * {@code aportaya.aportes.pasarela.modo=real} y NO depender de {@code ..pasarela.simulada..} (lo verifica
 * {@code ArquitecturaTest}). Dominio y aplicacion no cambian.
 */
package bo.aportaya.aportes.infraestructura.pasarela.real;
