import { ClassroomDto, AvailabilityDto } from '../dto/classroom-dto';
import { Availability, Classroom } from '../../domain/entities/classroom';

/**
 * Adapter (patrón Adapter): traduce los DTO del backend, en español tal como
 * los expone la API, a las entidades de dominio en inglés. Aísla el dominio y
 * la presentación de cambios en la forma externa de los datos.
 */
export class ClassroomMapper {
  static toClassroom(dto: ClassroomDto): Classroom {
    return {
      id: dto.id,
      externalCode: dto.codigoSiga,
      name: dto.nombre,
      building: { id: dto.edificio.id, externalCode: dto.edificio.codigoSiga, name: dto.edificio.nombre },
      capacity: dto.capacidad,
      type: { id: dto.tipoAula.id, externalCode: dto.tipoAula.codigoSiga, name: dto.tipoAula.nombre },
      department: {
        id: dto.dependencia.id,
        externalCode: dto.dependencia.codigoSiga,
        name: dto.dependencia.nombre
      }
    };
  }

  static toAvailability(dto: AvailabilityDto): Availability {
    return {
      range: { start: dto.inicio, end: dto.fin, verifiedAt: dto.verificadoEn },
      classrooms: dto.aulas.map(ClassroomMapper.toClassroom)
    };
  }
}
