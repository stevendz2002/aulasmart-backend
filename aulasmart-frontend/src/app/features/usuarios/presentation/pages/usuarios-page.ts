import { Component, ChangeDetectionStrategy, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../../../core/auth-service';
import {
  CREATE_USER_ACCOUNT_USE_CASE,
  GET_USERS_USE_CASE,
  UPDATE_USER_ACCOUNT_USE_CASE
} from '../../application/users-di';
import { GetUsersUseCase } from '../../application/use-cases/get-users.use-case';
import { CreateUserAccountUseCase } from '../../application/use-cases/create-user-account.use-case';
import { UpdateUserAccountUseCase } from '../../application/use-cases/update-user-account.use-case';
import { CreateUserAccountCommand, SystemRole, UpdateUserAccountCommand, User } from '../../domain/entities/user';

const ROLES_SISTEMA: SystemRole[] = ['ADMINISTRADOR', 'ADMINISTRATIVO', 'DOCENTE', 'ESTUDIANTE'];

type Pestana = 'directorio' | 'crear' | 'editar';

@Component({
  selector: 'app-usuarios-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './usuarios-page.css',
  template: `
    <section class="cabecera">
      <h2>Usuarios</h2>
      <p class="ayuda">
        Directorio del sistema y creación/edición de cuentas. Solo el administrador
        gestiona cuentas (<code>GET/POST/PATCH /api/usuarios</code>).
      </p>
    </section>

    <section class="tarjeta">
      <div class="controles">
        <div class="pestanas" role="tablist" aria-label="Gestión de usuarios">
          <button type="button" role="tab" [attr.aria-selected]="pestanaActiva() === 'directorio'"
                  [class.activa]="pestanaActiva() === 'directorio'" (click)="cambiarPestana('directorio')">
            Directorio <span class="cuenta">{{ visibles().length }}</span>
          </button>
          <button type="button" role="tab" [attr.aria-selected]="pestanaActiva() === 'crear'"
                  [class.activa]="pestanaActiva() === 'crear'" (click)="cambiarPestana('crear')">
            Crear cuenta
          </button>
          @if (editando(); as usuario) {
            <button type="button" role="tab" [attr.aria-selected]="pestanaActiva() === 'editar'"
                    [class.activa]="pestanaActiva() === 'editar'" (click)="cambiarPestana('editar')">
              Editando: {{ usuario.fullName }}
            </button>
          }
        </div>

        @if (pestanaActiva() === 'directorio') {
          <label class="busqueda">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                 stroke-linecap="round" aria-hidden="true">
              <path d="M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM16.2 16.2 21 21" />
            </svg>
            <input
              #entradaBusqueda
              type="search"
              placeholder="Buscar por nombre, correo o rol…"
              aria-label="Buscar usuarios"
              [value]="textoBusqueda()"
              (input)="buscar(entradaBusqueda.value)"
            />
          </label>
        }
      </div>

      @if (pestanaActiva() === 'directorio') {
        @if (errorDirectorio(); as mensaje) {
          <p class="mensaje-error">{{ mensaje }}</p>
        }
        <div class="tabla-envoltorio">
          @if (cargandoDirectorio()) {
            <p class="mensaje-info">Consultando el directorio…</p>
          } @else if (usuarios().length === 0 && !errorDirectorio()) {
            <p class="mensaje-info">El directorio está vacío: crea la primera cuenta en la pestaña Crear cuenta.</p>
          } @else if (visibles().length === 0 && !errorDirectorio()) {
            <p class="mensaje-info">Ningún usuario coincide con la búsqueda.</p>
          } @else {
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Nombre</th>
                  <th>Correo</th>
                  <th>Rol</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                @for (usuario of visibles(); track usuario.id) {
                  <tr>
                    <td class="numero">{{ usuario.id }}</td>
                    <td class="nombre">{{ usuario.fullName }}</td>
                    <td>{{ usuario.email }}</td>
                    <td>{{ usuario.role }}</td>
                    <td><span [class]="usuario.active ? 'chip activo' : 'chip inactivo'">
                      {{ usuario.active ? 'Activo' : 'Inactivo' }}</span></td>
                    <td class="acciones">
                      <button type="button" class="boton secundario" (click)="iniciarEdicion(usuario)">Editar</button>
                      <button type="button" class="boton" [class.no]="usuario.active" (click)="alternarActivacion(usuario)">
                        {{ usuario.active ? 'Desactivar' : 'Activar' }}
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </div>
      }

      @if (pestanaActiva() === 'crear') {
        <form class="formulario cuenta-nueva" (submit)="crearCuenta($event)">
          <label for="nombre">
            Nombre completo
            <input id="nombre" type="text" required maxlength="120"
                   placeholder="Ej: Ana María Restrepo" autocomplete="off" />
          </label>

          <label for="correo">
            Correo institucional
            <input id="correo" type="email" required maxlength="254"
                   placeholder="nombre@uceva.edu.co" autocomplete="off" />
          </label>

          <label for="clave">
            Contraseña temporal (mínimo 8 caracteres)
            <input id="clave" type="password" required minlength="8"
                   placeholder="••••••••" autocomplete="new-password" />
          </label>

          <fieldset class="roles">
            <legend>Rol de la cuenta</legend>
            @for (rol of rolesCatalogo; track rol) {
              <label class="rol">
                <input type="radio" name="rol-creacion" required [checked]="rolElegido() === rol" (change)="rolElegido.set(rol)" />
                <span class="chip rol-chip">{{ rotuloRol(rol) }}</span>
              </label>
            }
          </fieldset>

          <button type="submit" class="boton" [disabled]="cargandoCreacion()">
            {{ cargandoCreacion() ? 'Creando cuenta…' : 'Crear cuenta' }}
          </button>

          @if (errorCreacion(); as mensaje) {
            <p class="mensaje-error">{{ mensaje }}</p>
          }
          @if (exitoCreacion(); as mensaje) {
            <p class="mensaje-exito">{{ mensaje }}</p>
          }
          <p class="nota">La respuesta confirma la cuenta creada y el directorio se recarga.</p>
        </form>
      }

      @if (pestanaActiva() === 'editar' && editando(); as usuario) {
        <form class="formulario cuenta-nueva" (submit)="guardarEdicion($event)">
          <label for="nombre-edicion">
            Nombre completo
            <input id="nombre-edicion" type="text" required maxlength="120"
                   [value]="nombreEdicion()" (input)="nombreEdicion.set($any($event.target).value)" />
          </label>

          <fieldset class="roles">
            <legend>Rol de la cuenta</legend>
            @for (rol of rolesCatalogo; track rol) {
              <label class="rol">
                <input type="radio" name="rol-edicion" required [checked]="rolEdicion() === rol" (change)="rolEdicion.set(rol)" />
                <span class="chip rol-chip">{{ rotuloRol(rol) }}</span>
              </label>
            }
          </fieldset>

          <div class="acciones-formulario">
            <button type="submit" class="boton" [disabled]="cargandoEdicion()">
              {{ cargandoEdicion() ? 'Guardando…' : 'Guardar cambios' }}
            </button>
            <button type="button" class="boton secundario" (click)="cancelarEdicion()">Cancelar</button>
          </div>

          @if (errorEdicion(); as mensaje) {
            <p class="mensaje-error">{{ mensaje }}</p>
          }
          @if (exitoEdicion(); as mensaje) {
            <p class="mensaje-exito">{{ mensaje }}</p>
          }
        </form>
      }
    </section>
  `
})
export class UsuariosPage implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly getUsers = inject<GetUsersUseCase>(GET_USERS_USE_CASE);
  private readonly createUserAccount = inject<CreateUserAccountUseCase>(CREATE_USER_ACCOUNT_USE_CASE);
  private readonly updateUserAccount = inject<UpdateUserAccountUseCase>(UPDATE_USER_ACCOUNT_USE_CASE);

  protected readonly pestanaActiva = signal<Pestana>('directorio');
  protected readonly usuarios = signal<User[]>([]);
  protected readonly cargandoDirectorio = signal(false);
  protected readonly errorDirectorio = signal<string | null>(null);

  protected readonly textoBusqueda = signal('');
  protected readonly visibles = computed(() => {
    const consulta = this.textoBusqueda().toLowerCase().trim();
    const todos = this.usuarios();
    if (!consulta) {
      return todos;
    }
    return todos.filter(
      usuario =>
        usuario.fullName.toLowerCase().includes(consulta) ||
        usuario.email.toLowerCase().includes(consulta) ||
        usuario.role.toLowerCase().includes(consulta)
    );
  });

  // Crear
  protected readonly rolesCatalogo = ROLES_SISTEMA;
  protected readonly rolElegido = signal<SystemRole>('DOCENTE');
  protected readonly cargandoCreacion = signal(false);
  protected readonly errorCreacion = signal<string | null>(null);
  protected readonly exitoCreacion = signal<string | null>(null);

  // Editar
  protected readonly editando = signal<User | null>(null);
  protected readonly nombreEdicion = signal('');
  protected readonly rolEdicion = signal<SystemRole>('DOCENTE');
  protected readonly cargandoEdicion = signal(false);
  protected readonly errorEdicion = signal<string | null>(null);
  protected readonly exitoEdicion = signal<string | null>(null);

  ngOnInit(): void {
    this.cargarDirectorio();
  }

  protected cambiarPestana(pestana: Pestana): void {
    this.pestanaActiva.set(pestana);
  }

  protected buscar(texto: string): void {
    this.textoBusqueda.set(texto);
  }

  protected rotuloRol(rol: SystemRole): string {
    const rotulos: Record<SystemRole, string> = {
      ADMINISTRADOR: 'Administrador',
      ADMINISTRATIVO: 'Administrativo',
      DOCENTE: 'Docente',
      ESTUDIANTE: 'Estudiante'
    };
    return rotulos[rol];
  }

  protected crearCuenta(evento: Event): void {
    evento.preventDefault();
    const formulario = evento.target as HTMLFormElement;

    const command: CreateUserAccountCommand = {
      fullName: formulario.querySelector<HTMLInputElement>('#nombre')!.value.trim(),
      email: formulario.querySelector<HTMLInputElement>('#correo')!.value.trim(),
      password: formulario.querySelector<HTMLInputElement>('#clave')!.value,
      role: this.rolElegido()
    };

    this.cargandoCreacion.set(true);
    this.errorCreacion.set(null);
    this.exitoCreacion.set(null);
    this.createUserAccount.execute(command).subscribe({
      next: usuario => {
        this.cargandoCreacion.set(false);
        this.exitoCreacion.set(`Cuenta creada para ${usuario.fullName}.`);
        this.rolElegido.set('DOCENTE');
        formulario.reset();
        this.cargarDirectorio();
      },
      error: (err: HttpErrorResponse) => {
        this.cargandoCreacion.set(false);
        this.errorCreacion.set(this.mensajeDeErrorDeCreacion(err));
      }
    });
  }

  protected iniciarEdicion(usuario: User): void {
    this.editando.set(usuario);
    this.nombreEdicion.set(usuario.fullName);
    this.rolEdicion.set(usuario.role);
    this.errorEdicion.set(null);
    this.exitoEdicion.set(null);
    this.pestanaActiva.set('editar');
  }

  protected guardarEdicion(evento: Event): void {
    evento.preventDefault();
    const usuario = this.editando();
    if (!usuario) {
      return;
    }
    const command: UpdateUserAccountCommand = {
      fullName: this.nombreEdicion().trim(),
      role: this.rolEdicion()
    };

    this.cargandoEdicion.set(true);
    this.errorEdicion.set(null);
    this.exitoEdicion.set(null);
    this.updateUserAccount.execute(usuario.id, command).subscribe({
      next: actualizado => {
        this.cargandoEdicion.set(false);
        this.exitoEdicion.set(`Cuenta de ${actualizado.fullName} actualizada.`);
        this.authService.sesion.update(sesion => sesion?.correo === actualizado.email
          ? { ...sesion, nombre: actualizado.fullName, rol: actualizado.role } : sesion);
        this.usuarios.update(lista => lista.map(u => (u.id === actualizado.id ? actualizado : u)));
      },
      error: (err: HttpErrorResponse) => {
        this.cargandoEdicion.set(false);
        this.errorEdicion.set(this.mensajeDeErrorDeEdicion(err));
      }
    });
  }

  protected cancelarEdicion(): void {
    this.editando.set(null);
    this.pestanaActiva.set('directorio');
  }

  protected alternarActivacion(usuario: User): void {
    const accion = usuario.active ? 'desactivar' : 'activar';
    if (!confirm(`¿Confirmas ${accion} la cuenta de ${usuario.fullName}?`)) {
      return;
    }
    this.updateUserAccount.execute(usuario.id, { active: !usuario.active }).subscribe({
      next: actualizado => {
        this.usuarios.update(lista => lista.map(u => (u.id === actualizado.id ? actualizado : u)));
      },
      error: (err: HttpErrorResponse) => {
        this.errorDirectorio.set(this.mensajeDeErrorDeEdicion(err));
      }
    });
  }

  private cargarDirectorio(): void {
    this.cargandoDirectorio.set(true);
    this.errorDirectorio.set(null);
    this.getUsers.execute().subscribe({
      next: usuarios => {
        this.usuarios.set(usuarios);
        this.cargandoDirectorio.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.cargandoDirectorio.set(false);
        this.errorDirectorio.set(
          err.status === 401 || err.status === 403
            ? 'La API entregó el directorio solo al administrador: tu sesión no conserva ese permiso.'
            : `No fue posible consultar el directorio (HTTP ${err.status}).`
        );
      }
    });
  }

  private mensajeDeErrorDeCreacion(err: HttpErrorResponse): string {
    const detalle = typeof err.error?.message === 'string' ? err.error.message : null;
    if (err.status === 409) {
      return detalle ?? 'Ya existe una cuenta con ese correo.';
    }
    if (err.status === 400) {
      return detalle ?? 'Datos inválidos: revisa nombre, correo, clave (mín. 8) y rol.';
    }
    if (err.status === 401 || err.status === 403) {
      return 'Solo el administrador puede crear cuentas con esta API.';
    }
    return `No fue posible crear la cuenta (HTTP ${err.status}).`;
  }

  private mensajeDeErrorDeEdicion(err: HttpErrorResponse): string {
    const detalle = typeof err.error?.message === 'string' ? err.error.message : null;
    if (err.status === 404) {
      return detalle ?? 'La cuenta ya no existe en el directorio.';
    }
    if (err.status === 400) {
      return detalle ?? 'Datos inválidos: revisa nombre y rol.';
    }
    if (err.status === 401 || err.status === 403) {
      return 'Solo el administrador puede editar cuentas con esta API.';
    }
    return `No fue posible actualizar la cuenta (HTTP ${err.status}).`;
  }
}
