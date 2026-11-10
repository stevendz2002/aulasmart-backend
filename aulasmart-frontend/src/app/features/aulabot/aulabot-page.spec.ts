import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Subject, of, throwError } from 'rxjs';
import { AulaBotPage } from './aulabot-page';
import { AuthService } from '../../core/auth-service';
import { SendChatMessageUseCase, BotReply } from './application/send-chat-message.use-case';

describe('AulaBot', () => {
  const execute = vi.fn();
  const executeAction = vi.fn();
  const reply: BotReply = {texto:'¿Fecha?',estado:'ACLARACION',aulas:[],inicio:null,fin:null,verificadoEn:null,filtros:null,total:0,sugerencias:['CAMBIAR_BLOQUE','CAMBIAR_HORARIO']};
  let page: AulaBotPage;
  beforeEach(() => {
    execute.mockReset();executeAction.mockReset();
    TestBed.configureTestingModule({providers:[
      {provide: AuthService,useValue:{sesion:signal({correo:'docente'}),tieneAlgunRol:()=>true}},
      {provide: SendChatMessageUseCase,useValue:{execute,executeAction}}
    ]});
    page=TestBed.runInInjectionContext(()=>new AulaBotPage());
  });
  it('cada mensaje se envía como texto con contexto en el servidor', () => {
    execute.mockReturnValue(of(reply));
    page['draft']='para 40 personas';page['send']();
    page['draft']='mañana de 8 a 10';page['send']();
    expect(execute).toHaveBeenNthCalledWith(1,'para 40 personas',false);
    expect(execute).toHaveBeenNthCalledWith(2,'mañana de 8 a 10',false);
    expect(executeAction).not.toHaveBeenCalled();
  });
  it('los chips envían acciones estructuradas sin llamar al modelo', () => {
    executeAction.mockReturnValue(of(reply));
    page['pick']('CAMBIAR_BLOQUE');
    expect(executeAction).toHaveBeenCalledWith('CAMBIAR_BLOQUE');
    expect(execute).not.toHaveBeenCalled();
    expect(page['turns']()[0].question).toBe('Cambiar bloque');
  });
  it('las sugerencias vienen de la respuesta del backend', () => {
    expect(page['suggestions'](null)).toEqual(['BUSCAR_AULA','CONSULTAR_DISPONIBILIDAD','VER_OPCIONES']);
    expect(page['suggestions'](reply)).toEqual(['CAMBIAR_BLOQUE','CAMBIAR_HORARIO']);
  });
  it('evita doble envío y cancela respuestas de una conversación anterior', () => {
    const pending=new Subject<BotReply>();execute.mockReturnValue(pending);
    page['draft']='hola';page['send']();page['draft']='otra';page['send']();
    expect(execute).toHaveBeenCalledTimes(1);
    page['reset']();pending.next(reply);
    expect(page['turns']()).toEqual([]);expect(page['loading']()).toBe(false);
  });
  it('permite reintentar sin duplicar el mensaje ni la señal de reinicio', () => {
    execute.mockReturnValueOnce(throwError(()=>new Error())).mockReturnValueOnce(of(reply));
    page['draft']='buscar B101';page['send']();
    expect(page['turns']()[0].error).toBeTruthy();
    page['retry'](0);
    expect(page['turns']()).toHaveLength(1);expect(page['turns']()[0].reply).toEqual(reply);
  });
  it('la señal de nueva conversación se envía una sola vez tras el reinicio', () => {
    execute.mockReturnValue(of(reply));
    page['reset']();page['draft']='bloque A';page['send']();page['draft']='bloque B';page['send']();
    expect(execute).toHaveBeenNthCalledWith(1,'bloque A',true);
    expect(execute).toHaveBeenNthCalledWith(2,'bloque B',false);
  });
  it('no envía mensajes vacíos ni demasiado largos', () => {
    page['draft']=' ';page['send']();page['draft']='x'.repeat(601);page['send']();expect(execute).not.toHaveBeenCalled();
  });
});
