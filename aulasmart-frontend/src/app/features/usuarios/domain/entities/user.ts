/* Entidades de dominio de la gestión de usuarios.
   Puro TypeScript: sin Angular, HttpClient ni DTO externos. */

/** Roles reales del catálogo del backend (tabla rol del esquema V1). */
export type SystemRole = 'ADMINISTRADOR' | 'ADMINISTRATIVO' | 'DOCENTE' | 'ESTUDIANTE';

/** Cuenta del sistema para el directorio del administrador (sin claves). */
export interface User {
  id: number;
  fullName: string;
  email: string;
  active: boolean;
  role: SystemRole;
  createdAt: string;
}

/** Datos para crear una cuenta. */
export interface CreateUserAccountCommand {
  fullName: string;
  email: string;
  password: string;
  role: SystemRole;
}

/** Cambios parciales sobre una cuenta existente (null/ausente = no tocar). */
export interface UpdateUserAccountCommand {
  fullName?: string;
  role?: SystemRole;
  active?: boolean;
}
