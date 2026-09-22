// HU-05 · Ver el historial del estudiante: recorrido por las prácticas con evaluaciones y planificaciones.
//   Estudiante: #/historial (el suyo)   ·   Guía, profesor del curso, coordinador: #/historial/{estudianteId}
import { api } from '../api.js';
import {
  MODALIDADES, alerta, alertaError, badge, cabecera, debounce, fecha, html, icono, iniciales, nota, promedio, vacio,
} from '../ui.js';

/** Sin distinguir mayúsculas ni tildes, para buscar como lo hace la base de datos. */
const normalizar = (t) => String(t ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

export async function render(cont, ctx) {
  const [id] = ctx.args;
  ctx.titulo('Historial');

  if (ctx.rol === 'ESTUDIANTE') {
    const h = await api.get('/api/estudiantes/me/historial');
    return pintar(cont, ctx, h, { propio: true });
  }
  if (id) {
    const h = await api.get(`/api/estudiantes/${id}/historial`);
    return pintar(cont, ctx, h, { propio: false });
  }
  return selector(cont, ctx);
}

/** Guía, profesor del curso y coordinador eligen a un estudiante. */
async function selector(cont, ctx) {
  const encabezado = cabecera({
    hu: 'HU-05', titulo: 'Ver el historial del estudiante',
    subtitulo: 'Elige a un estudiante para ver su recorrido completo por las prácticas y cómo evolucionan sus evaluaciones.',
  });
  let estudiantes;
  if (ctx.rol === 'COORDINADOR') {
    const lista = await api.get('/api/estudiantes');
    estudiantes = lista.map((e) => ({
      id: e.id, nombre: `${e.nombre} ${e.apellidos}`, detalle: `${e.rut} · semestre ${e.semestreActual}`,
      busqueda: normalizar(`${e.nombre} ${e.apellidos} ${e.rut} ${e.email}`),
    }));
  } else { // guía / profesor del curso: los estudiantes de sus inscripciones
    const inscripciones = await api.get('/api/inscripciones');
    const mapa = new Map();
    inscripciones.forEach((i) => {
      if (!mapa.has(i.estudiante.id)) {
        mapa.set(i.estudiante.id, {
          id: i.estudiante.id, nombre: i.estudiante.nombreCompleto, detalle: i.estudiante.rut,
          busqueda: normalizar(`${i.estudiante.nombreCompleto} ${i.estudiante.rut}`),
        });
      }
    });
    estudiantes = [...mapa.values()];
  }

  const listaVacia = estudiantes.length === 0;
  cont.innerHTML = html`${encabezado}
    ${listaVacia
      ? vacio('Todavía no tienes estudiantes asignados', 'Cuando el coordinador te asigne una inscripción, podrás ver el historial del estudiante.', 'users')
      : html`<label class="buscador">${icono('search')}<input type="search" id="buscar-estudiante"
              placeholder="Buscar por nombre, RUT o correo…" aria-label="Buscar estudiante"></label>
          <div class="tarjetas-estudiantes" id="lista-estudiantes"></div>`}`.toString();
  if (listaVacia) return;

  const buscador = cont.querySelector('#buscar-estudiante');
  const lista = cont.querySelector('#lista-estudiantes');
  const pintarLista = () => {
    const filtro = normalizar(buscador.value.trim());
    const visibles = filtro ? estudiantes.filter((e) => e.busqueda.includes(filtro)) : estudiantes;
    lista.innerHTML = visibles.length === 0
      ? vacio('Sin resultados', 'Prueba con otro nombre, RUT o correo.', 'search').toString()
      : visibles.map((e) => html`
          <a class="tarjeta-estudiante" href="#/historial/${e.id}">
            <span class="avatar-grande">${iniciales({ nombre: e.nombre.split(' ')[0], apellidos: e.nombre.split(' ')[1] ?? '' })}</span>
            <span><strong>${e.nombre}</strong><small>${e.detalle}</small></span>
            ${icono('clock')}
          </a>`).join('');
  };
  buscador.addEventListener('input', debounce(pintarLista, 150));
  pintarLista();
}

function pintar(cont, ctx, h, { propio }) {
  const e = h.estudiante;
  ctx.titulo(`Historial de ${e.nombre}`);
  const insc = [...h.inscripciones].sort((a, b) => a.periodo.localeCompare(b.periodo) || a.asignatura.orden - b.asignatura.orden);
  const total = insc.length;
  const aprobadas = h.asignaturasAprobadas.length;
  const enCurso = insc.filter((i) => i.estado === 'EN_CURSO').length;

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-05', titulo: propio ? 'Mi historial de prácticas' : `Historial de ${e.nombre} ${e.apellidos}`,
      subtitulo: 'Recorrido completo por las prácticas: cómo evoluciona a lo largo del tiempo y no solo una nota final.',
      acciones: propio ? '' : html`<a class="btn btn-fantasma" href="#/historial">${icono('back')} Cambiar estudiante</a>`,
    })}

    <div class="tarjeta perfil-historial">
      <span class="avatar-grande">${iniciales(e)}</span>
      <div class="perfil-datos">
        <h2>${e.nombre} ${e.apellidos}</h2>
        <p class="subtitulo">${e.email} · RUT ${e.rut}</p>
      </div>
      <dl class="cifras">
        <div><dt>Semestre actual</dt><dd>${e.semestreActual}</dd></div>
        <div><dt>Ingreso</dt><dd>${e.anioIngreso}</dd></div>
        <div><dt>Prácticas aprobadas</dt><dd>${aprobadas} de 6</dd></div>
        <div><dt>En curso</dt><dd>${enCurso}</dd></div>
      </dl>
    </div>

    ${aprobadas > 0 ? html`<div class="tarjeta">
      <h2>Asignaturas aprobadas <small class="subtitulo">(contexto que usará el chatbot en un sprint futuro)</small></h2>
      <ul class="chips">${h.asignaturasAprobadas.map((a) => html`<li class="chip"><strong>${a.nombre}</strong> <span>${a.periodo} · nota ${nota(a.notaFinal)}</span></li>`)}</ul>
    </div>` : ''}

    <h2 class="titulo-linea">Recorrido por período</h2>
    ${total === 0 ? vacio('Este estudiante aún no tiene inscripciones', '', 'clipboard') : html`<ol class="recorrido">${insc.map((i) => practica(i, propio))}</ol>`}`.toString();
}

function practica(i, propio) {
  const evals = [...i.evaluaciones].sort((a, b) => String(a.fechaClase).localeCompare(String(b.fechaClase)));
  return html`<li class="practica estado-${i.estado}">
    <span class="practica-marca">${i.asignatura.orden}</span>
    <div class="tarjeta">
      <div class="tarjeta-titulo">
        <div>
          <h3>${i.asignatura.nombre}</h3>
          <small class="subtitulo">${i.periodo} · ${MODALIDADES[i.asignatura.modalidad]} · ${i.asignatura.requiereCentroEducativo ? 'en un colegio' : 'sin colegio'}</small>
        </div>
        <div class="practica-estado">${badge(i.estado)} ${i.notaFinal !== null && i.notaFinal !== undefined ? html`<strong class="nota-final">${nota(i.notaFinal)}</strong>` : ''}</div>
      </div>
      <p class="equipo">
        <span>Profesor guía: <strong>${i.profesorGuia ? i.profesorGuia.nombreCompleto : 'no lleva'}</strong></span>
        <span>Profesor del curso: <strong>${i.profesorCurso?.nombreCompleto ?? '—'}</strong></span>
      </p>

      ${evals.length > 0 ? html`
        <h4>Evaluaciones</h4>
        <ul class="evals">${evals.map((ev) => html`<li>
          <a href="#/evaluaciones/${ev.id}">${fecha(ev.fechaClase)}</a>
          <span class="ev-lugar">${ev.establecimiento} · ${ev.curso}</span>
          <span class="barra-prom mini" role="img" aria-label="Promedio ${promedio(ev.promedio)}"><span style="width:${Math.max(0, Math.min(100, ((Number(ev.promedio) - 1) / 6) * 100))}%"></span></span>
          <strong class="ev-prom">${promedio(ev.promedio)}</strong>
        </li>`)}</ul>` : ''}

      ${i.planificaciones.length > 0 ? html`
        <h4>Planificaciones</h4>
        <ul class="planes">${i.planificaciones.map((p) => html`<li>
          <a href="#/planificaciones/${p.id}">${p.titulo}</a>
          <small>${fecha(p.fechaClase)}</small>
          ${badge(p.estado)}
        </li>`)}</ul>` : ''}

      ${evals.length === 0 && i.planificaciones.length === 0
        ? html`<p class="subtitulo">${i.asignatura.requierePlanificacion ? 'Sin evaluaciones ni planificaciones registradas.' : 'Esta práctica no entrega planificaciones.'}</p>` : ''}
    </div>
  </li>`;
}
