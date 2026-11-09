import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { HttpUserRepository } from './http-user-repository';
import { User } from '../../domain/entities/user';

describe('HttpUserRepository', () => {
  let repository: HttpUserRepository;
  let requests: HttpTestingController;

  const usuarioDto = {
    id: 9003,
    nombre: 'PRUEBA Administrador',
    correo: 'admin@prueba.aulasmart.local',
    activo: true,
    rol: 'ADMINISTRADOR',
    creadoEn: '2026-11-03T03:24:03Z'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    repository = TestBed.inject(HttpUserRepository);
    requests = TestBed.inject(HttpTestingController);
  });

  afterEach(() => requests.verify());

  it('lista el directorio y mapea el DTO al dominio', () => {
    let usuarios: User[] | undefined;
    repository.getAll().subscribe(result => (usuarios = result));

    requests.expectOne('http://localhost:8080/api/usuarios').flush([usuarioDto]);

    expect(usuarios).toEqual([
      {
        id: 9003,
        fullName: 'PRUEBA Administrador',
        email: 'admin@prueba.aulasmart.local',
        active: true,
        role: 'ADMINISTRADOR',
        createdAt: '2026-11-03T03:24:03Z'
      }
    ]);
  });

  it('crea una cuenta traduciendo el comando al contrato real del backend', () => {
    let creada: User | undefined;
    repository
      .create({
        fullName: 'Ana María Restrepo',
        email: 'ana@uceva.edu.co',
        password: 'claveSegura1',
        role: 'DOCENTE'
      })
      .subscribe(result => (creada = result));

    const req = requests.expectOne('http://localhost:8080/api/usuarios');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      nombre: 'Ana María Restrepo',
      correo: 'ana@uceva.edu.co',
      clave: 'claveSegura1',
      rol: 'DOCENTE'
    });
    req.flush({
      id: 9,
      nombre: 'Ana María Restrepo',
      correo: 'ana@uceva.edu.co',
      activo: true,
      rol: 'DOCENTE',
      creadoEn: '2026-11-03T13:00:00Z'
    });

    expect(creada?.id).toBe(9);
    expect(creada?.fullName).toBe('Ana María Restrepo');
  });
});
