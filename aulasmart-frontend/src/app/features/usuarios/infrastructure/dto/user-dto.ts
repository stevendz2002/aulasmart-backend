import { SystemRole } from "../../domain/entities/user";
/* Contratos JSON reales del backend para /api/usuarios (verificados en vivo). */

export interface UserDto {
  id: number;
  nombre: string;
  correo: string;
  activo: boolean;
  rol: SystemRole;
  creadoEn: string;
}

export interface CreateUserDto {
  nombre: string;
  correo: string;
  clave: string;
  rol: SystemRole;
}

export interface UpdateUserDto {
  nombre?: string;
  rol?: SystemRole;
  activo?: boolean;
}
