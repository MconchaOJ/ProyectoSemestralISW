// HU-08 · Informe de una asignatura en un período (coordinador y jefe de carrera, solo lectura).
//   #/informes?asignaturaId=3&periodo=2024-2
// Usa GET /api/reportes/asignatura (para verlo en la página) y GET /api/reportes/asignatura/pdf (descarga).
// El informe por estudiante es el historial en PDF (HU-28): botón «Descargar PDF» en la pantalla de historial.
import { api, descargarArchivo } from '../api.js';
import { asignaturas } from '../datos.js';
import { alerta, alertaError, cabecera, cargando, conCarga, html, icono, nombreEstado, nota, promedio, toast, vacio } from '../ui.js';

export async function render(cont, ctx) {
  ctx.titulo('Informes por asignatura');
  const [catalogo, inscripciones] = await Promise.all([asignaturas(), api.get('/api/inscripciones')]);
  // Períodos con inscripciones en cada asignatura, del más reciente al más antiguo
  const periodos = (asignaturaId) => [...new Set(inscripciones.filter((i) => String(i.asignatura.id) === String(asignaturaId))
    .map((i) => i.periodo))].sort().reverse();

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-08', titulo: 'Informes por asignatura',
      subtitulo: 'Síntesis de una práctica en un período: notas finales, evaluaciones por pauta con su distribución, planificaciones y una fila por estudiante. El informe por estudiante es su historial en PDF.',
    })}
    <div class="tarjeta">
      <form id="form-informe" class="rejilla-form" novalidate>
        <div class="campo">
          <label for="f-asignatura">Práctica</label>
          <select id="f-asignatura" name="asignaturaId">
            ${catalogo.map((a) => html`<option value="${a.id}">${a.orden}. ${a.nombre}</option>`)}
          </select>
        </div>
        <div class="campo">
          <label for="f-periodo">Período</label>
          <select id="f-periodo" name="periodo"></select>
        </div>
        <div class="acciones-form" style="grid-column:1/-1">
          <button type="submit" class="btn btn-primario" data-cargando="Cargando…">${icono('chart')} Ver informe</button>
          <button type="button" class="btn" id="btn-pdf" data-cargando="Generando…">${icono('download')} Descargar PDF</button>
        </div>
      </form>
    </div>
    <div id="informe"></div>`.toString();

  const form = cont.querySelector('#form-informe');
  const selAsig = form.elements.asignaturaId;
  const selPer = form.elements.periodo;
  const salida = cont.querySelector('#informe');

  const llenarPeriodos = (elegido) => {
    const lista = periodos(selAsig.value);
    selPer.innerHTML = lista.length
      ? html`${lista.map((p) => html`<option value="${p}" ${p === elegido ? 'selected' : ''}>${p}</option>`)}`.toString()
      : '<option value="">Sin inscripciones</option>';
    selPer.disabled = lista.length === 0;
    cont.querySelector('#btn-pdf').disabled = lista.length === 0;
  };

  const ver = async () => {
    if (!selPer.value) {
      salida.innerHTML = vacio('Esta práctica todavía no tiene inscripciones', 'Elige otra práctica.', 'chart').toString();
      return;
    }
    history.replaceState(null, '', `#/informes?asignaturaId=${selAsig.value}&periodo=${selPer.value}`);
    salida.innerHTML = cargando().toString();
    try {
      pintar(salida, await api.get('/api/reportes/asignatura', { asignaturaId: selAsig.value, periodo: selPer.value }));
    } catch (e) {
      if (e.status !== 401) salida.innerHTML = alertaError(e).toString();
    }
  };

  selAsig.addEventListener('change', () => { llenarPeriodos(); ver(); });
  selPer.addEventListener('change', ver);
  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    await conCarga(ev.submitter ?? form.querySelector('button[type=submit]'), ver);
  });
  cont.querySelector('#btn-pdf').addEventListener('click', async (ev) => {
    try {
      const archivo = await conCarga(ev.currentTarget, () =>
        api.archivo(`/api/reportes/asignatura/pdf?asignaturaId=${selAsig.value}&periodo=${encodeURIComponent(selPer.value)}`));
      descargarArchivo({ ...archivo, nombre: archivo.nombre === 'archivo' ? 'informe-asignatura.pdf' : archivo.nombre });
      toast('Informe descargado.');
    } catch (e) {
      if (e.status !== 401) toast(e.message, 'error');
    }
  });

  // Lo que venga en la dirección (#/informes?asignaturaId=3&periodo=2024-2) o la primera práctica con inscripciones
  const pedida = ctx.consulta.get('asignaturaId');
  selAsig.value = pedida && catalogo.some((a) => String(a.id) === pedida) ? pedida
    : String((catalogo.find((a) => periodos(a.id).length) ?? catalogo[0]).id);
  llenarPeriodos(ctx.consulta.get('periodo'));
  await ver();
}

/** Tramos como barras horizontales: el ancho es la proporción sobre el total. */
function tramos(lista) {
  const total = lista.reduce((s, t) => s + t.cantidad, 0) || 1;
  return html`<table class="tabla"><thead><tr><th>Tramo</th><th style="width:55%">Distribución</th><th>Cantidad</th></tr></thead><tbody>
    ${lista.map((t) => html`<tr><td>${t.tramo}</td>
      <td><div style="background:#e8edf3;border-radius:4px;height:.8rem"><div style="background:#003b71;border-radius:4px;height:.8rem;width:${Math.round((t.cantidad / total) * 100)}%"></div></div></td>
      <td>${t.cantidad}</td></tr>`)}
  </tbody></table>`;
}

