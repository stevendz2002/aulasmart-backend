/* Contratos JSON reales del backend para /api/aulas (verificados en vivo).
   Formas externas, nombres del backend; solo la infraestructura los conoce. */

export interface CatalogItemDto {
  id: number;
  codigoSiga: number;
  nombre: string;
}

export interface ClassroomDto {
  id: number;
  codigoSiga: number;
  nombre: string;
  edificio: CatalogItemDto;
  capacidad: number;
  tipoAula: CatalogItemDto;
  dependencia: CatalogItemDto;
}

export interface AvailabilityDto {
  inicio: string;
  fin: string;
  verificadoEn: string;
  aulas: ClassroomDto[];
}
