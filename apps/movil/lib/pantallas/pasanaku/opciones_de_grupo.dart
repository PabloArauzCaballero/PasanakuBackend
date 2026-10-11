/// Las opciones cerradas del formulario de «Organizar un grupo»: lo que la persona elige
/// entre tarjetas, con el nombre que se le muestra. Las claves son los valores del contrato.
library;

const periodicidades = {
  'SEMANAL': (nombre: 'Semanal', alAno: '52 turnos al año'),
  'QUINCENAL': (nombre: 'Quincenal', alAno: '24 al año'),
  'MENSUAL': (nombre: 'Mensual', alAno: '12 al año'),
  'BIMENSUAL': (nombre: 'Bimensual', alAno: '6 al año'),
};

const modalidades = {
  'SORTEO_ALEATORIO': (
    nombre: 'Sorteo al azar',
    detalle: 'Nadie elige: el orden sale sorteado y verificable',
  ),
  'ORDEN_DE_INGRESO': (
    nombre: 'Orden de ingreso',
    detalle: 'Cobra primero quien entró primero',
  ),
  'ACUERDO_MANUAL': (
    nombre: 'Lo acuerda el grupo',
    detalle: 'Ustedes fijan el orden entre todos',
  ),
};
