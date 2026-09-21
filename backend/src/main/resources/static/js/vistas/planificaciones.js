// HU-04 · Planificar una clase y que la aprueben en dos etapas.
//   #/planificaciones                lista con filtros (cada rol ve lo suyo)
//   #/planificaciones/nueva          borrador nuevo (estudiante)
//   #/planificaciones/{id}           detalle, flujo y acciones según rol y estado
//   #/planificaciones/{id}/editar    editar un borrador o una planificación con observaciones (estudiante)
import { api } from '../api.js';
import { opcionesInscripcion } from '../datos.js';
import {
  alerta, alertaError, alertaForm, badge, cabecera, cajaAlertaForm, campo, cargando, confirmar, conCarga, datosForm,
  fecha, fechaHora, html, icono, limpiarAlertaForm, nombreEstado, nombreRol, toast, validarForm, vacio,
} from '../ui.js';

/** Quién debe actuar según el estado (igual que el campo pendienteDe del backend). */
const PENDIENTE = {
  BORRADOR: 'ESTUDIANTE', CON_OBSERVACIONES: 'ESTUDIANTE', EN_REVISION_GUIA: 'PROFESOR_GUIA',
  EN_AUTORIZACION: 'PROFESOR_CURSO', AUTORIZADA: null,
};
const ETAPAS = { GUIA: 'Revisión del profesor guía', CURSO: 'Autorización del profesor del curso' };

export async function render(cont, ctx) {
  const [a, b] = ctx.args;
  if (!a) return lista(cont, ctx);
  if (a === 'nueva') return formulario(cont, ctx, null);
  if (b === 'editar') return formulario(cont, ctx, a);
  return detalle(cont, ctx, a);
}

