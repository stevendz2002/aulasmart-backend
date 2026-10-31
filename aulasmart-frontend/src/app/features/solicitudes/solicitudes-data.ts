import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { apiUrl } from '../../core/api-url';
import { DecisionRevision, Solicitud } from '../../core/modelos';

export interface DatosDeReserva {
  aulaId: number;
  inicio: Date;
  fin: Date;
  motivo: string;
}

@Injectable({ providedIn: 'root' })
export class SolicitudesData {
  private readonly http = inject(HttpClient);

  listar(estado?: string): Observable<Solicitud[]> {
    let params = new HttpParams();
    if (estado && estado !== 'TODAS') {
      params = params.set('estado', estado);
    }
    return this.http.get<Solicitud[]>(`${apiUrl}/solicitudes`, { params });
  }

  crear(datos: DatosDeReserva): Observable<Solicitud> {
    return this.http.post<Solicitud>(`${apiUrl}/solicitudes`, {
      aulaId: datos.aulaId,
      inicio: datos.inicio.toISOString(),
      fin: datos.fin.toISOString(),
      motivo: datos.motivo
    });
  }

  revisar(idSolicitud: number, decision: DecisionRevision): Observable<Solicitud> {
    return this.http.post<Solicitud>(`${apiUrl}/solicitudes/${idSolicitud}/revision`, { decision });
  }
}
