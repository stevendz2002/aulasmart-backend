import { UserDto, CreateUserDto } from '../dto/user-dto';
import { CreateUserAccountCommand, User } from '../../domain/entities/user';

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
}
