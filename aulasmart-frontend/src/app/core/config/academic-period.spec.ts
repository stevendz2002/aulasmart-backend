import { describe, expect, it } from 'vitest';

import { ACADEMIC_PERIOD, semanaDelPeriodo } from './academic-period';

const periodoBase = { label: '2026-2', startDate: '2026-08-10', totalWeeks: 16 } as const;

describe('semanaDelPeriodo', () => {
  it('semana 1 en el primer día de inicio de clases', () => {
    expect(semanaDelPeriodo(periodoBase, new Date('2026-08-10T10:00:00'))).toBe(1);
  });

  it('semana 5 a los 28 días de iniciado el período', () => {
    expect(semanaDelPeriodo(periodoBase, new Date('2026-09-07T10:00:00'))).toBe(5);
  });

  it('devuelve la última semana dentro del rango del período', () => {
    expect(semanaDelPeriodo(periodoBase, new Date('2026-11-23T10:00:00'))).toBe(16);
  });

  it('devuelve null antes del inicio de clases', () => {
    expect(semanaDelPeriodo(periodoBase, new Date('2026-08-02T10:00:00'))).toBeNull();
  });

  it('devuelve null después de la última semana', () => {
    expect(semanaDelPeriodo(periodoBase, new Date('2026-12-01T10:00:00'))).toBeNull();
  });

  it('el período configurado corresponde a 16 semanas', () => {
    expect(ACADEMIC_PERIOD.totalWeeks).toBe(16);
  });
});
