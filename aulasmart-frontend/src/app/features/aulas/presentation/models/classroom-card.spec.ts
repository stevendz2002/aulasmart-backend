import { ClassroomCardBuilder } from './classroom-card';
import { Classroom } from '../../domain/entities/classroom';

const catalog = { id: 1, externalCode: 1, name: 'Bloque B' };
const aula: Classroom = { id: 9, externalCode: 195, name: 'B101-PANTALLA (195)', capacity: 50, building: catalog, type: catalog, department: catalog };

describe('Nombre y código de la tarjeta', () => {
  it('separa el código numérico final conservando el nombre completo para búsquedas', () => {
    const card = ClassroomCardBuilder.of(aula, 'LIBRE');
    expect(card.displayName).toBe('B101-Pantalla');
    expect(card.referenceCode).toBe('195');
    expect(card.name).toBe(aula.name);
  });
  it('conserva nombres sin código y paréntesis descriptivos sin inventar referencias', () => {
    const card = ClassroomCardBuilder.of({ ...aula, name: 'Laboratorio (Química)' }, 'DESCONOCIDO');
    expect(card.displayName).toBe('Laboratorio (Química)');
    expect(card.referenceCode).toBeNull();
    expect(card.statusLabel).toBe('Sin verificar');
  });
  it('normaliza ubicación y etiquetas sin alterar códigos ni datos originales', () => {
    const card = ClassroomCardBuilder.of({ ...aula, building: { ...catalog, name: 'BLOQUE B - AVELLANOS' }, type: { ...catalog, name: 'SALÓN DE CLASE' }, department: { ...catalog, name: 'INFORMÁTICA' } }, 'OCUPADA');
    expect(card.building).toBe('Bloque B - Avellanos');
    expect(card.characteristics).toEqual(['Salón de clase', 'Informática']);
    expect(card.statusLabel).toBe('Ocupada');
    expect(card.name).toBe('B101-PANTALLA (195)');
  });
});
