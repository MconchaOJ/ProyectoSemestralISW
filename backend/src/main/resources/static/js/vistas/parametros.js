// HU-27 · Parámetros del sistema. Rigen desde la petición siguiente. Usa GET /api/parametros y PUT /api/parametros/{clave}.
// Cada parámetro trae su rango (S-80) y qué roles lo editan: el coordinador todos; el jefe de carrera solo la exigencia
// de la escala de notas (2026-11-22). Los que no puede editar se muestran de solo lectura.
import { api } from '../api.js';
import { alertaError, cabecera, conCarga, fechaHora, html, icono, toast } from '../ui.js';

const NOMBRES = {
  'planificacion.dias_anticipacion': 'Anticipación mínima de una planificación',
  'panel.dias_atencion': 'Días para «requiere atención» en el panel',
  'archivos.documento_mb': 'Tamaño máximo de un documento adjunto',
  'archivos.audio_mb': 'Tamaño máximo del audio de una nota de voz',
  'evaluacion.exigencia_pct': 'Exigencia de la escala de notas',
};
const ORDEN = Object.keys(NOMBRES);

export async function render(cont, ctx) {
  ctx.titulo('Parámetros del sistema');
  const editable = (p) => (p.rolesQueEditan ?? ['COORDINADOR']).some((r) => ctx.tiene(r));
  const lista = (await api.get('/api/parametros'))
    .sort((a, b) => ORDEN.indexOf(a.clave) - ORDEN.indexOf(b.clave));

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-27', titulo: 'Parámetros del sistema',
      subtitulo: 'Reglas operativas de la carrera. Un cambio rige desde la siguiente acción de cualquier usuario, sin reiniciar el sistema. Los tamaños de archivo solo se pueden bajar del máximo actual. La exigencia de la escala rige para las evaluaciones que se finalicen después: las notas ya calculadas no cambian.',
    })}
    ${lista.map((p) => editable(p) ? html`
      <form class="tarjeta" data-parametro="${p.clave}" novalidate>
        <div class="tarjeta-titulo"><h2>${NOMBRES[p.clave] ?? p.clave}</h2></div>
        <p class="ayuda">${p.descripcion}</p>
        <div class="rejilla-form">
          <div class="campo">
            <label for="f-${p.clave}">Valor (${p.unidad})</label>
            <input type="number" id="f-${p.clave}" name="valor" value="${p.valor}" min="${p.minimo}" max="${p.maximo}" step="1" required>
            <span class="ayuda">Entre ${p.minimo} y ${p.maximo} ${p.unidad}.</span>
          </div>
          <div class="campo">
            <span class="ayuda">${p.editadoPor ? `Último cambio: ${p.editadoPor.nombreCompleto}, ${fechaHora(p.fechaEdicion)}.` : 'Valor inicial: nadie lo ha cambiado.'}</span>
          </div>
        </div>
        <div data-alerta-parametro></div>
        <div class="acciones-form"><button type="submit" class="btn btn-primario" data-cargando="Guardando…">${icono('check')} Guardar</button></div>
      </form>` : html`
      <div class="tarjeta" data-solo-lectura="${p.clave}">
        <div class="tarjeta-titulo"><h2>${NOMBRES[p.clave] ?? p.clave}</h2></div>
        <p class="ayuda">${p.descripcion}</p>
        <p><strong>${p.valor} ${p.unidad}</strong> <small>(entre ${p.minimo} y ${p.maximo}). Solo el coordinador lo cambia.</small></p>
      </div>`)}`.toString();

  cont.querySelectorAll('form[data-parametro]').forEach((form) => {
    form.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const clave = form.dataset.parametro;
      const alertaCaja = form.querySelector('[data-alerta-parametro]');
      alertaCaja.innerHTML = '';
      const texto = form.elements.valor.value.trim();
      try {
        await conCarga(ev.submitter ?? form.querySelector('button[type=submit]'), () =>
          api.put(`/api/parametros/${clave}`, { valor: texto === '' ? null : Number(texto) }));
        toast(`${NOMBRES[clave] ?? clave}: guardado.`);
        await render(cont, ctx);
      } catch (e) {
        if (e.status !== 401) alertaCaja.innerHTML = alertaError(e).toString();
      }
    });
  });
}
