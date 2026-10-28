// HU-09 · Nota final ponderada de una inscripción (modal que se abre desde la tabla de inscripciones).
// Todos los que ven la inscripción ven sus componentes y la nota calculada; solo su profesor del curso, con la
// inscripción en curso, registra o corrige las notas (PUT /api/inscripciones/{id}/nota-final/componentes/{c}).
import { api } from '../api.js';
import { abrirModal, alerta, alertaError, cargando, conCarga, decimal, fechaHora, html, nota, toast, toastError } from '../ui.js';

export async function abrirNotaFinal(inscripcion, alTerminar) {
  if (!inscripcion) return;
  const m = abrirModal({
    titulo: 'Nota final ponderada',
    cuerpo: html`<p><strong>${inscripcion.estudiante.nombreCompleto}</strong><br>${inscripcion.asignatura.nombre} · ${inscripcion.periodo}</p>
      <div data-nota>${cargando()}</div>`,
    pie: html`<button type="button" class="btn" data-cerrar>Cerrar</button>
      <button type="button" class="btn btn-primario" data-guardar data-cargando="Guardando…" hidden>Guardar notas</button>`,
  });
  const zona = m.el.querySelector('[data-nota]');
  const guardar = m.el.querySelector('[data-guardar]');
  m.el.querySelectorAll('[data-cerrar]').forEach((b) => b.addEventListener('click', () => alTerminar?.()));

  let datos;
  const pintar = () => {
    if (datos.componentes.length === 0) {
      zona.innerHTML = alerta('info', 'Esta asignatura todavía no tiene estructura evaluativa: la define el coordinador en «Nota final por asignatura».').toString();
      guardar.hidden = true;
      return;
    }
    const editable = datos.puedeRegistrar;
    zona.innerHTML = html`
      ${datos.estructuraCompleta ? '' : alerta('aviso', `Los pesos de la asignatura suman ${decimal(datos.sumaPesos, 2)} %, no 100 %: no se calcula la nota.`)}
      <div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>Componente</th><th class="num">Peso</th><th class="num">Nota</th><th class="num">Aporte</th></tr></thead>
        <tbody>${datos.componentes.map((c) => html`<tr>
          <td>${c.nombre}${c.registradaPor ? html`<br><small>${c.registradaPor} · ${fechaHora(c.fechaRegistro)}</small>` : ''}</td>
          <td class="num">${String(Number(c.peso)).replace('.', ',')} %</td>
          <td class="num">${editable
            ? html`<input type="number" min="1" max="7" step="0.1" data-componente="${c.componenteId}" value="${c.nota ?? ''}" data-original="${c.nota ?? ''}" aria-label="Nota de ${c.nombre}" style="width:5em">`
            : nota(c.nota)}</td>
          <td class="num">${c.aporte === null ? '—' : decimal(c.aporte, 2)}</td>
        </tr>`)}</tbody>
        <tfoot><tr><th colspan="2">Nota calculada (${datos.registradas} de ${datos.componentes.length} componentes)</th>
          <th class="num" colspan="2"><strong>${nota(datos.notaCalculada)}</strong></th></tr></tfoot>
      </table></div>
      ${datos.notaFinal !== null ? html`<p>Nota de cierre puesta por el coordinador: <strong>${nota(datos.notaFinal)}</strong></p>` : ''}
      <p class="subtitulo">${editable
        ? 'Notas de 1,0 a 7,0 con un decimal (usa punto: 6.2). La nota calculada se actualiza al guardar; la de cierre la pone el coordinador al cerrar la inscripción.'
        : 'Solo lectura: las notas las registra el profesor del curso de esta inscripción.'}</p>`.toString();
    guardar.hidden = !editable;
  };

  try {
    datos = await api.get(`/api/inscripciones/${inscripcion.id}/nota-final`);
    pintar();
  } catch (e) {
    zona.innerHTML = alertaError(e).toString();
    return;
  }

  guardar.addEventListener('click', () => conCarga(guardar, async () => {
    const cambios = [...zona.querySelectorAll('input[data-componente]')]
      .filter((i) => i.value.trim() !== '' && i.value.trim() !== i.dataset.original);
    if (cambios.length === 0) { toast('No hay notas nuevas que guardar.'); return; }
    for (const i of cambios) {
      // Una por componente: si una falla (por ejemplo 7.5), las anteriores ya quedaron guardadas
      datos = await api.put(`/api/inscripciones/${inscripcion.id}/nota-final/componentes/${i.dataset.componente}`, { nota: Number(i.value) });
    }
    pintar();
    toast(datos.notaCalculada !== null ? `Notas guardadas. Nota calculada: ${nota(datos.notaCalculada)}.` : 'Notas guardadas.');
  }).catch(async (e) => {
    toastError(e);
    try { datos = await api.get(`/api/inscripciones/${inscripcion.id}/nota-final`); pintar(); } catch { /* queda lo que había */ }
  }));
}