// ============================ LISTA ============================
async function lista(cont, ctx) {
  ctx.titulo('Planificaciones');
  const esEstudiante = ctx.rol === 'ESTUDIANTE';
  const inscripciones = await api.get('/api/inscripciones');

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-04', titulo: 'Planificar una clase y que la aprueben en dos etapas',
      subtitulo: 'Estudiante → profesor guía → profesor del curso. Quien devuelve una planificación debe dejar un comentario y el flujo vuelve a empezar desde el guía.',
      acciones: esEstudiante ? html`<a class="btn btn-primario" href="#/planificaciones/nueva">${icono('plus')} Nueva planificación</a>` : '',
    })}
    <div class="flujo-mini" aria-label="Flujo de aprobación">
      <span>Borrador</span><i>›</i><span>Revisión del guía</span><i>›</i><span>Autorización del profesor del curso</span><i>›</i><span class="fin">Autorizada</span>
      <small>En el ciclo inicial (sin guía) pasa directo a autorización.</small>
    </div>
    <div class="barra-herramientas">
      <label class="filtro">Estado
        <select id="f-estado"><option value="">Todos</option>
          ${Object.keys(PENDIENTE).map((e) => html`<option value="${e}" ${ctx.consulta.get('estado') === e ? html`selected` : ''}>${nombreEstado(e)}</option>`)}</select></label>
      <label class="filtro">Inscripción
        <select id="f-inscripcion"><option value="">Todas</option>${opcionesInscripcion(inscripciones).map((o) => html`<option value="${o.valor}" ${ctx.consulta.get('inscripcionId') === String(o.valor) ? html`selected` : ''}>${o.texto}</option>`)}</select></label>
    </div>
    <div id="lista"></div>`.toString();

  const zona = cont.querySelector('#lista');
  const fEstado = cont.querySelector('#f-estado');
  const fInsc = cont.querySelector('#f-inscripcion');
  let pedido = 0;

  async function cargar() {
    const mio = ++pedido;
    zona.innerHTML = cargando('Cargando planificaciones…').toString();
    try {
      const datos = await api.get('/api/planificaciones', { estado: fEstado.value, inscripcionId: fInsc.value });
      if (mio !== pedido) return;
      // Primero lo que espera tu acción, luego lo más reciente
      datos.sort((x, y) => (Number(PENDIENTE[y.estado] === ctx.rol) - Number(PENDIENTE[x.estado] === ctx.rol))
        || String(y.fechaActualizacion).localeCompare(String(x.fechaActualizacion)));
      zona.innerHTML = datos.length === 0
        ? vacio('No hay planificaciones para mostrar',
          esEstudiante ? 'Crea tu primer borrador con "Nueva planificación".' : 'Cuando un estudiante envíe una planificación a tu revisión, aparecerá aquí.', 'file').toString()
        : html`<div class="tabla-contenedor"><table class="tabla">
            <thead><tr><th>Planificación</th><th>Estudiante</th><th>Fecha de la clase</th><th>Estado</th><th>Pendiente de</th><th>Actualizada</th><th></th></tr></thead>
            <tbody>${datos.map((p) => {
              const pendiente = PENDIENTE[p.estado];
              return html`<tr>
                <td><strong>${p.titulo}</strong></td>
                <td>${p.estudiante}</td>
                <td>${fecha(p.fechaClase)}</td>
                <td>${badge(p.estado)}</td>
                <td>${pendiente ? nombreRol(pendiente) : html`<small>Nadie: ya está autorizada</small>`}
                  ${pendiente === ctx.rol ? html` <span class="badge badge-dorado">Requiere tu acción</span>` : ''}</td>
                <td><small>${fechaHora(p.fechaActualizacion)}</small></td>
                <td class="acciones"><a class="btn btn-chico" href="#/planificaciones/${p.id}">Abrir</a></td>
              </tr>`;
            })}</tbody></table></div>
          <p class="pie-tabla">${datos.length} planificaci${datos.length === 1 ? 'ón' : 'ones'}</p>`.toString();
    } catch (e) {
      if (mio === pedido) zona.innerHTML = alertaError(e).toString();
    }
  }
  [fEstado, fInsc].forEach((el) => el.addEventListener('change', cargar));
  await cargar();
}

// ============================ FORMULARIO (crear / editar) ============================
async function formulario(cont, ctx, idEdicion) {
  const editando = idEdicion !== null;
  ctx.titulo(editando ? 'Editar planificación' : 'Nueva planificación');
  const volver = html`<a class="btn btn-fantasma" href="${editando ? `#/planificaciones/${idEdicion}` : '#/planificaciones'}">${icono('back')} Volver</a>`;

  if (ctx.rol !== 'ESTUDIANTE') {
    cont.innerHTML = html`${cabecera({ hu: 'HU-04', titulo: 'Planificación', acciones: volver })}
      ${alerta('error', html`<strong>No tienes permiso para esta acción.</strong> Solo el estudiante dueño de la inscripción escribe su planificación.`)}`.toString();
    return;
  }

  let actual = null;
  let inscripciones = [];
  if (editando) {
    actual = await api.get(`/api/planificaciones/${idEdicion}`);
    if (!['BORRADOR', 'CON_OBSERVACIONES'].includes(actual.estado)) {
      cont.innerHTML = html`${cabecera({ hu: 'HU-04', titulo: actual.titulo, acciones: volver })}
        ${alerta('aviso', html`Esta planificación está <strong>${nombreEstado(actual.estado)}</strong>: solo se puede editar en borrador o con observaciones.`)}`.toString();
      return;
    }
  } else {
    inscripciones = (await api.get('/api/inscripciones')).filter((i) => i.estado === 'EN_CURSO' && i.asignatura.requierePlanificacion);
    if (inscripciones.length === 0) {
      cont.innerHTML = html`${cabecera({ hu: 'HU-04', titulo: 'Nueva planificación', acciones: volver })}
        ${vacio('No tienes una práctica en curso que pida planificaciones',
          'Solo se planifica en una inscripción EN_CURSO. Aproximación al Sistema Escolar, por ejemplo, no entrega planificaciones.', 'file')}`.toString();
      return;
    }
  }

  const v = actual ?? {};
  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-04', titulo: editando ? 'Editar planificación' : 'Nueva planificación',
      subtitulo: 'Un borrador puede estar incompleto; para enviarlo a revisión deben estar los cinco elementos del formato y la clase debe ser dentro de al menos 3 días.',
      acciones: volver,
    })}
    ${editando && actual.estado === 'CON_OBSERVACIONES' ? ultimaObservacion(actual) : ''}
    <form class="tarjeta" id="form" novalidate>
      ${cajaAlertaForm()}
      <div class="rejilla-form">
        ${editando ? '' : campo({ nombre: 'inscripcionId', etiqueta: 'Inscripción', tipo: 'select', requerido: true, completo: true,
          atributos: { 'data-numero': true }, opciones: opcionesInscripcion(inscripciones), valor: ctx.consulta.get('inscripcionId') ?? (inscripciones.length === 1 ? inscripciones[0].id : '') })}
        ${campo({ nombre: 'titulo', etiqueta: 'Título de la clase', requerido: true, completo: true, valor: v.titulo ?? '', atributos: { maxlength: 200 } })}
        ${campo({ nombre: 'fechaClase', etiqueta: 'Fecha de la clase', tipo: 'date', valor: v.fechaClase ?? '' })}
        ${campo({ nombre: 'duracionMinutos', etiqueta: 'Duración (minutos)', tipo: 'number', valor: v.duracionMinutos ?? '', atributos: { min: 1, max: 600, placeholder: '90' } })}
        ${campo({ nombre: 'objetivo', etiqueta: '1. Objetivo de la clase', tipo: 'textarea', completo: true, valor: v.objetivo ?? '', atributos: { maxlength: 5000 } })}
        ${campo({ nombre: 'recursos', etiqueta: '2. Recursos', tipo: 'textarea', completo: true, valor: v.recursos ?? '', ayuda: 'Materiales, guías, tecnología que se usará.', atributos: { maxlength: 5000 } })}
        ${campo({ nombre: 'tiempos', etiqueta: '3. Tiempos', tipo: 'textarea', completo: true, valor: v.tiempos ?? '', ayuda: 'Distribución de la clase: inicio, desarrollo y cierre.', atributos: { maxlength: 5000 } })}
        ${campo({ nombre: 'evaluacionAprendizaje', etiqueta: '4. Evaluación del aprendizaje', tipo: 'textarea', completo: true, valor: v.evaluacionAprendizaje ?? '', atributos: { maxlength: 5000 } })}
        ${campo({ nombre: 'interaccionParticipantes', etiqueta: '5. Interacción de los participantes con los recursos', tipo: 'textarea', completo: true, valor: v.interaccionParticipantes ?? '', atributos: { maxlength: 5000 } })}
      </div>
      <div class="acciones-form">
        <a class="btn" href="${editando ? `#/planificaciones/${idEdicion}` : '#/planificaciones'}">Cancelar</a>
        <button type="submit" class="btn" data-accion="guardar" data-cargando="Guardando…">Guardar borrador</button>
        <button type="submit" class="btn btn-primario" data-accion="enviar" data-cargando="Enviando…">${icono('send')} Guardar y enviar a revisión</button>
      </div>
    </form>`.toString();

  const form = cont.querySelector('#form');
  let idActual = editando ? Number(idEdicion) : null; // tras crear el borrador, los siguientes guardados son PUT

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const boton = ev.submitter ?? form.querySelector('[data-accion="guardar"]');
    limpiarAlertaForm(form);
    if (!validarForm(form)) return;
    const datos = datosForm(form);
    let guardada = false;
    try {
      await conCarga(boton, async () => {
        const resp = idActual
          ? await api.put(`/api/planificaciones/${idActual}`, datos)
          : await api.post('/api/planificaciones', datos);
        idActual = resp.id;
        guardada = true;
        if (boton.dataset.accion === 'enviar') await api.post(`/api/planificaciones/${idActual}/enviar`);
      });
      toast(boton.dataset.accion === 'enviar' ? 'Planificación enviada a revisión.' : 'Borrador guardado.');
      ctx.ir(`#/planificaciones/${idActual}`);
    } catch (e) {
      if (e.status === 401) return;
      alertaForm(form, e);
      if (guardada) toast('El borrador se guardó, pero no se pudo enviar. Corrige y vuelve a intentarlo.', 'error');
    }
  });
}