/**
 * Cantidad, promedio, mínimo y máximo, y la tabla de tramos (o un texto si no hay datos). `formato` es el de cada
 * valor: las notas finales con 1 decimal y los promedios de las evaluaciones con 2; el promedio siempre con 2.
 */
function estadistica(n, siVacio, formato = promedio) {
  if (!n.cantidad) return html`<p class="subtitulo">${siVacio}</p>`;
  return html`<dl class="datos datos-2">
      <div><dt>Cantidad</dt><dd>${n.cantidad}</dd></div>
      <div><dt>Promedio</dt><dd>${promedio(n.promedio)}</dd></div>
      <div><dt>Mínimo</dt><dd>${formato(n.minimo)}</dd></div>
      <div><dt>Máximo</dt><dd>${formato(n.maximo)}</dd></div>
    </dl>${tramos(n.tramos)}`;
}

const EVALUADOR = { COLABORADOR: 'Profesor/a colaborador/a', TUTOR: 'Tutor/a', ACADEMICO: 'Académico/a' };

function pintar(salida, inf) {
  const ins = inf.inscripciones;
  const pl = inf.planificaciones;
  salida.innerHTML = html`
    <div class="tarjeta">
      <div class="tarjeta-titulo"><h2>${inf.asignatura} · ${inf.periodo}</h2></div>
      <dl class="datos datos-2">
        <div><dt>Inscritos</dt><dd>${ins.total}</dd></div>
        <div><dt>En curso</dt><dd>${ins.enCurso}</dd></div>
        <div><dt>Aprobadas</dt><dd>${ins.aprobadas}</dd></div>
        <div><dt>Reprobadas</dt><dd>${ins.reprobadas}</dd></div>
      </dl>
      ${ins.total === 0 ? alerta('info', 'No hay estudiantes inscritos en este período.') : ''}
    </div>

    <div class="tarjeta">
      <h2>Notas finales</h2>
      ${estadistica(inf.notasFinales, 'Todavía no hay notas finales: las prácticas en curso no tienen nota.', nota)}
    </div>

    <div class="tarjeta">
      <h2>Evaluaciones finalizadas por pauta</h2>
      ${inf.evaluaciones.length ? '' : html`<p class="subtitulo">No hay evaluaciones finalizadas en este período.</p>`}
      ${inf.evaluaciones.map((p) => html`
        <div class="elemento">
          <h3>${p.pauta}${p.version > 1 ? ` (versión ${p.version})` : ''}</h3>
          <p class="subtitulo">${EVALUADOR[p.tipoEvaluador] ?? p.tipoEvaluador} · ${p.evaluaciones} ${p.evaluaciones === 1 ? 'evaluación' : 'evaluaciones'} ·
            puntaje promedio <strong>${promedio(p.puntajePromedio)} de ${p.puntajeMaximo}</strong> (logro ${nota(p.porcentajeLogro)} %)</p>
          <h4>Promedio de cada evaluación</h4>
          ${estadistica(p.promedios, 'Sin promedios.')}
          <h4>Puntaje promedio por indicador en cada dimensión</h4>
          <table class="tabla"><thead><tr><th>Dimensión</th><th>Promedio por indicador</th></tr></thead><tbody>
            ${p.dimensiones.map((d) => html`<tr><td>${d.dimension}</td><td>${promedio(d.promedioPorIndicador)}</td></tr>`)}
          </tbody></table>
        </div>`)}
    </div>

    <div class="tarjeta">
      <h2>Planificaciones</h2>
      <dl class="datos datos-2">
        <div><dt>Total</dt><dd>${pl.total}</dd></div>
        <div><dt>Autorizadas</dt><dd>${pl.autorizadas}</dd></div>
        <div><dt>En autorización</dt><dd>${pl.enAutorizacion}</dd></div>
        <div><dt>En revisión del guía</dt><dd>${pl.enRevisionGuia}</dd></div>
        <div><dt>Con observaciones</dt><dd>${pl.conObservaciones}</dd></div>
        <div><dt>Borrador</dt><dd>${pl.borrador}</dd></div>
        <div><dt>Devueltas al menos una vez</dt><dd>${pl.devueltasAlMenosUnaVez}</dd></div>
      </dl>
    </div>

    <div class="tarjeta">
      <h2>Estudiantes (${inf.estudiantes.length})</h2>
      ${inf.estudiantes.length ? html`<div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>Estudiante</th><th>RUT</th><th>Estado</th><th>Nota final</th><th>Evaluaciones</th><th>Promedio evaluaciones</th><th>Planificaciones (autorizadas)</th><th></th></tr></thead>
        <tbody>${inf.estudiantes.map((f) => html`<tr>
          <td>${f.estudiante}</td><td>${f.rut}</td><td>${nombreEstado(f.estado)}</td><td>${nota(f.notaFinal)}</td>
          <td>${f.evaluacionesFinalizadas}</td><td>${promedio(f.promedioEvaluaciones)}</td>
          <td>${f.planificaciones} (${f.planificacionesAutorizadas})</td>
          <td><a class="btn btn-chico" href="#/historial/${f.estudianteId}">${icono('clock')} Historial</a></td></tr>`)}
        </tbody></table></div>` : html`<p class="subtitulo">Sin estudiantes.</p>`}
    </div>`.toString();
}
