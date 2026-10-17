// HU-06 · Configurar pautas de evaluación (solo coordinador).
//   #/pautas                 todas las pautas y sus versiones, con cuántas evaluaciones usa cada una
//   #/pautas/nueva           crear una pauta
//   #/pautas/{id}            ver una pauta (cualquier versión) con sus dimensiones e indicadores
//   #/pautas/{id}/editar     editar la versión vigente: si ya se usó, al guardar se crea la versión siguiente
import { api } from '../api.js';
import { asignaturas } from '../datos.js';
import {
  alerta, alertaError, cabecera, cajaAlertaForm, campo, cargando, confirmar, enlazarFormulario, fecha, html, icono, toast,
  vacio,
} from '../ui.js';

const EVALUADORES = { COLABORADOR: 'Profesor colaborador (guía)', TUTOR: 'Tutor', ACADEMICO: 'Académico (profesor del curso)' };
const AMBITOS = { OBSERVACION_CLASE: 'Observación de clase', ESPECIALIDAD: 'Especialidad', ORIENTACION: 'Orientación' };
// HU-07: las pautas de Especialidad y Orientación son de la primera o de la segunda observación (instrumentos distintos).
const OBSERVACIONES = { 1: 'Primera observación', 2: 'Segunda observación' };
const conObservacion = (p) => (p.numeroObservacion ? ` · ${p.numeroObservacion}.ª observación` : '');
const opciones = (mapa) => Object.entries(mapa).map(([valor, texto]) => ({ valor, texto }));
const estadoPauta = (activa) => (activa
  ? html`<span class="badge badge-AUTORIZADA">Activa</span>`
  : html`<span class="badge badge-REPROBADA">Inactiva</span>`);

export async function render(cont, ctx) {
  const [a, b] = ctx.args;
  if (!a) return lista(cont, ctx);
  if (a === 'nueva') return formulario(cont, ctx, null);
  if (b === 'editar') return formulario(cont, ctx, a);
  return detalle(cont, ctx, a);
}

