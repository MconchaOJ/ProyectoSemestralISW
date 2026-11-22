// HU-14 · Panel de monitoreo (coordinador y jefe de carrera, solo lectura).
//   #/panel?asignaturaId=3&periodo=2026-2   (ambos filtros opcionales)
// Usa GET /api/reportes/panel. "Requiere atención": planificaciones que esperan revisión más de 3 días (S-79) y
// estudiantes en curso sin ninguna evaluación finalizada en prácticas con guía. Del trabajo de Alej (commit 7e9b132):
// todas las planificaciones pendientes con a quién esperan y hace cuántos días, el avance por estudiante y los conteos
// de devueltas, autorizadas y borradores. Las planificaciones de prácticas ya cerradas no cuentan como pendientes.
import { api } from '../api.js';
import { asignaturas } from '../datos.js';
import { alertaError, badge, cabecera, cargando, decimal, fecha, html, icono, nombreRol, nota } from '../ui.js';

export async function render(cont, ctx) {
  ctx.titulo('Panel de monitoreo');
  const [catalogo, inscripciones] = await Promise.all([asignaturas(), api.get('/api/inscripciones')]);
  const periodos = [...new Set(inscripciones.map((i) => i.periodo))].sort().reverse();
  const pedidaAsig = ctx.consulta.get('asignaturaId') ?? '';
  const pedidoPer = ctx.consulta.get('periodo') ?? '';

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-14', titulo: 'Panel de monitoreo',
      subtitulo: 'Avance de cada práctica: planificaciones esperando revisión, evaluaciones hechas y estudiantes sin evaluar. Arriba, lo que requiere atención.',
    })}
    <div class="barra-herramientas">
      <label class="filtro">Práctica
        <select id="f-asignatura"><option value="">Todas</option>
          ${catalogo.map((a) => html`<option value="${a.id}" ${String(a.id) === pedidaAsig ? 'selected' : ''}>${a.orden}. ${a.nombre}</option>`)}</select></label>
      <label class="filtro">Período
        <select id="f-periodo"><option value="">Todos</option>
          ${periodos.map((p) => html`<option value="${p}" ${p === pedidoPer ? 'selected' : ''}>${p}</option>`)}</select></label>
    </div>
    <div id="panel"></div>`.toString();

  const selAsig = cont.querySelector('#f-asignatura');
  const selPer = cont.querySelector('#f-periodo');
  const zona = cont.querySelector('#panel');
  let pedido = 0;

  const cargar = async () => {
    const mio = ++pedido;
    const filtros = {};
    if (selAsig.value) filtros.asignaturaId = selAsig.value;
    if (selPer.value) filtros.periodo = selPer.value;
    history.replaceState(null, '', `#/panel${Object.keys(filtros).length ? `?${new URLSearchParams(filtros)}` : ''}`);
    zona.innerHTML = cargando().toString();
    try {
      const p = await api.get('/api/reportes/panel', filtros);
      if (mio === pedido) pintar(zona, p);
    } catch (e) {
      if (mio === pedido && e.status !== 401) zona.innerHTML = alertaError(e).toString();
    }
  };
  selAsig.addEventListener('change', cargar);
  selPer.addEventListener('change', cargar);
  await cargar();
}

function cifra(valor, texto, ico, alerta = false) {
  return html`<div class="tarjeta cifra-panel" style="flex:1 1 10rem;min-width:9rem${alerta && valor > 0 ? ';border-left:4px solid #c0392b' : ''}">
    <div class="subtitulo">${icono(ico)} ${texto}</div><div style="font-size:1.8rem;font-weight:700">${valor}</div></div>`;
}

