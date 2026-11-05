import { Classroom } from '../../domain/entities/classroom';

export type ClassroomAvailabilityStatus = 'LIBRE' | 'OCUPADA' | 'DESCONOCIDO';

/**
 * Vista de una aula para la consulta de disponibilidad: el modelo de
 * presentación construido a partir de las entidades de dominio (aula + estado
 * de la franja). Inmutable y sin dependencias de Angular/HttpClient.
 */
export interface ClassroomCard {
  readonly id: number;
  readonly name: string;
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
    return {
      id: classroom.id,
      name: classroom.name,
      building: classroom.building.name,
      type: classroom.type.name,
      department: classroom.department.name,
      capacity: classroom.capacity,
      status: this.status,
      statusLabel: this.labelFor(this.status),
      characteristics: [this.classroom.type.name, this.classroom.department.name]
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
        return 'Ocupada en la franja';
      default:
        return 'Sin verificar';
    }
  }
}
