export interface BotRoom { id: number; codigoSiga: number; nombre: string; capacidad: number; edificio: {nombre: string}; tipoAula: {nombre: string}; }
export interface BotReply {
  texto: string; estado: string; aulas: BotRoom[]; inicio: string | null; fin: string | null;
  verificadoEn: string | null; filtros: Record<string, unknown> | null; total: number;
  sugerencias?: string[] | null;
}
