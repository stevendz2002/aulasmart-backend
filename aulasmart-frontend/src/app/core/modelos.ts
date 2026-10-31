/* Contratos JSON reales del backend (verificados en vivo contra /api/aulas,
   /api/aulas/disponibles y /api/solicitudes). */

export interface Catalogo {
  id: number;
  codigoSiga: number;
  nombre: string;
}

export interface Aula {
  id: number;
  codigoSiga: number;
  nombre: string;
  edificio: Catalogo;
  capacidad: number;
  tipoAula: Catalogo;
  dependencia: Catalogo;
}

export interface Disponibilidad {
  inicio: string;
  fin: string;
  verificadoEn: string;
  aulas: Aula[];
}

export type EstadoSolicitud = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';

export interface Solicitud {
  id: number;
  aulaId: number;
  docenteId: number;
  inicio: string;
  fin: string;
  estado: EstadoSolicitud;
  motivo: string;
  revisadaPorId: number | null;
  revisadaEn: string | null;
  creadaEn: string;
}

export type DecisionRevision = 'APROBAR' | 'RECHAZAR';