function ultimaObservacion(p) {
  const ult = [...p.revisiones].reverse().find((r) => r.decision === 'OBSERVADA');
  if (!ult) return '';
  return alerta('aviso', html`<strong>${ETAPAS[ult.etapa]} dejó observaciones</strong> (${ult.revisor.nombreCompleto}, ${fechaHora(ult.fecha)}):<br><em>“${ult.comentario}”</em>`);
}

// ============================ DETALLE ============================
async function detalle(cont, ctx, id) {
  ctx.titulo('Planificación');
  const p = await api.get(`/api/planificaciones/${id}`);
  const insc = await api.get(`/api/inscripciones/${p.inscripcionId}`).catch(() => null);
  const sinGuia = insc ? !insc.profesorGuia : false;

  const pasos = [
    { clave: 'BORRADOR', texto: 'Borrador del estudiante' },
    ...(sinGuia ? [] : [{ clave: 'EN_REVISION_GUIA', texto: 'Revisión del profesor guía' }]),
    { clave: 'EN_AUTORIZACION', texto: 'Autorización del profesor del curso' },
    { clave: 'AUTORIZADA', texto: 'Autorizada' },
  ];
  const posicion = { BORRADOR: 'BORRADOR', CON_OBSERVACIONES: 'BORRADOR' }[p.estado] ?? p.estado;
  const indice = pasos.findIndex((s) => s.clave === posicion);

  const bloque = (titulo, texto) => html`<div class="elemento"><h3>${titulo}</h3>${texto ? html`<p>${texto}</p>` : html`<p class="falta-texto">Sin completar.</p>`}</div>`;

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-04', titulo: p.titulo,
      subtitulo: html`${p.estudiante.nombreCompleto} · ${p.asignatura} (${p.periodo})`,
      acciones: html`<a class="btn btn-fantasma" href="#/planificaciones">${icono('back')} Volver</a>`,
    })}
    <div id="avisos"></div>
    ${p.estado === 'CON_OBSERVACIONES' ? ultimaObservacion(p) : ''}
    <div class="rejilla-detalle">
      <div>
        <div class="tarjeta">
          <div class="tarjeta-titulo"><h2>Planificación de la clase ${badge(p.estado)}</h2></div>
          <dl class="datos datos-2">
            <div><dt>Fecha de la clase</dt><dd>${fecha(p.fechaClase)}</dd></div>
            <div><dt>Duración</dt><dd>${p.duracionMinutos ? `${p.duracionMinutos} minutos` : '—'}</dd></div>
            <div><dt>Creada</dt><dd>${fechaHora(p.fechaCreacion)}</dd></div>
            <div><dt>Última actualización</dt><dd>${fechaHora(p.fechaActualizacion)}</dd></div>
          </dl>
          ${bloque('1. Objetivo de la clase', p.objetivo)}
          ${bloque('2. Recursos', p.recursos)}
          ${bloque('3. Tiempos', p.tiempos)}
          ${bloque('4. Evaluación del aprendizaje', p.evaluacionAprendizaje)}
          ${bloque('5. Interacción de los participantes con los recursos', p.interaccionParticipantes)}
        </div>
      </div>

      <aside>
        <div class="tarjeta">
          <h2>Estado del flujo</h2>
          <ol class="pasos">
            ${pasos.map((s, i) => html`<li class="${i < indice || p.estado === 'AUTORIZADA' ? 'hecho' : i === indice ? (p.estado === 'CON_OBSERVACIONES' ? 'observado' : 'actual') : ''}">
              <span class="paso-marca">${i < indice || p.estado === 'AUTORIZADA' ? '✓' : i + 1}</span><span>${s.texto}</span></li>`)}
          </ol>
          ${sinGuia ? html`<p class="subtitulo">Esta práctica no lleva profesor guía: la envía directo al profesor del curso.</p>` : ''}
          <p class="pendiente">${PENDIENTE[p.estado]
            ? html`Pendiente de: <strong>${nombreRol(PENDIENTE[p.estado])}</strong>`
            : html`<strong>Autorizada:</strong> la clase se puede realizar.`}</p>
          <div id="acciones"></div>
        </div>

        <div class="tarjeta">
          <h2>Historial de revisiones</h2>
          ${p.revisiones.length === 0
            ? html`<p class="subtitulo">Todavía no hay decisiones registradas.</p>`
            : html`<ul class="revisiones">${p.revisiones.map((r) => html`<li>
                <div class="rev-cab"><strong>${ETAPAS[r.etapa]}</strong> <span class="badge badge-${r.decision === 'APROBADA' ? 'AUTORIZADA' : 'CON_OBSERVACIONES'}" title="${r.decision}">${r.decision === 'APROBADA' ? 'Aprobada' : 'Observada'}</span></div>
                <small>${r.revisor.nombreCompleto} · ${fechaHora(r.fecha)}</small>
                ${r.comentario ? html`<p class="rev-comentario">“${r.comentario}”</p>` : ''}
              </li>`)}</ul>`}
        </div>
      </aside>
    </div>`.toString();

  pintarAcciones(cont, ctx, p);
}

function pintarAcciones(cont, ctx, p) {
  const zona = cont.querySelector('#acciones');
  const avisos = cont.querySelector('#avisos');
  const recargar = () => ctx.recargar();

  // ----- Estudiante: editar y enviar -----
  if (ctx.rol === 'ESTUDIANTE' && ['BORRADOR', 'CON_OBSERVACIONES'].includes(p.estado)) {
    zona.innerHTML = html`<div class="acciones-panel">
      <a class="btn" href="#/planificaciones/${p.id}/editar">${icono('edit')} Editar</a>
      <button type="button" class="btn btn-primario" id="enviar" data-cargando="Enviando…">${icono('send')} Enviar a revisión</button>
    </div>`.toString();
    zona.querySelector('#enviar').addEventListener('click', async (e) => {
      const boton = e.currentTarget; // currentTarget se pierde después de un await
      avisos.innerHTML = '';
      const ok = await confirmar({ titulo: 'Enviar a revisión', mensaje: 'La planificación pasará a revisión y no podrás editarla hasta que te respondan. ¿Enviar?', textoOk: 'Enviar' });
      if (!ok) return;
      try {
        await conCarga(boton, () => api.post(`/api/planificaciones/${p.id}/enviar`));
        toast('Planificación enviada a revisión.');
        recargar();
      } catch (err) { if (err.status !== 401) avisos.innerHTML = alertaError(err).toString(); }
    });
    return;
  }

  // ----- Guía o profesor del curso: decidir -----
  const decide = (ctx.rol === 'PROFESOR_GUIA' && p.estado === 'EN_REVISION_GUIA')
    || (ctx.rol === 'PROFESOR_CURSO' && p.estado === 'EN_AUTORIZACION');
  if (decide) {
    const esGuia = ctx.rol === 'PROFESOR_GUIA';
    const ruta = esGuia ? 'revision-guia' : 'revision-curso';
    zona.innerHTML = html`<form id="decision" novalidate>
      <div class="campo">
        <label for="comentario">Comentario</label>
        <textarea id="comentario" name="comentario" maxlength="5000" placeholder="Obligatorio si devuelves con observaciones"></textarea>
        <span class="error-campo" id="error-comentario" role="alert" hidden></span>
      </div>
      <div class="acciones-panel">
        <button type="button" class="btn btn-ok" data-decision="APROBADA" data-cargando="Guardando…">${icono('check-circle')} ${esGuia ? 'Aprobar y enviar al profesor del curso' : 'Autorizar planificación'}</button>
        <button type="button" class="btn btn-peligro" data-decision="OBSERVADA" data-cargando="Guardando…">${icono('x')} Devolver con observaciones</button>
      </div>
    </form>`.toString();

    const area = zona.querySelector('#comentario');
    const error = zona.querySelector('#error-comentario');
    area.addEventListener('input', () => { error.hidden = true; area.classList.remove('invalido'); });

    zona.querySelectorAll('[data-decision]').forEach((boton) => boton.addEventListener('click', async () => {
      avisos.innerHTML = '';
      const decision = boton.dataset.decision;
      const comentario = area.value.trim();
      if (decision === 'OBSERVADA' && !comentario) {
        error.textContent = 'Al devolver con observaciones el comentario es obligatorio.';
        error.hidden = false;
        area.classList.add('invalido');
        area.focus();
        return;
      }
      const ok = await confirmar({
        titulo: decision === 'APROBADA' ? (esGuia ? 'Aprobar planificación' : 'Autorizar planificación') : 'Devolver con observaciones',
        mensaje: decision === 'APROBADA'
          ? (esGuia ? 'Pasará a autorización del profesor del curso.' : 'La clase quedará autorizada.')
          : 'Volverá al estudiante, que deberá corregirla y reenviarla desde el guía.',
        textoOk: decision === 'APROBADA' ? 'Confirmar' : 'Devolver', peligro: decision === 'OBSERVADA',
      });
      if (!ok) return;
      try {
        await conCarga(boton, () => api.post(`/api/planificaciones/${p.id}/${ruta}`, { decision, ...(comentario ? { comentario } : {}) }));
        toast(decision === 'APROBADA' ? (esGuia ? 'Aprobada: pasó al profesor del curso.' : 'Planificación autorizada.') : 'Planificación devuelta con observaciones.');
        recargar();
      } catch (err) { if (err.status !== 401) avisos.innerHTML = alertaError(err).toString(); }
    }));
    return;
  }

  // ----- Sin acciones -----
  if (ctx.rol === 'PROFESOR_GUIA' && p.estado === 'AUTORIZADA') {
    zona.innerHTML = html`<div class="acciones-panel"><a class="btn btn-primario" href="#/evaluaciones/nueva?inscripcionId=${p.inscripcionId}">${icono('check')} Evaluar esta clase</a></div>`.toString();
    return;
  }
  zona.innerHTML = html`<p class="subtitulo">${PENDIENTE[p.estado] && PENDIENTE[p.estado] !== ctx.rol
    ? `No tienes acciones pendientes: espera al ${nombreRol(PENDIENTE[p.estado]).toLowerCase()}.`
    : 'No tienes acciones disponibles en este estado.'}</p>`.toString();
}
