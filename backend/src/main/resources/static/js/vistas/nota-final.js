// HU-09 · Estructura evaluativa (coordinador): componentes de la nota final y sus pesos por asignatura.
// Reutiliza GET/PUT /api/asignaturas/{id}/componentes-nota. La nota de cada componente la registra el profesor del
// curso desde «Mis inscripciones» (botón «Nota»).
import { ApiError, api } from '../api.js';
import { asignaturas } from '../datos.js';
import { alerta, alertaForm, cabecera, cajaAlertaForm, conCarga, decimal, html, icono, limpiarAlertaForm, toast } from '../ui.js';

export async function render(cont, ctx) {
  ctx.titulo('Nota final por asignatura');
  const lista = await asignaturas();
  const [primera] = ctx.args;

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-09', titulo: 'Nota final ponderada',
      subtitulo: 'Define los componentes de la nota final de cada asignatura y su peso. El profesor del curso registra la nota de cada componente y el sistema calcula la nota ponderada.',
    })}
    <div class="tarjeta">
      <div class="barra-herramientas">
        <label class="filtro">Asignatura
          <select id="sel-asignatura">${lista.map((a) => html`<option value="${a.id}" ${String(a.id) === String(primera ?? 5) ? 'selected' : ''}>${a.orden}. ${a.nombre}</option>`)}</select>
        </label>
      </div>
      ${alerta('info', 'Los pesos deben sumar 100 %. Un componente que ya tiene notas no se puede quitar, pero sí renombrar o cambiar su peso. Al guardar se recalcula la nota de las inscripciones en curso. La escala (1,0 a 7,0 con un decimal) está por confirmar con la carrera.')}
      <form id="form-estructura" novalidate>
        ${cajaAlertaForm()}
        <div class="tabla-contenedor"><table class="tabla">
          <thead><tr><th>Componente</th><th class="num">Peso (%)</th><th></th></tr></thead>
          <tbody id="filas"></tbody>
          <tfoot><tr><th>Total</th><th class="num" id="suma">—</th><th></th></tr></tfoot>
        </table></div>
        <div class="barra-herramientas">
          <button type="button" class="btn" id="agregar">${icono('plus')} Agregar componente</button>
          <button type="submit" class="btn btn-primario" data-cargando="Guardando…">Guardar estructura</button>
        </div>
      </form>
    </div>`.toString();

  const sel = cont.querySelector('#sel-asignatura');
  const form = cont.querySelector('#form-estructura');
  const filas = cont.querySelector('#filas');
  const suma = cont.querySelector('#suma');

  const fila = (c = {}) => html`<tr data-id="${c.id ?? ''}">
    <td><input type="text" name="nombre" maxlength="150" value="${c.nombre ?? ''}" aria-label="Nombre del componente" required style="width:100%"></td>
    <td class="num"><input type="number" name="peso" min="0.01" max="100" step="0.01" value="${c.peso ?? ''}" aria-label="Peso en porcentaje" required style="width:6em"></td>
    <td class="acciones"><button type="button" class="btn btn-chico btn-fantasma" data-quitar aria-label="Quitar">${icono('x')}</button></td>
  </tr>`;

  const actualizarSuma = () => {
    const total = [...filas.querySelectorAll('input[name=peso]')].reduce((s, i) => s + (Number(i.value) || 0), 0);
    suma.textContent = `${decimal(total, 2)} %`;
    suma.classList.toggle('texto-error', filas.children.length > 0 && Math.abs(total - 100) > 0.001);
  };

  async function cargar() {
    limpiarAlertaForm(form);
    const e = await api.get(`/api/asignaturas/${sel.value}/componentes-nota`);
    filas.innerHTML = e.componentes.length === 0
      ? ''
      : e.componentes.map((c) => fila(c)).join('');
    actualizarSuma();
  }

  filas.addEventListener('input', actualizarSuma);
  filas.addEventListener('click', (ev) => {
    if (ev.target.closest('[data-quitar]')) { ev.target.closest('tr').remove(); actualizarSuma(); }
  });
  cont.querySelector('#agregar').addEventListener('click', () => {
    filas.insertAdjacentHTML('beforeend', fila().toString());
    filas.lastElementChild.querySelector('input').focus();
    actualizarSuma();
  });
  sel.addEventListener('change', () => cargar().catch((e) => alertaForm(form, e)));

  form.addEventListener('submit', (ev) => {
    ev.preventDefault();
    limpiarAlertaForm(form);
    const componentes = [...filas.children].map((tr) => ({
      id: tr.dataset.id ? Number(tr.dataset.id) : null,
      nombre: tr.querySelector('input[name=nombre]').value.trim(),
      peso: Number(tr.querySelector('input[name=peso]').value),
    }));
    if (componentes.some((c) => !c.nombre || !(c.peso > 0))) {
      alertaForm(form, new ApiError(400, 'Cada componente necesita un nombre y un peso mayor que 0.'));
      return;
    }
    conCarga(form.querySelector('button[type=submit]'), async () => {
      const e = await api.put(`/api/asignaturas/${sel.value}/componentes-nota`, { componentes });
      toast(`Estructura de ${e.asignatura} guardada.`);
      await cargar();
    }).catch((e) => alertaForm(form, e));
  });

  await cargar();
}
