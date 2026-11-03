import { Observable } from 'rxjs';

import { Availability, Classroom, ClassroomFilter } from '../entities/classroom';

/**
 * Contrato de acceso al catálogo y a la disponibilidad de aulas.
 * Las implementaciones concretas (HTTP) viven en la capa de infraestructura;
 * los casos de uso y las pruebas dependen solo de esta abstracción.
 */
export interface ClassroomRepository {
  /** Lista el catálogo aplicando los filtros dados. */
  getAll(filter: ClassroomFilter): Observable<Classroom[]>;

  /** Aulas libres en el intervalo [inicio, fin), solo con reservas APROBADAS. */
  getAvailable(range: { start: Date; end: Date }, filter: ClassroomFilter): Observable<Availability>;
}
