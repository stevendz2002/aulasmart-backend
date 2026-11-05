import { Classroom } from '../../domain/entities/classroom';
import { ClassroomOccupation } from '../../domain/entities/occupation';

export type ClassroomAvailabilityStatus = 'LIBRE' | 'OCUPADA' | 'DESCONOCIDO';

/** Una fila del horario de hoy en la ficha del aula. */
export interface TimelineEntry {
  readonly start: string;
  readonly end: string;
  readonly label: string;
  readonly free: boolean;
}

/** Vista inmutable de la ficha de detalles del aula (Modal 1). */
export interface ClassroomDetail {
  readonly id: number;
  readonly name: string;
  readonly subtitle: string;
  readonly status: ClassroomAvailabilityStatus;
  readonly statusLabel: string;
  readonly capacity: number;
  readonly building: string;
  readonly freeNowLabel: string;
  readonly characteristics: readonly string[];
  readonly timeline: readonly TimelineEntry[];
}

/**
 * Patrón Builder: ensambla la ficha de detalles del aula a partir de la
 * entidad de dominio, su estado en la franja consultada y la ocupación real
 * de SIGA. Encapsula las reglas de presentación (etiquetas, chips, gaps
 * libres del horario) y evita constructores telescópicos.
 */
export class ClassroomDetailBuilder {
  private classroom?: Classroom;
  private status: ClassroomAvailabilityStatus = 'DESCONOCIDO';
  private occupations: ClassroomOccupation[] = [];

  forClassroom(classroom: Classroom): this {
    this.classroom = classroom;
    return this;
  }

  withStatus(status: ClassroomAvailabilityStatus): this {
    this.status = status;
    return this;
  }

  withOccupations(occupations: ClassroomOccupation[]): this {
    this.occupations = occupations;
    return this;
  }

  build(): ClassroomDetail {
    if (this.classroom === undefined) {
      throw new Error('ClassroomDetailBuilder requiere un aula (forClassroom) antes de build().');
    }
    const classroom = this.classroom;
    return {
      id: classroom.id,
      name: classroom.name,
      subtitle: `${classroom.type.name} · ${classroom.building.name}`,
      status: this.status,
      statusLabel: this.status === 'LIBRE' ? 'Disponible' : this.status === 'OCUPADA' ? 'Ocupada' : 'Sin verificar',
      capacity: classroom.capacity,
      building: classroom.building.name,
      freeNowLabel: this.status === 'LIBRE' ? 'Libre ahora' : 'Ocupada en la franja',
      characteristics: [classroom.type.name, classroom.department.name],
      timeline: this.buildTimeline(this.occupations)
    };
  }

  static of(classroom: Classroom, status: ClassroomAvailabilityStatus, occupations: ClassroomOccupation[]): ClassroomDetail {
    return new ClassroomDetailBuilder().forClassroom(classroom).withStatus(status).withOccupations(occupations).build();
  }

  private buildTimeline(occupations: ClassroomOccupation[]): TimelineEntry[] {
    const DAY_START = 7 * 60;
    const DAY_END = 22 * 60;
    const toMinutes = (iso: string): number => {
      const d = new Date(iso);
      return d.getHours() * 60 + d.getMinutes();
    };
    const fmt = (mins: number): string => {
      const h = Math.floor(mins / 60);
      const m = mins % 60;
      return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
    };

    const sorted = [...occupations]
      .map(o => ({ start: Math.max(toMinutes(o.start), DAY_START), end: Math.min(toMinutes(o.end), DAY_END), details: o.details }))
      .filter(o => o.end > o.start)
      .sort((a, b) => a.start - b.start);

    const entries: TimelineEntry[] = [];
    let cursor = DAY_START;
    for (const occ of sorted) {
      if (occ.start > cursor) {
        entries.push({ start: fmt(cursor), end: fmt(occ.start), label: 'Libre', free: true });
      }
      entries.push({ start: fmt(occ.start), end: fmt(occ.end), label: occ.details ?? 'Ocupada', free: false });
      cursor = Math.max(cursor, occ.end);
    }
    if (cursor < DAY_END) {
      entries.push({ start: fmt(cursor), end: fmt(DAY_END), label: 'Libre', free: true });
    }
    return entries;
  }
}
