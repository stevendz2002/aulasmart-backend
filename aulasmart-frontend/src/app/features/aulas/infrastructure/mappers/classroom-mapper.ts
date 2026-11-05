import { ClassroomDto, AvailabilityDto, OccupationDto } from '../dto/classroom-dto';
import { Availability, Classroom } from '../../domain/entities/classroom';
import { ClassroomOccupation } from '../../domain/entities/occupation';

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

  static toOccupation(dto: OccupationDto): ClassroomOccupation {
    return {
      start: dto.inicio,
      end: dto.fin,
      details: dto.detalles
    };
  }
}
