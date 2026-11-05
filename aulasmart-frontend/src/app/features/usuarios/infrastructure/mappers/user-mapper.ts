import { UserDto, CreateUserDto, UpdateUserDto } from '../dto/user-dto';
import { CreateUserAccountCommand, UpdateUserAccountCommand, User } from '../../domain/entities/user';

/**
 * Adapter (patrón Adapter): traduce los DTO del backend, en español tal como
 * los expone la API, a las entidades de dominio. Aísla dominio y presentación
 * de cambios en la forma externa de los datos.
 */
export class UserMapper {
  static toUser(dto: UserDto): User {
    return {
      id: dto.id,
      fullName: dto.nombre,
      email: dto.correo,
      active: dto.activo,
      roles: dto.roles,
      createdAt: dto.creadoEn
    };
  }

  static toCreateDto(command: CreateUserAccountCommand): CreateUserDto {
    return {
      nombre: command.fullName,
      correo: command.email,
      clave: command.password,
      roles: command.roles
    };
  }

  static toUpdateDto(command: UpdateUserAccountCommand): UpdateUserDto {
    const dto: UpdateUserDto = {};
    if (command.fullName !== undefined) {
      dto.nombre = command.fullName;
    }
    if (command.roles !== undefined) {
      dto.roles = command.roles;
    }
    if (command.active !== undefined) {
      dto.activo = command.active;
    }
    return dto;
  }
}
