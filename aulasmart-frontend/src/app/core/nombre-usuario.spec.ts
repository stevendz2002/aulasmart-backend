import { describe, expect, it } from 'vitest';

import { inicialesDesdeCorreo, nombreDesdeCorreo } from './nombre-usuario';

describe('nombreDesdeCorreo', () => {
  it('deriva el nombre de un correo institucional con punto', () => {
    expect(nombreDesdeCorreo('paola.ramirez@uceva.edu.co')).toBe('Paola Ramirez');
  });

  it('deriva el nombre de correos con guion bajo o guion', () => {
    expect(nombreDesdeCorreo('maria_fernanda-rojas@uceva.edu.co')).toBe('Maria Fernanda Rojas');
  });

  it('deja un apellido compuesto con mayúsculas intermezcladas en título', () => {
    expect(nombreDesdeCorreo('andrea.delRio@prueba.aulasmart.local')).toBe('Andrea Delrio');
  });

  it('devuelve el correo cuando el local part no tiene palabras útiles', () => {
    expect(nombreDesdeCorreo('12345@uceva.edu.co')).toBe('12345@uceva.edu.co');
  });
});

describe('inicialesDesdeCorreo', () => {
  it('toma hasta dos iniciales de las palabras del correo', () => {
    expect(inicialesDesdeCorreo('paola.ramirez@uceva.edu.co')).toBe('PR');
  });

  it('usa la primera letra cuando hay una sola palabra', () => {
    expect(inicialesDesdeCorreo('docente@prueba.aulasmart.local')).toBe('D');
  });

  it('devuelve un valor por defecto con correos sin palabras', () => {
    expect(inicialesDesdeCorreo('77@x.co')).toBe('AU');
  });
});
