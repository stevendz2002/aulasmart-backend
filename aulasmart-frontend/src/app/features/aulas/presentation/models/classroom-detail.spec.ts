import { ClassroomDetailBuilder } from './classroom-detail';
import { Classroom } from '../../domain/entities/classroom';

describe('Horario en Colombia', () => {
  const catalogo = { id: 1, externalCode: 1, name: 'Catálogo' };
  const aula: Classroom = { id: 1, externalCode: 1, name: 'Aula', capacity: 30, building: catalogo, type: catalogo, department: catalogo };

  it('recorta eventos de varios días y no crea una franja libre falsa', () => {
    const detalle = ClassroomDetailBuilder.of(aula, 'OCUPADA', [
      { start: '2026-11-09T23:00:00-05:00', end: '2026-11-11T01:00:00-05:00', details: 'Evento' }
    ], '2026-11-10');
    expect(detalle.timeline).toEqual([{ start: '07:00', end: '22:00', label: 'Evento', free: false }]);
  });

  it('representa 13:00 UTC como 08:00 en Colombia', () => {
    const detalle = ClassroomDetailBuilder.of(aula, 'OCUPADA', [
      { start: '2026-11-10T13:00:00Z', end: '2026-11-10T14:00:00Z', details: 'Clase' }
    ], '2026-11-10');
    expect(detalle.timeline.find(f => !f.free)?.start).toBe('08:00');
  });
});
