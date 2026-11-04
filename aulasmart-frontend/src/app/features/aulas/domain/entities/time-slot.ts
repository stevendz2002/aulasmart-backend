/* Franjas horarias predefinidas para consultar disponibilidad: valores de
   dominio puros (sin Angular ni HttpClient), reutilizables en la SPA. */

/** Franja de tiempo del día usada para consultar disponibilidad. */
export interface TimeSlot {
  /** Identificador estable usado como clave de selección. */
  readonly id: string;
  /** Etiqueta legible para el usuario (es-CO). */
  readonly label: string;
  /** Hora de inicio en formato "HH:mm" (hora local de Colombia). */
  readonly start: string;
  /** Hora de fin en formato "HH:mm" (hora local de Colombia). */
  readonly end: string;
}

/**
 * Catálogo de franjas del día. Se mantienen en el dominio para que la
 * presentación solo las pinte y no las invente; los valores son los que el
 * backend acepta como inicio/fin en el endpoint `/api/aulas/disponibles`.
 */
export const DAY_TIME_SLOTS: readonly TimeSlot[] = [
  { id: '07-09', label: '07:00 – 09:00', start: '07:00', end: '09:00' },
  { id: '09-11', label: '09:00 – 11:00', start: '09:00', end: '11:00' },
  { id: '11-13', label: '11:00 – 13:00', start: '11:00', end: '13:00' },
  { id: '14-16', label: '14:00 – 16:00', start: '14:00', end: '16:00' },
  { id: '16-18', label: '16:00 – 18:00', start: '16:00', end: '18:00' },
  { id: '18-20', label: '18:00 – 20:00', start: '18:00', end: '20:00' },
  { id: '20-22', label: '20:00 – 22:00', start: '20:00', end: '22:00' }
];

/** Franja por defecto cuando la página abre (medio día del día siguiente). */
export const DEFAULT_TIME_SLOT_ID = '14-16';
