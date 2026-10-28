// HU-14 · Panel de monitoreo del coordinador (y del jefe de carrera, que lee lo mismo).
// Reutiliza GET /api/panel?periodo=&asignaturaId=: cifras, una fila por asignatura, planificaciones pendientes,
// estudiantes sin evaluar y el avance de cada estudiante.
import { api } from '../api.js';
import { asignaturas } from '../datos.js';
import { alertaError, badge, cabecera, cargando, debounce, fecha, html, icono, nota, promedio, vacio } from '../ui.js';

/** Periodo en curso: el semestre 1 va de marzo a julio y el 2 de agosto a diciembre (como S-48). */
const periodoActual = (hoy = new Date()) => `${hoy.getFullYear()}-${hoy.getMonth() < 7 ? 1 : 2}`;

const ESPERA = { EN_REVISION_GUIA: 'Revisión del guía', EN_AUTORIZACION: 'Autorización del profesor del curso', CON_OBSERVACIONES: 'Corrección del estudiante' };

export async function render(cont, ctx) {
  ctx.titulo('Panel de monitoreo');
  const catalogo = await asignaturas();

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-14', titulo: 'Panel de monitoreo',
      subtitulo: 'El avance de cada asignatura y de cada estudiante: planificaciones pendientes, evaluaciones hechas y estudiantes sin evaluar.',
    })}
    <div class="barra-herramientas">
      <label class="filtro">Período
        <input type="text" id="p-periodo" value="${periodoActual()}" placeholder="todos" maxlength="6" style="width:7em" aria-label="Período (vacío = todos)"></label>
      <label class="filtro">Práctica
        <select id="p-asignatura"><option value="">Todas</option>${catalogo.map((a) => html`<option value="${a.id}">${a.orden}. ${a.nombre}</option>`)}</select></label>
    </div>
    <div id="panel"></div>`.toString();

  const zona = cont.querySelector('#panel');
  const fPeriodo = cont.querySelector('#p-periodo');
  const fAsig = cont.querySelector('#p-asignatura');
  let pedido = 0;

  async function cargar() {
    const periodo = fPeriodo.value.trim();
    if (periodo && !/^[0-9]{4}-[12]$/.test(periodo)) return;   // se espera a que termine de escribirlo
    const mio = ++pedido;
    zona.innerHTML = cargando('Cargando el panel…').toString();
    try {
      const p = await api.get('/api/panel', { periodo, asignaturaId: fAsig.value });
      if (mio === pedido) pintar(zona, p);
    } catch (e) {
      if (mio === pedido) zona.innerHTML = alertaError(e).toString();
    }
  }

  fPeriodo.addEventListener('input', debounce(cargar, 300));
  fAsig.addEventListener('change', cargar);
  await cargar();
}

function cifra(titulo, valor, destacar = false) {
  return html`<div class="cifra-panel ${destacar && valor > 0 ? 'alerta-cifra' : ''}"><dt>${titulo}</dt><dd>${valor}</dd></div>`;
}

function pintar(zona, p) {
  const r = p.resumen;
  if (r.inscripciones === 0) {
    zona.innerHTML = vacio('No hay inscripciones con esos filtros', 'Prueba con otro período o deja el período vacío para ver todos.', 'chart').toString();
    return;
  }
  const historial = (id) => `#/historial/${id}`;

  zona.innerHTML = html`
    <dl class="cifras-panel">
      ${cifra('Inscripciones en curso', `${r.enCurso} de ${r.inscripciones}`)}
      ${cifra('Esperan al guía', r.pendientesGuia, true)}
      ${cifra('Esperan al profesor del curso', r.pendientesCurso, true)}
      ${cifra('Devueltas al estudiante', r.conObservaciones, true)}
      ${cifra('Evaluaciones finalizadas', r.evaluacionesFinalizadas)}
      ${cifra('Evaluaciones en borrador', r.evaluacionesBorrador)}
      ${cifra('Estudiantes sin evaluar', r.estudiantesSinEvaluar, true)}
    </dl>

    <div class="tarjeta">
      <h2>Por asignatura</h2>
      <div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>Práctica</th><th class="num">Inscripciones</th><th class="num">En curso</th><th class="num">Esperan guía</th>
          <th class="num">Esperan curso</th><th class="num">Devueltas</th><th class="num">Autorizadas</th>
          <th class="num">Evaluaciones</th><th class="num">Borradores</th><th class="num">Sin evaluar</th><th class="num">Promedio</th></tr></thead>
        <tbody>${p.asignaturas.map((a) => html`<tr>
          <td><strong>${a.orden}. ${a.nombre}</strong></td><td class="num">${a.inscripciones}</td><td class="num">${a.enCurso}</td>
          <td class="num">${a.pendientesGuia}</td><td class="num">${a.pendientesCurso}</td><td class="num">${a.conObservaciones}</td>
          <td class="num">${a.autorizadas}</td><td class="num">${a.evaluacionesFinalizadas}</td><td class="num">${a.evaluacionesBorrador}</td>
          <td class="num">${a.sinEvaluar}</td><td class="num">${promedio(a.promedioEvaluaciones)}</td>
        </tr>`)}</tbody>
      </table></div>
    </div>

    <div class="rejilla-2">
      <div class="tarjeta">
        <h2>${icono('file')} Planificaciones pendientes <small class="subtitulo">(${p.planificacionesPendientes.length})</small></h2>
        ${p.planificacionesPendientes.length === 0 ? html`<p class="subtitulo">No hay planificaciones esperando a nadie.</p>` : html`
          <div class="tabla-contenedor"><table class="tabla">
            <thead><tr><th>Planificación</th><th>Espera</th><th class="num">Días</th></tr></thead>
            <tbody>${p.planificacionesPendientes.map((x) => html`<tr>
              <td><a href="#/planificaciones/${x.planificacionId}">${x.titulo}</a><br><small>${x.estudiante} · ${x.asignatura} · clase ${fecha(x.fechaClase)}</small></td>
              <td>${badge(x.estado)}<br><small>${ESPERA[x.estado]}: <strong>${x.esperaA}</strong></small></td>
              <td class="num">${x.diasEsperando}</td>
            </tr>`)}</tbody>
          </table></div>`}
      </div>
      <div class="tarjeta">
        <h2>${icono('alert')} Estudiantes sin evaluar <small class="subtitulo">(${p.sinEvaluar.length})</small></h2>
        <p class="subtitulo">En curso y sin ninguna evaluación finalizada, en las prácticas que ya tienen pauta en el sistema.</p>
        ${p.sinEvaluar.length === 0 ? html`<p class="subtitulo">Todos los estudiantes en curso tienen al menos una evaluación.</p>` : html`
          <ul class="planes">${p.sinEvaluar.map((s) => html`<li>
            <a href="${historial(s.estudianteId)}">${s.estudiante}</a>
            <small>${s.asignatura} (${s.periodo}) · guía: ${s.profesorGuia ?? 'no lleva'}${s.borradores ? ` · ${s.borradores} en borrador` : ''}</small>
          </li>`)}</ul>`}
      </div>
    </div>

    <div class="tarjeta">
      <h2>Avance de cada estudiante</h2>
      <div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>Estudiante</th><th>Práctica</th><th>Período</th><th>Estado</th><th class="num">Planificaciones</th>
          <th class="num">Evaluaciones</th><th class="num">Promedio</th><th class="num">Nota</th></tr></thead>
        <tbody>${p.avance.map((a) => html`<tr>
          <td><a href="${historial(a.estudianteId)}">${a.estudiante}</a></td><td>${a.asignatura}</td><td>${a.periodo}</td>
          <td>${badge(a.estado)}</td>
          <td class="num">${a.planificacionesAutorizadas} de ${a.planificaciones} <small>autorizadas</small></td>
          <td class="num">${a.evaluacionesFinalizadas}</td><td class="num">${promedio(a.promedioEvaluaciones)}</td>
          <td class="num">${a.notaFinal != null ? nota(a.notaFinal) : a.notaCalculada != null ? html`${nota(a.notaCalculada)} <small>(calc.)</small>` : '—'}</td>
        </tr>`)}</tbody>
      </table></div>
      <p class="pie-tabla">${p.avance.length} inscripci${p.avance.length === 1 ? 'ón' : 'ones'}</p>
    </div>`.toString();
}