// ============================ LISTA ============================
async function lista(cont, ctx) {
  ctx.titulo('Pautas de evaluación');
  const catalogo = await asignaturas();
  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-06', titulo: 'Configurar pautas de evaluación',
      subtitulo: 'Crea y edita las pautas: dimensiones, indicadores, escala, quién la aplica y su ámbito. Una pauta que ya se usó no se modifica: al editarla se crea una versión nueva y las evaluaciones hechas conservan la suya.',
      acciones: html`<a class="btn btn-primario" href="#/pautas/nueva">${icono('plus')} Nueva pauta</a>`,
    })}
    <div class="barra-herramientas">
      <label class="filtro">Asignatura
        <select id="f-asignatura"><option value="">Todas</option>${catalogo.map((x) => html`<option value="${x.id}">${x.nombre}</option>`)}</select></label>
      <label class="filtro filtro-check"><input type="checkbox" id="f-versiones"> Mostrar versiones antiguas</label>
    </div>
    <div id="avisos"></div>
    <div id="lista"></div>`.toString();

  const zona = cont.querySelector('#lista');
  const avisos = cont.querySelector('#avisos');
  const fAsig = cont.querySelector('#f-asignatura');
  const fVers = cont.querySelector('#f-versiones');
  let datos = [];

  const pintar = () => {
    const filas = datos.filter((p) => fVers.checked || p.vigente);
    zona.innerHTML = filas.length === 0
      ? vacio('No hay pautas para mostrar', 'Crea la primera con «Nueva pauta».', 'clipboard').toString()
      : html`<div class="tabla-contenedor"><table class="tabla tabla-pautas">
          <thead><tr><th>Pauta</th><th>Quién la aplica · ámbito</th><th>Escala e indicadores</th><th class="num">Evaluaciones</th><th>Estado</th><th></th></tr></thead>
          <tbody>${filas.map((p) => html`<tr>
            <td><strong>${p.nombre}</strong><br><small>${p.asignatura} · versión ${p.version}${p.vigente ? '' : ' (reemplazada)'} · ${fecha(p.fechaCreacion)}</small></td>
            <td>${EVALUADORES[p.tipoEvaluador] ?? p.tipoEvaluador}<br><small>${AMBITOS[p.ambito] ?? p.ambito}${conObservacion(p)}</small></td>
            <td>Escala ${p.escalaMin} a ${p.escalaMax}<br><small>${p.totalIndicadores} indicadores · máx. ${p.puntajeMaximo} pts</small></td>
            <td class="num">${p.evaluaciones}</td>
            <td>${estadoPauta(p.activa)}</td>
            <td class="acciones">
              <a class="btn btn-chico" href="#/pautas/${p.id}">Ver</a>
              ${p.vigente ? html`
                <a class="btn btn-chico" href="#/pautas/${p.id}/editar">${icono('edit')} Editar</a>
                <button type="button" class="btn btn-chico" data-activa="${p.id}" data-valor="${p.activa ? 'false' : 'true'}">${p.activa ? 'Desactivar' : 'Activar'}</button>
                ${p.evaluaciones === 0 ? html`<button type="button" class="btn btn-chico btn-peligro" data-borrar="${p.id}">Borrar</button>` : ''}` : ''}
            </td>
          </tr>`)}</tbody></table></div>
        <p class="pie-tabla">${filas.length} pauta${filas.length === 1 ? '' : 's'}</p>`.toString();
  };

  async function cargar() {
    zona.innerHTML = cargando('Cargando pautas…').toString();
    try {
      datos = await api.get('/api/pautas/gestion', { asignaturaId: fAsig.value });
      pintar();
    } catch (e) {
      zona.innerHTML = alertaError(e).toString();
    }
  }

  zona.addEventListener('click', async (ev) => {
    const activar = ev.target.closest('[data-activa]');
    const borrar = ev.target.closest('[data-borrar]');
    if (!activar && !borrar) return;
    avisos.innerHTML = '';
    const p = datos.find((x) => String(x.id) === (activar ?? borrar).dataset[activar ? 'activa' : 'borrar']);
    try {
      if (activar) {
        const activa = activar.dataset.valor === 'true';
        const ok = activa || await confirmar({
          titulo: 'Desactivar pauta',
          mensaje: `«${p.nombre}» dejará de ofrecerse para evaluaciones nuevas. Las evaluaciones ya hechas con ella no cambian.`,
          textoOk: 'Desactivar',
        });
        if (!ok) return;
        await api.put(`/api/pautas/${p.id}/activa`, { activa });
        toast(activa ? 'Pauta activada.' : 'Pauta desactivada.');
      } else {
        const ok = await confirmar({
          titulo: 'Borrar pauta',
          mensaje: p.version > 1
            ? `Se borrará la versión ${p.version} de «${p.nombre}» (nadie la ha usado) y la versión ${p.version - 1} volverá a ser la vigente.`
            : `Se borrará «${p.nombre}» con sus dimensiones e indicadores. Nadie la ha usado todavía.`,
          textoOk: 'Borrar', peligro: true,
        });
        if (!ok) return;
        await api.del(`/api/pautas/${p.id}`);
        toast('Pauta borrada.');
      }
      await cargar();
    } catch (e) {
      if (e.status !== 401) avisos.innerHTML = alertaError(e).toString();
    }
  });
  fAsig.addEventListener('change', cargar);
  fVers.addEventListener('change', pintar);
  await cargar();
}

// ============================ DETALLE ============================
async function detalle(cont, ctx, id) {
  ctx.titulo('Pauta');
  const p = await api.get(`/api/pautas/${id}`);
  const catalogo = await asignaturas();
  const asig = catalogo.find((x) => x.id === p.asignaturaId);
  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-06', titulo: p.nombre,
      subtitulo: `${asig?.nombre ?? ''} · versión ${p.version} · ${EVALUADORES[p.tipoEvaluador] ?? p.tipoEvaluador} · ${AMBITOS[p.ambito] ?? p.ambito}${conObservacion(p)} · escala ${p.escalaMin} a ${p.escalaMax} · ${p.totalIndicadores} indicadores`,
      acciones: html`<a class="btn btn-fantasma" href="#/pautas">${icono('back')} Volver</a>`,
    })}
    <div class="tarjeta pauta">
      <p>${estadoPauta(p.activa)}${p.versionAnteriorId ? html` <a href="#/pautas/${p.versionAnteriorId}">Ver la versión anterior</a>` : ''}</p>
      ${p.dimensiones.map((d) => html`
        <section class="dimension">
          <h3>${d.nombre}</h3>
          <ol start="${d.indicadores[0]?.numero ?? 1}">${d.indicadores.map((i) => html`<li>${i.descripcion}</li>`)}</ol>
        </section>`)}
    </div>`.toString();
}

