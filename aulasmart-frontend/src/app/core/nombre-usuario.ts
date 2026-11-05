/**
 * Derivaciones de presentación para el nombre visible de la sesión. La API
 * (`/api/usuarios/whoami`) entrega `nombre` real de la cuenta cuando puede
 * resolverlo; cuando llega, se muestra tal cual. Sin ese campo, el nombre
 * se deriva del correo, nunca se inventa (p. ej.
 * `paola.ramirez@uceva.edu.co` → «Paola Ramirez»).
 */

/** Convierte una frase en formato título (cada palabra capitalizada). */
function enTitulo(texto: string): string {
  return texto
    .split(/\s+/)
    .filter(palabra => palabra.length > 0)
    .map(palabra => palabra[0].toUpperCase() + palabra.slice(1).toLowerCase())
    .join(' ');
}

/** Nombre visible a partir del correo institucional. */
export function nombreDesdeCorreo(correo: string): string {
  const localPart = correo.split('@')[0] ?? '';
  const palabras = localPart.split(/[^a-zA-Z]+/).filter(palabra => palabra.length >= 2);
  if (palabras.length === 0) {
    return correo;
  }
  return enTitulo(palabras.join(' '));
}

/** Iniciales para el avatar (máximo 2) a partir del correo real. */
export function inicialesDesdeCorreo(correo: string): string {
  const palabras = (correo.split('@')[0] ?? '').split(/[^a-zA-Z]+/).filter(p => p.length > 0);
  if (palabras.length === 0) {
    return 'AU';
  }
  const iniciales = palabras.slice(0, 2).map(palabra => palabra[0].toUpperCase()).join('');
  return iniciales.length > 0 ? iniciales : 'AU';
}

/** Iniciales para el avatar a partir del nombre real de la cuenta. */
export function inicialesDesdeNombre(nombre: string): string {
  const palabras = nombre.split(/\s+/).filter(palabra => palabra.length > 0);
  if (palabras.length === 0) {
    return 'AU';
  }
  return palabras.slice(0, 2).map(palabra => palabra[0].toUpperCase()).join('');
}
