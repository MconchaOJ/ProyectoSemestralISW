// HU-17 · El coordinador calibra el estilo de retroalimentación del asistente de planificación (chatbot).
//   #/estilo-asistente: un solo texto para todo el sistema (reglas, tono, expresiones y respuestas modelo).
//   El contexto de cada estudiante (semestre, prácticas aprobadas) lo agrega el sistema: no se edita aquí.
import { api } from '../api.js';
import {
  alerta, alertaError, cabecera, confirmar, conCarga, fechaHora, html, icono, toast,
} from '../ui.js';

export async function render(cont, ctx) {
  ctx.titulo('Estilo del asistente');
  let datos = await api.get('/api/chatbot/instrucciones');

  cont.innerHTML = html`
    ${cabecera({
      titulo: 'Estilo del asistente de planificación',
      subtitulo: 'Instrucciones con que el asistente retroalimenta las planificaciones de Práctica Pedagógica y Profesional: reglas, tono, expresiones a usar y a evitar, y respuestas modelo.',
    })}
    ${alerta('info', html`Los cambios rigen desde el <strong>siguiente mensaje</strong> de cualquier conversación; los mensajes ya guardados no cambian. El sistema agrega por su cuenta el contexto de cada estudiante (semestre, prácticas aprobadas, evaluaciones), y el texto viaja en cada mensaje: mientras más largo, más cuota del proveedor consume.`)}
    <form class="tarjeta" id="form-estilo" novalidate>
      <div data-alerta-form hidden></div>
      <p class="ayuda" id="estado-texto"></p>
      <label for="f-instrucciones">Instrucciones del asistente</label>
      <textarea id="f-instrucciones" class="estilo-texto" rows="26" maxlength="${datos.maximoCaracteres}" spellcheck="true"></textarea>
      <div class="estilo-pie">
        <small class="ayuda" id="contador" aria-live="polite"></small>
        <div class="estilo-acciones">
          <button type="button" class="btn btn-fantasma" id="btn-base">${icono('back')} Volver al texto base</button>
          <button type="button" class="btn" id="btn-descartar">Descartar cambios</button>
          <button type="submit" class="btn btn-primario" data-cargando="Guardando…">${icono('check')} Guardar</button>
        </div>
      </div>
    </form>`.toString();

  const form = cont.querySelector('#form-estilo');
  const area = form.querySelector('#f-instrucciones');
  const caja = form.querySelector('[data-alerta-form]');
  const estado = form.querySelector('#estado-texto');
  const contador = form.querySelector('#contador');
  const guardar = form.querySelector('button[type=submit]');

  const largo = () => area.value.trim().length;
  const pintarEstado = () => {
    estado.textContent = datos.editadoPor
      ? `Última edición: ${datos.editadoPor.nombreCompleto}, ${fechaHora(datos.fechaEdicion)}.${datos.esTextoBase ? ' Es igual al texto base.' : ''}`
      : 'Texto base de RetroPlani Mate (propuesta del coordinador de prácticas), sin editar.';
  };
  const pintarContador = () => {
    const n = largo();
    const fuera = n < datos.minimoCaracteres || n > datos.maximoCaracteres;
    contador.textContent = `${n.toLocaleString('es-CL')} de ${datos.maximoCaracteres.toLocaleString('es-CL')} caracteres`
      + (n < datos.minimoCaracteres ? ` (mínimo ${datos.minimoCaracteres})` : '')
      + (area.value !== datos.texto ? ' · cambios sin guardar' : '');
    contador.classList.toggle('texto-error', fuera);
  };
  const cargar = (texto) => { area.value = texto; pintarContador(); };

  pintarEstado();
  cargar(datos.texto);
  area.addEventListener('input', pintarContador);

  form.querySelector('#btn-descartar').addEventListener('click', () => { caja.hidden = true; cargar(datos.texto); });
  form.querySelector('#btn-base').addEventListener('click', async () => {
    const ok = await confirmar({
      titulo: 'Volver al texto base',
      mensaje: 'Se reemplazará el texto del editor por el texto base de RetroPlani Mate. No se guarda hasta que presiones «Guardar».',
      textoOk: 'Reemplazar',
    });
    if (ok) { caja.hidden = true; cargar(datos.textoBase); area.focus(); }
  });

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    caja.hidden = true;
    try {
      datos = await conCarga(guardar, () => api.put('/api/chatbot/instrucciones', { texto: area.value }));
      if (!ctx.vigente()) return;
      cargar(datos.texto);
      pintarEstado();
      toast('Instrucciones guardadas: rigen desde el próximo mensaje.');
    } catch (e) {
      if (e.status === 401) return;
      caja.innerHTML = alertaError(e).toString();
      caja.hidden = false;
    }
  });
}
