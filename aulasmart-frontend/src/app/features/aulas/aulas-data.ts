import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { apiUrl } from '../../core/api-url';
import { Aula, Disponibilidad } from '../../core/modelos';

export interface FiltroAulas {
  capacidadMinima?: number;
}

@Injectable({ providedIn: 'root' })
export class AulasData {
  private readonly http = inject(HttpClient);

  listar(filtro: FiltroAulas = {}): Observable<Aula[]> {
    return this.http.get<Aula[]>(`${apiUrl}/aulas`, { params: this.params(filtro) });
  }

  disponibles(inicio: Date, fin: Date, filtro: FiltroAulas = {}): Observable<Disponibilidad> {
    return this.http.get<Disponibilidad>(`${apiUrl}/aulas/disponibles`, {
      params: this.params({
        inicio: inicio.toISOString(),
        fin: fin.toISOString(),
        ...filtro
      })
    });
  }

  private params(valores: object): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(valores)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return params;
  }
}