function pintar(zona, p) {
  const t = p.totales;
  zona.innerHTML = html`
    <div style="display:flex;flex-wrap:wrap;gap:.75rem;margin-bottom:1rem">
      ${cifra(t.enCurso, 'Prácticas en curso', 'users')}
      ${cifra(t.planificacionesEnRevisionGuia + t.planificacionesEnAutorizacion, 'Planificaciones esperando revisión', 'file')}
      ${cifra(p.planificacionesEsperando.length, `Esperando más de ${p.diasAtencion} días`, 'alert', true)}
      ${cifra(t.evaluacionesFinalizadas, 'Evaluaciones finalizadas', 'check')}
      ${cifra(t.estudiantesSinEvaluar, 'Estudiantes sin evaluar', 'alert', true)}
    </div>

    <div class="tarjeta" id="atencion-planificaciones">
      <h2>${icono('alert')} Planificaciones que esperan revisión hace más de ${p.diasAtencion} días</h2>
      ${p.planificacionesEsperando.length ? html`<div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>Planificación</th><th>Estudiante</th><th>Práctica</th><th>Espera a</th><th class="num">Días</th><th>Fecha de la clase</th></tr></thead>
        <tbody>${p.planificacionesEsperando.map((x) => html`<tr>
          <td><a href="#/planificaciones/${x.planificacionId}">${x.titulo}</a></td>
          <td>${x.estudiante}</td><td>${x.asignatura} <small>(${x.periodo})</small></td>
          <td>${x.esperaA ?? '—'}<br><small>${nombreRol(x.rolQueDebeActuar)}</small></td>
          <td class="num"><strong>${x.diasEsperando}</strong></td><td>${fecha(x.fechaClase)}</td></tr>`)}
        </tbody></table></div>` : html`<p class="subtitulo">Ninguna: todas las planificaciones pendientes llegaron hace ${p.diasAtencion} días o menos.</p>`}
    </div>

    <div class="tarjeta" id="atencion-sin-evaluar">
      <h2>${icono('alert')} Estudiantes en curso sin ninguna evaluación</h2>
      <p class="subtitulo">Solo prácticas que van a un colegio (las que evalúa el profesor guía con la pauta).</p>
      ${p.sinEvaluar.length ? html`<div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>Estudiante</th><th>Práctica</th><th>Profesor guía</th><th class="num">Borradores</th><th></th></tr></thead>
        <tbody>${p.sinEvaluar.map((x) => html`<tr>
          <td>${x.estudiante}</td><td>${x.asignatura} <small>(${x.periodo})</small></td><td>${x.profesorGuia ?? '—'}</td>
          <td class="num">${x.borradores || '—'}</td>
          <td><a class="btn btn-chico" href="#/historial/${x.estudianteId}">${icono('clock')} Historial</a></td></tr>`)}
        </tbody></table></div>` : html`<p class="subtitulo">Ninguno.</p>`}
    </div>

    <div class="tarjeta">
      <h2>Avance por práctica y período</h2>
      ${p.filas.length ? html`<div class="tabla-contenedor"><table class="tabla" id="tabla-avance">
        <thead><tr><th>Práctica</th><th>Período</th><th class="num">Inscritos</th><th class="num">En curso</th>
          <th class="num">Esperan al guía</th><th class="num">Esperan al profesor del curso</th>
          <th class="num">Devueltas</th><th class="num">Autorizadas</th>
          <th class="num">Evaluaciones finalizadas</th><th class="num">Borradores</th><th class="num">Promedio</th>
          <th class="num">Sin evaluar</th><th></th></tr></thead>
        <tbody>${p.filas.map((f) => html`<tr>
          <td>${f.orden}. ${f.asignatura}</td><td>${f.periodo}</td><td class="num">${f.inscritos}</td><td class="num">${f.enCurso}</td>
          <td class="num">${f.planificacionesEnRevisionGuia}</td><td class="num">${f.planificacionesEnAutorizacion}</td>
          <td class="num">${f.planificacionesConObservaciones}</td><td class="num">${f.planificacionesAutorizadas}</td>
          <td class="num">${f.evaluacionesFinalizadas}</td><td class="num">${f.evaluacionesBorrador}</td>
          <td class="num">${f.promedioEvaluaciones == null ? '—' : decimal(f.promedioEvaluaciones, 2)}</td>
          <td class="num">${f.estudiantesSinEvaluar === null ? html`<small>No aplica</small>` : f.estudiantesSinEvaluar}</td>
          <td><a class="btn btn-chico" href="#/informes?asignaturaId=${f.asignaturaId}&periodo=${f.periodo}">${icono('chart')} Informe</a></td></tr>`)}
        </tbody></table></div>` : html`<p class="subtitulo">No hay inscripciones con esos filtros.</p>`}
    </div>

    <div class="tarjeta" id="pendientes">
      <h2>${icono('file')} Todas las planificaciones pendientes</h2>
      <p class="subtitulo">Las que esperan al guía, al profesor del curso o, si fueron devueltas, al estudiante. La que lleva más tiempo esperando va primero; en rojo, las que esperan a un profesor hace más de ${p.diasAtencion} días.</p>
      ${p.pendientes.length ? html`<div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>Planificación</th><th>Estudiante</th><th>Práctica</th><th>Estado</th><th>Espera a</th><th class="num">Días</th><th>Fecha de la clase</th></tr></thead>
        <tbody>${p.pendientes.map((x) => html`<tr>
          <td><a href="#/planificaciones/${x.planificacionId}">${x.titulo}</a></td>
          <td>${x.estudiante}</td><td>${x.asignatura} <small>(${x.periodo})</small></td><td>${badge(x.estado)}</td>
          <td>${x.esperaA ?? '—'}<br><small>${nombreRol(x.rolQueDebeActuar)}</small></td>
          <td class="num">${x.requiereAtencion ? html`<strong style="color:#c0392b">${x.diasEsperando}</strong>` : x.diasEsperando}</td>
          <td>${fecha(x.fechaClase)}</td></tr>`)}
        </tbody></table></div>` : html`<p class="subtitulo">Ninguna.</p>`}
    </div>

    <div class="tarjeta" id="avance-estudiantes">
      <h2>${icono('users')} Avance por estudiante</h2>
      ${p.avance.length ? html`<div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>Estudiante</th><th>Práctica</th><th>Estado</th><th class="num">Planificaciones</th><th class="num">Autorizadas</th>
          <th class="num">Evaluaciones</th><th class="num">Promedio</th><th class="num">Nota final</th><th></th></tr></thead>
        <tbody>${p.avance.map((x) => html`<tr>
          <td>${x.estudiante}</td><td>${x.asignatura} <small>(${x.periodo})</small></td><td>${badge(x.estado)}</td>
          <td class="num">${x.planificaciones}</td><td class="num">${x.planificacionesAutorizadas}</td>
          <td class="num">${x.evaluacionesFinalizadas}</td>
          <td class="num">${x.promedioEvaluaciones == null ? '—' : decimal(x.promedioEvaluaciones, 2)}</td>
          <td class="num">${nota(x.notaFinal)}</td>
          <td><a class="btn btn-chico" href="#/historial/${x.estudianteId}">${icono('clock')} Historial</a></td></tr>`)}
        </tbody></table></div>` : html`<p class="subtitulo">No hay inscripciones con esos filtros.</p>`}
    </div>`.toString();
}
