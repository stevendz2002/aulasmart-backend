/* Entidades de dominio del catálogo de aulas.
   Puro TypeScript: sin Angular, HttpClient ni DTO externos. */

/** Elemento de catálogo sincronizado desde SIGA (edificio, tipo o dependencia). */
export interface ClassroomCatalogItem {
  id: number;
  externalCode: number;
  name: string;
}

/** Aula del catálogo de la universidad. */
export interface Classroom {
  id: number;
  externalCode: number;
  name: string;
  building: ClassroomCatalogItem;
  capacity: number;
  type: ClassroomCatalogItem;
  department: ClassroomCatalogItem;
}

/** Criterios de búsqueda sobre el catálogo (coinciden con los parámetros de la API). */
export interface ClassroomFilter {
  minCapacity?: number;
  buildingId?: number;
  typeId?: number;
  departmentId?: number;
}

export interface AvailabilityRange {
  start: string;
  end: string;
  verifiedAt: string;
}

/** Aulas libres en un intervalo [inicio, fin) verificado contra la base. */
export interface Availability {
  range: AvailabilityRange;
  classrooms: Classroom[];
}