// ============================ FORMULARIO (crear / editar) ============================
async function formulario(cont, ctx, idEdicion) {
  const editando = idEdicion !== null;
  ctx.titulo(editando ? 'Editar pauta' : 'Nueva pauta');
  const volver = html`<a class="btn btn-fantasma" href="#/pautas">${icono('back')} Volver</a>`;
  const catalogo = await asignaturas();

  let actual = null;
  let usos = 0;
  if (editando) {
    actual = await api.get(`/api/pautas/${idEdicion}`);
    const fila = (await api.get('/api/pautas/gestion', { asignaturaId: actual.asignaturaId })).find((x) => x.id === actual.id);
    if (fila && !fila.vigente) {
      cont.innerHTML = html`${cabecera({ hu: 'HU-06', titulo: actual.nombre, acciones: volver })}
        ${alerta('aviso', html`Esta es la versión ${actual.version} y ya fue reemplazada: solo se edita la versión vigente.`)}`.toString();
      return;
    }
    usos = fila?.evaluaciones ?? 0;
  }
  const v = actual ?? { escalaMin: 1, escalaMax: 7, dimensiones: [{ nombre: '', indicadores: [] }] };

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-06', titulo: editando ? `Editar «${actual.nombre}»` : 'Nueva pauta',
      subtitulo: 'Los indicadores se numeran solos en el orden en que los escribes, a lo largo de toda la pauta.',
      acciones: volver,
    })}
    ${usos > 0 ? alerta('aviso', html`<strong>Esta pauta ya se usó en ${usos} evaluaci${usos === 1 ? 'ón' : 'ones'}.</strong> Al guardar cambios se creará la <strong>versión ${actual.version + 1}</strong>; la versión ${actual.version} queda intacta y las evaluaciones hechas con ella conservan sus indicadores y su puntaje máximo.`) : ''}
    <form class="tarjeta" id="form" novalidate>
      ${cajaAlertaForm()}
      <div class="rejilla-form">
        ${campo({ nombre: 'nombre', etiqueta: 'Nombre de la pauta', requerido: true, completo: true, valor: v.nombre ?? '', atributos: { maxlength: 200 } })}
        ${campo({ nombre: 'asignaturaId', etiqueta: 'Asignatura', tipo: 'select', requerido: true, valor: v.asignaturaId ?? '',
          atributos: { 'data-numero': true, ...(editando ? { disabled: true } : {}) },
          ayuda: editando ? 'La asignatura de una pauta no se cambia.' : '',
          opciones: catalogo.map((x) => ({ valor: x.id, texto: x.nombre })) })}
        ${campo({ nombre: 'tipoEvaluador', etiqueta: 'Quién la aplica', tipo: 'select', requerido: true, valor: v.tipoEvaluador ?? '', opciones: opciones(EVALUADORES) })}
        ${campo({ nombre: 'ambito', etiqueta: 'Ámbito', tipo: 'select', requerido: true, valor: v.ambito ?? '', opciones: opciones(AMBITOS) })}
        ${campo({ nombre: 'numeroObservacion', etiqueta: 'Observación', tipo: 'select', valor: v.numeroObservacion ?? '',
          atributos: { 'data-numero': true }, opciones: opciones(OBSERVACIONES),
          ayuda: 'Solo en Especialidad y Orientación: la primera y la segunda observación son pautas distintas.' })}
        ${campo({ nombre: 'escalaMin', etiqueta: 'Escala: puntaje mínimo', tipo: 'number', requerido: true, valor: v.escalaMin, atributos: { min: 0, max: 100 } })}
        ${campo({ nombre: 'escalaMax', etiqueta: 'Escala: puntaje máximo', tipo: 'number', requerido: true, valor: v.escalaMax, atributos: { min: 1, max: 100 } })}
      </div>
      <h2>Dimensiones e indicadores</h2>
      <p class="subtitulo">Cada dimensión agrupa indicadores. Escribe <strong>un indicador por línea</strong>.</p>
      <div id="dimensiones"></div>
      <button type="button" class="btn" id="agregar">${icono('plus')} Agregar dimensión</button>
      <div class="acciones-form">
        <a class="btn" href="#/pautas">Cancelar</a>
        <button type="submit" class="btn btn-primario" data-cargando="Guardando…">${editando ? 'Guardar cambios' : 'Crear pauta'}</button>
      </div>
    </form>`.toString();

  const form = cont.querySelector('#form');
  const zonaDims = cont.querySelector('#dimensiones');
  // El campo "Observación" solo tiene sentido fuera de la observación de clase.
  const campoObs = form.elements.numeroObservacion.closest('.campo');
  const alCambiarAmbito = () => {
    campoObs.hidden = form.elements.ambito.value === 'OBSERVACION_CLASE' || !form.elements.ambito.value;
    form.elements.numeroObservacion.required = !campoObs.hidden;   // visible = obligatorio
  };
  form.elements.ambito.addEventListener('change', alCambiarAmbito);
  alCambiarAmbito();
  let siguiente = 0; // sufijo único de los campos de cada dimensión

  const agregarDimension = (d = { nombre: '', indicadores: [] }) => {
    const n = siguiente++;
    const caja = document.createElement('fieldset');
    caja.className = 'tarjeta dimension-editor';
    caja.dataset.dimension = n;
    caja.innerHTML = html`
      <div class="rejilla-form">
        ${campo({ nombre: `dim-nombre-${n}`, etiqueta: 'Nombre de la dimensión', requerido: true, completo: true, valor: d.nombre, atributos: { maxlength: 150, placeholder: 'A. Aspectos de la estructura de la clase' } })}
        ${campo({ nombre: `dim-indicadores-${n}`, etiqueta: 'Indicadores (uno por línea)', tipo: 'textarea', requerido: true, completo: true,
          valor: d.indicadores.map((i) => (typeof i === 'string' ? i : i.descripcion)).join('\n'), atributos: { rows: 6 } })}
      </div>
      <button type="button" class="btn btn-chico btn-peligro" data-quitar>${icono('x')} Quitar dimensión</button>`.toString();
    caja.querySelector('[data-quitar]').addEventListener('click', () => {
      if (zonaDims.children.length > 1) caja.remove();
      else toast('La pauta necesita al menos una dimensión.', 'error');
    });
    zonaDims.appendChild(caja);
  };
  v.dimensiones.forEach((d) => agregarDimension(d));
  cont.querySelector('#agregar').addEventListener('click', () => agregarDimension());

  enlazarFormulario(form, form.querySelector('button[type=submit]'), {
    alEnviar: async (datos) => {
      const cuerpo = {
        asignaturaId: editando ? actual.asignaturaId : datos.asignaturaId,
        nombre: datos.nombre, tipoEvaluador: datos.tipoEvaluador, ambito: datos.ambito,
        numeroObservacion: datos.ambito === 'OBSERVACION_CLASE' ? null : (datos.numeroObservacion || null),
        escalaMin: datos.escalaMin, escalaMax: datos.escalaMax,
        dimensiones: [...zonaDims.children].map((caja) => ({
          nombre: datos[`dim-nombre-${caja.dataset.dimension}`],
          indicadores: String(datos[`dim-indicadores-${caja.dataset.dimension}`] ?? '').split('\n').map((t) => t.trim()).filter(Boolean),
        })),
      };
      if (!editando) return api.post('/api/pautas', cuerpo);
      if (usos > 0) {
        const ok = await confirmar({
          titulo: 'Crear versión nueva',
          mensaje: `La pauta ya se usó en ${usos} evaluaci${usos === 1 ? 'ón' : 'ones'}. Se creará la versión ${actual.version + 1} y la ${actual.version} quedará inactiva (si no cambiaste nada, no se crea ninguna versión).`,
          textoOk: 'Crear versión',
        });
        if (!ok) return null;
      }
      return api.put(`/api/pautas/${actual.id}`, cuerpo);
    },
    alListo: (p) => {
      if (!p) return;
      toast(editando && p.id !== actual.id ? `Se creó la versión ${p.version}.` : 'Pauta guardada.');
      ctx.ir(`#/pautas/${p.id}`);
    },
  });
}
