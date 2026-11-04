/* Contratos JSON reales del backend para /api/usuarios (verificados en vivo). */

export interface UserDto {
  id: number;
  nombre: string;
  correo: string;
  activo: boolean;
  roles: string[];
  creadoEn: string;
}

export interface CreateUserDto {
  nombre: string;
  correo: string;
  clave: string;
  roles: string[];
}

export interface UpdateUserDto {
  nombre?: string;
  roles?: string[];
  activo?: boolean;
}
