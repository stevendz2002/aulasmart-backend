import { Classroom } from '../../domain/entities/classroom';

/** Normaliza solo textos del catálogo escritos íntegramente en mayúsculas. */
function readableLabel(value: string, title = false): string {
  if (value !== value.toLocaleUpperCase('es')) return value;
  const lower = value.toLocaleLowerCase('es');
  const formatted = title
    ? lower.replace(/(^|[\s(-])(\p{L})/gu, (_, prefix, letter) => prefix + letter.toLocaleUpperCase('es'))
        .replace(/\b(De|Del|La|Las|El|Los|Y|En)\b/g, word => word.toLocaleLowerCase('es'))
    : lower;
  return formatted.replace(/\b[\p{L}\d]+\b/gu, word => /\d/.test(word) || word.length === 1 || ['uceva', 'siga', 'tic'].includes(word.toLowerCase()) ? word.toUpperCase() : word)
    .replace(/^\p{L}/u, letter => letter.toLocaleUpperCase('es'));
}

export type ClassroomAvailabilityStatus = 'LIBRE' | 'OCUPADA' | 'DESCONOCIDO';

/**
 * Vista de una aula para la consulta de disponibilidad: el modelo de
 * presentación construido a partir de las entidades de dominio (aula + estado
 * de la franja). Inmutable y sin dependencias de Angular/HttpClient.
 */
export interface ClassroomCard {
  readonly id: number;
  readonly name: string;
  readonly displayName: string;
  readonly referenceCode: string | null;
  readonly building: string;
  readonly type: string;
  readonly department: string;
  readonly capacity: number;
  readonly status: ClassroomAvailabilityStatus;
  readonly statusLabel: string;
  readonly characteristics: readonly string[];
}

/**
 * Patrón Builder: construye de forma fluida e inmutable un `ClassroomCard`
 * a partir de la entidad de dominio `Classroom` y su estado en la franja.
 * Encapsula las reglas de presentación (etiquetas, valores por defecto) y
 * permite ensamblar la vista sin constructores telescópicos.
 */
export class ClassroomCardBuilder {
  private classroom?: Classroom;
  private status: ClassroomAvailabilityStatus = 'DESCONOCIDO';

  /** Aula de dominio que se va a representar. */
  forClassroom(classroom: Classroom): this {
    this.classroom = classroom;
    return this;
  }

  /** Estado de la aula en la franja consultada. */
  withStatus(status: ClassroomAvailabilityStatus): this {
    this.status = status;
    return this;
  }

  /** Construye la tarjeta inmutable; falla si falta el aula. */
  build(): ClassroomCard {
    if (this.classroom === undefined) {
      throw new Error('ClassroomCardBuilder requiere un aula (forClassroom) antes de build().');
    }
    const classroom = this.classroom;
    const suffix = classroom.name.match(/^(.*?)\s*\((\d+)\)\s*$/);
    return {
      id: classroom.id,
      name: classroom.name,
      displayName: readableLabel(suffix?.[1].trim() || classroom.name, true),
      referenceCode: suffix?.[2] ?? null,
      building: readableLabel(classroom.building.name, true),
      type: classroom.type.name,
      department: classroom.department.name,
      capacity: classroom.capacity,
      status: this.status,
      statusLabel: this.labelFor(this.status),
      characteristics: [readableLabel(classroom.type.name), readableLabel(classroom.department.name)]
    };
  }

  /** Construcción directa para el caso más común (aula + estado). */
  static of(classroom: Classroom, status: ClassroomAvailabilityStatus): ClassroomCard {
    return new ClassroomCardBuilder().forClassroom(classroom).withStatus(status).build();
  }

  private labelFor(status: ClassroomAvailabilityStatus): string {
    switch (status) {
      case 'LIBRE':
        return 'Libre';
      case 'OCUPADA':
        return 'Ocupada';
      default:
        return 'Sin verificar';
    }
  }
}
