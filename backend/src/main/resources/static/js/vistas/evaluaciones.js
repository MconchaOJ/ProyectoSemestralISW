// HU-03 · Evaluar una clase con la pauta digital (indicadores 1 a 7).
//   #/evaluaciones            lista con filtros
//   #/evaluaciones/nueva      crear la cabecera (solo profesor guía)
//   #/evaluaciones/{id}       detalle: el guía autor completa, guarda y finaliza; los demás roles solo lo ven
import { api } from '../api.js';
import { opcionesInscripcion } from '../datos.js';
import { montarDocumentos } from '../documentos.js';
import { montarNotaVoz } from '../notaVoz.js';
import {
  alerta, alertaError, badge, cabecera, campo, cargando, cajaAlertaForm, confirmar, conCarga, decimal,
  enlazarFormulario, fecha, fechaHora, hora, html, icono, modalFormulario, promedio, toast, vacio,
} from '../ui.js';

/** HU-07: evalúa el guía (pautas de COLABORADOR) o el tutor (pautas de TUTOR), cada uno en sus inscripciones. */
const evaluaEn = (ctx, i) => ctx.actuaComo('PROFESOR_GUIA', i) || ctx.actuaComo('TUTOR', i);
const TIPO_DE_PAUTA = (ctx, i) => (ctx.actuaComo('TUTOR', i) ? 'TUTOR' : 'COLABORADOR');
// Se agrega al nombre de la pauta solo si el nombre no lo dice ya ("Pauta de Especialidad, 1.ª observación - ...")
const conObservacion = (pauta) => (pauta?.numeroObservacion && !/observaci[oó]n/i.test(pauta.nombre ?? '')
  ? ` · ${pauta.numeroObservacion}.ª observación` : '');

export async function render(cont, ctx) {
  const [id] = ctx.args;
  if (!id) return lista(cont, ctx);
  if (id === 'nueva') return nueva(cont, ctx);
  return detalle(cont, ctx, id);
}

// ============================ LISTA ============================
async function lista(cont, ctx) {
  const esGuia = ctx.tiene('PROFESOR_GUIA') || ctx.tiene('TUTOR');   // quien evalúa (guía o tutor)
  const esEstudiante = ctx.tiene('ESTUDIANTE');
  // Cuenta con dos roles (jefe de carrera que también es guía): título neutro y un subtítulo que explica ambos.
  const guiaQueVeTodo = esGuia && ctx.veTodo;
  const titulo = guiaQueVeTodo ? 'Evaluaciones de clases'
    : ctx.tiene('TUTOR') ? 'Evaluar una clase con las pautas de tutor'
    : esGuia ? 'Evaluar una clase con la pauta digital (indicadores 1 a 7)'
      : esEstudiante ? 'Mis evaluaciones' : 'Evaluaciones de clases';
  ctx.titulo('Evaluaciones');
  const inscripciones = await api.get('/api/inscripciones');
  const porId = new Map(inscripciones.map((i) => [i.id, i]));
  const subtitulo = guiaQueVeTodo
    ? 'Ves las evaluaciones de todos los estudiantes, incluidos los borradores, en modo lectura. Como profesor guía, en las inscripciones que tienes asignadas creas y completas la pauta de 20 indicadores (escala 1 a 7).'
    : ctx.tiene('TUTOR') ? 'Observa la clase y completa la pauta de tutor (Especialidad u Orientación, escala 2 a 7) de los estudiantes de los que eres tutor. El borrador es privado; al finalizar se calcula el puntaje y ya no se puede modificar.'
    : esGuia ? 'Observa la clase y completa la pauta de 20 indicadores (escala 1 a 7). El borrador es privado; al finalizar se calcula el puntaje y ya no se puede modificar.'
      : ctx.veTodo ? 'Ves las evaluaciones de todos los estudiantes, incluidos los borradores. Solo lectura.'
        : esEstudiante ? 'Ves tus evaluaciones finalizadas: mientras el profesor guía la completa, la evaluación es un borrador privado y no aparece aquí.'
          : 'Solo ves las evaluaciones finalizadas de los estudiantes que tienes asociados.';

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-03', titulo, subtitulo,
      acciones: esGuia ? html`<a class="btn btn-primario" href="#/evaluaciones/nueva">${icono('plus')} Nueva evaluación</a>` : '',
    })}
    <div class="barra-herramientas">
      <label class="filtro" ${esEstudiante ? html`hidden` : ''}>Estado
        <select id="f-estado"><option value="">Todos</option><option value="BORRADOR">Borrador</option><option value="FINALIZADA">Finalizada</option></select></label>
      <label class="filtro">Inscripción
        <select id="f-inscripcion"><option value="">Todas</option>${opcionesInscripcion(inscripciones).map((o) => html`<option value="${o.valor}">${o.texto}</option>`)}</select></label>
    </div>
    <div id="lista"></div>`.toString();

  const zona = cont.querySelector('#lista');
  const fEstado = cont.querySelector('#f-estado');
  const fInsc = cont.querySelector('#f-inscripcion');
  let pedido = 0;

  async function cargar() {
    const mio = ++pedido;
    zona.innerHTML = cargando('Cargando evaluaciones…').toString();
    try {
      const datos = await api.get('/api/evaluaciones', { estado: fEstado.value, inscripcionId: fInsc.value });
      if (mio !== pedido) return;
      zona.innerHTML = datos.length === 0
        ? vacio('No hay evaluaciones para mostrar', esGuia ? 'Crea la primera con "Nueva evaluación".' : 'Cuando un profesor guía finalice una evaluación, la verás aquí.', 'check').toString()
        : html`<div class="tabla-contenedor"><table class="tabla">
            <thead><tr><th>Estudiante</th><th>Práctica</th><th>Fecha de la clase</th><th>Establecimiento</th><th>Estado</th><th class="num">Puntaje</th><th class="num">Promedio</th><th></th></tr></thead>
            <tbody>${datos.map((e) => {
              const insc = porId.get(e.inscripcionId);
              return html`<tr>
                <td><strong>${insc?.estudiante.nombreCompleto ?? `Inscripción ${e.inscripcionId}`}</strong></td>
                <td>${insc?.asignatura.nombre ?? '—'}<br><small>${insc?.periodo ?? ''}</small></td>
                <td>${fecha(e.fechaClase)}</td>
                <td>${e.establecimiento}<br><small>${e.curso}</small></td>
                <td>${badge(e.estado)}</td>
                <td class="num">${e.puntajeTotal ?? '—'}</td>
                <td class="num"><strong>${promedio(e.promedio)}</strong></td>
                <td class="acciones"><a class="btn btn-chico" href="#/evaluaciones/${e.id}">${e.estado === 'BORRADOR' && evaluaEn(ctx, insc) ? 'Continuar' : 'Ver'}</a></td>
              </tr>`;
            })}</tbody></table></div>
          <p class="pie-tabla">${datos.length} evaluaci${datos.length === 1 ? 'ón' : 'ones'}</p>`.toString();
    } catch (e) {
      if (mio === pedido) zona.innerHTML = alertaError(e).toString();
    }
  }
  [fEstado, fInsc].forEach((el) => el.addEventListener('change', cargar));
  await cargar();
}

// ============================ FORMULARIO DE CABECERA ============================
function camposCabecera(v = {}, { planificaciones = [] } = {}) {
  return [
    campo({ nombre: 'planificacionId', etiqueta: 'Planificación de la clase (opcional)', tipo: 'select', completo: true, valor: v.planificacionId ?? '',
      atributos: { 'data-numero': true }, vacioTexto: 'Sin planificación vinculada',
      ayuda: 'Solo aparecen las planificaciones AUTORIZADAS de esta inscripción.',
      opciones: planificaciones.map((p) => ({ valor: p.id, texto: `${p.titulo} (${fecha(p.fechaClase)})` })) }),
    campo({ nombre: 'fechaClase', etiqueta: 'Fecha de la clase', tipo: 'date', requerido: true, valor: v.fechaClase ?? '' }),
    campo({ nombre: 'curso', etiqueta: 'Curso', requerido: true, valor: v.curso ?? '', atributos: { maxlength: 50, placeholder: '1° Medio B' } }),
    campo({ nombre: 'establecimiento', etiqueta: 'Establecimiento', requerido: true, completo: true, valor: v.establecimiento ?? '', atributos: { maxlength: 150 } }),
    campo({ nombre: 'horaInicio', etiqueta: 'Hora de inicio', tipo: 'time', requerido: true, valor: v.horaInicio ? hora(v.horaInicio) : '' }),
    campo({ nombre: 'horaTermino', etiqueta: 'Hora de término', tipo: 'time', requerido: true, valor: v.horaTermino ? hora(v.horaTermino) : '' }),
    campo({ nombre: 'objetivoClase', etiqueta: 'Objetivo de la clase', tipo: 'textarea', requerido: true, completo: true, valor: v.objetivoClase ?? '', atributos: { maxlength: 5000 } }),
    campo({ nombre: 'observacionesGenerales', etiqueta: 'Observaciones generales (fortalezas y aspectos a mejorar)', tipo: 'textarea', completo: true, valor: v.observacionesGenerales ?? '', atributos: { maxlength: 5000 } }),
  ];
}

async function planificacionesAutorizadas(inscripcionId) {
  if (!inscripcionId) return [];
  try { return await api.get('/api/planificaciones', { inscripcionId, estado: 'AUTORIZADA' }); } catch { return []; }
}

// ============================ NUEVA ============================
async function nueva(cont, ctx) {
  ctx.titulo('Nueva evaluación');
  const volver = html`<a class="btn btn-fantasma" href="#/evaluaciones">${icono('back')} Volver a evaluaciones</a>`;
  if (!ctx.tiene('PROFESOR_GUIA') && !ctx.tiene('TUTOR')) {
    cont.innerHTML = html`${cabecera({ hu: 'HU-03', titulo: 'Nueva evaluación', acciones: volver })}
      ${alerta('error', html`<strong>No tienes permiso para esta acción.</strong> Solo el profesor guía o el tutor asignado puede evaluar una clase.`)}`.toString();
    return;
  }

  // Solo donde es el guía asignado: el jefe de carrera que también es guía recibe todas las inscripciones
  const inscripciones = (await api.get('/api/inscripciones'))
    .filter((i) => i.estado === 'EN_CURSO' && evaluaEn(ctx, i));
  if (inscripciones.length === 0) {
    cont.innerHTML = html`${cabecera({ hu: 'HU-03', titulo: 'Nueva evaluación', acciones: volver })}
      ${vacio('No tienes estudiantes con una práctica en curso', 'Solo se puede evaluar una inscripción EN_CURSO que el coordinador te haya asignado.', 'users')}`.toString();
    return;
  }

  cont.innerHTML = html`
    ${cabecera({ hu: 'HU-03', titulo: 'Nueva evaluación', subtitulo: 'Primero los datos de la clase observada; después completas los 20 indicadores.', acciones: volver })}
    <form class="tarjeta" id="form" novalidate>
      ${cajaAlertaForm()}
      <div class="rejilla-form">
        ${campo({ nombre: 'inscripcionId', etiqueta: 'Estudiante e inscripción', tipo: 'select', requerido: true, completo: true,
          atributos: { 'data-numero': true }, opciones: opcionesInscripcion(inscripciones), valor: ctx.consulta.get('inscripcionId') ?? '' })}
        ${campo({ nombre: 'pautaId', etiqueta: 'Pauta', tipo: 'select', requerido: true, completo: true, atributos: { 'data-numero': true },
          vacioTexto: 'Elige primero la inscripción', ayuda: 'Cargamos las pautas activas de la práctica elegida.' })}
        <div id="aviso-pauta" class="completo" style="grid-column:1/-1"></div>
        ${camposCabecera()}
      </div>
      <div class="acciones-form">
        <a class="btn" href="#/evaluaciones">Cancelar</a>
        <button type="submit" class="btn btn-primario" data-cargando="Creando…">Crear y completar pauta</button>
      </div>
    </form>`.toString();

  const form = cont.querySelector('#form');
  const selInsc = form.elements.inscripcionId;
  const selPauta = form.elements.pautaId;
  const selPlan = form.elements.planificacionId;
  const aviso = cont.querySelector('#aviso-pauta');
  let cambio = 0;

  async function alCambiarInscripcion() {
    const mio = ++cambio;
    const insc = inscripciones.find((i) => String(i.id) === selInsc.value);
    selPauta.innerHTML = '<option value="">Cargando…</option>';
    selPlan.innerHTML = '<option value="">Sin planificación vinculada</option>';
    aviso.innerHTML = '';
    if (!insc) { selPauta.innerHTML = '<option value="">Elige primero la inscripción</option>'; return; }
    const [pautas, planes] = await Promise.all([
      api.get('/api/pautas', { asignaturaId: insc.asignatura.id }).catch(() => []),
      planificacionesAutorizadas(insc.id),
    ]);
    if (mio !== cambio) return;
    // Solo las pautas que aplica quien evalúa en ESTA inscripción (HU-07, criterio 3: el tutor solo usa pautas de tutor)
    const activas = pautas.filter((p) => p.activa && p.tipoEvaluador === TIPO_DE_PAUTA(ctx, insc));
    selPauta.innerHTML = html`<option value="">Seleccione…</option>${activas.map((p) => html`<option value="${p.id}">${p.nombre} (${p.totalIndicadores} indicadores, escala ${p.escalaMin} a ${p.escalaMax})</option>`)}`.toString();
    if (activas.length === 1) selPauta.value = String(activas[0].id);
    if (activas.length === 0) {
      aviso.innerHTML = alerta('aviso', html`Todavía no hay una pauta digital activa para <strong>${insc.asignatura.nombre}</strong> que puedas aplicar ${TIPO_DE_PAUTA(ctx, insc) === 'TUTOR' ? 'como tutor' : 'como profesor guía'}. El coordinador las crea en «Pautas de evaluación».`).toString();
    }
    selPlan.innerHTML = html`<option value="">Sin planificación vinculada</option>${planes.map((p) => html`<option value="${p.id}" data-fecha="${p.fechaClase ?? ''}">${p.titulo} (${fecha(p.fechaClase)})</option>`)}`.toString();
  }
  selInsc.addEventListener('change', alCambiarInscripcion);

  // Al vincular una planificación se adelantan la fecha y el objetivo (el guía puede corregirlos).
  selPlan.addEventListener('change', async () => {
    if (!selPlan.value) return;
    const opcion = selPlan.selectedOptions[0];
    if (opcion?.dataset.fecha && !form.elements.fechaClase.value) form.elements.fechaClase.value = opcion.dataset.fecha;
    if (!form.elements.objetivoClase.value.trim()) {
      try {
        const p = await api.get(`/api/planificaciones/${selPlan.value}`);
        if (p.objetivo && !form.elements.objetivoClase.value.trim()) form.elements.objetivoClase.value = p.objetivo;
      } catch { /* el objetivo se puede escribir a mano */ }
    }
  });

  enlazarFormulario(form, form.querySelector('button[type=submit]'), {
    alEnviar: (datos) => {
      const cuerpo = { ...datos };
      if (!cuerpo.planificacionId) delete cuerpo.planificacionId;
      return api.post('/api/evaluaciones', cuerpo);
    },
    alListo: (ev) => { toast('Evaluación creada. Completa ahora los indicadores.'); ctx.ir(`#/evaluaciones/${ev.id}`); },
  });

  if (selInsc.value) await alCambiarInscripcion();
  // Desde "Evaluar esta clase" llega también la planificación: se deja vinculada (adelanta fecha y objetivo).
  const planPedida = ctx.consulta.get('planificacionId');
  if (planPedida && [...selPlan.options].some((o) => o.value === planPedida)) {
    selPlan.value = planPedida;
    selPlan.dispatchEvent(new Event('change'));
  }
}

// ============================ DETALLE ============================
async function detalle(cont, ctx, id) {
  ctx.titulo('Evaluación');
  let ev = await api.get(`/api/evaluaciones/${id}`);
  const pauta = await api.get(`/api/pautas/${ev.pauta.id}`);

  const esAutor = () => ev.evaluador.id === ctx.usuario.id;   // el guía o el tutor que la creó
  const editable = () => ev.estado === 'BORRADOR' && esAutor();
  const respondidas = new Map(ev.respuestas.map((r) => [r.indicadorId, r]));
  const escala = [];
  for (let n = pauta.escalaMin; n <= pauta.escalaMax; n += 1) escala.push(n);

  const pintar = () => {
    cont.innerHTML = html`
      ${cabecera({
        hu: 'HU-03', titulo: `Evaluación de ${ev.estudiante.nombreCompleto}`,
        subtitulo: `${ev.pauta.nombre}${conObservacion(ev.pauta)}`,
        acciones: html`<a class="btn btn-fantasma" href="#/evaluaciones">${icono('back')} Volver</a>
          <a class="btn" href="#/historial${ctx.tiene('ESTUDIANTE') ? '' : `/${ev.estudiante.id}`}">${icono('clock')} Historial</a>`,
      })}
      <div id="avisos"></div>
      <div id="datos"></div>
      ${ev.estado === 'FINALIZADA' ? resultado() : ''}
      <div id="nota-voz"></div>
      <div class="tarjeta pauta" id="pauta">
        <h2>Indicadores de la pauta <span class="contador" id="contador-titulo"></span></h2>
        ${!editable() && ev.estado === 'BORRADOR' ? alerta('info', 'Borrador: solo lo ven su autor, el coordinador y el jefe de carrera. Al finalizar lo verán el estudiante y el profesor del curso.') : ''}
        ${pauta.dimensiones.map((d) => html`
          <section class="dimension">
            <h3>${d.nombre}</h3>
            ${d.indicadores.map((ind) => indicador(ind))}
          </section>`)}
      </div>
      ${editable() ? barraAcciones() : ''}
      <div id="documentos"></div>`.toString();
    pintarDatos();
    enlazar();
    actualizarContador();
    montarNotaVoz(cont.querySelector('#nota-voz'), { evaluacionId: ev.id, esEvaluador: esAutor() });
    montarDocumentos(cont.querySelector('#documentos'), {
      base: `/api/evaluaciones/${ev.id}/documentos`,
      puedeSubir: esAutor(),
      tiposAceptados: '.docx,.xlsx,.pdf',
      ayuda: 'Word (.docx), Excel (.xlsx) o PDF, máximo 10 MB: la pauta y evaluación formal.',
    });
  };

  function resultado() {
    const pct = Math.max(0, Math.min(100, ((Number(ev.promedio) - pauta.escalaMin) / (pauta.escalaMax - pauta.escalaMin)) * 100));
    return html`<div class="tarjeta resultado">
      <div class="resultado-cifra"><span class="cifra">${promedio(ev.promedio)}</span><span class="cifra-etiqueta">promedio (escala ${pauta.escalaMin} a ${pauta.escalaMax})</span></div>
      <div class="resultado-detalle">
        <p><strong>${ev.puntajeTotal}</strong> de ${ev.puntajeMaximo} puntos posibles · ${ev.indicadoresRespondidos} de ${ev.indicadoresTotales} indicadores</p>
        <div class="barra-prom" role="img" aria-label="Promedio ${promedio(ev.promedio)} de ${pauta.escalaMax}"><span style="width:${pct}%"></span></div>
        <p class="subtitulo">Finalizada el ${fechaHora(ev.fechaFinalizacion)}. Ya no se puede modificar.</p>
      </div>
    </div>`;
  }

  function indicador(ind) {
    const r = respondidas.get(ind.id);
    if (!editable()) {
      return html`<div class="indicador">
        <span class="ind-num">${ind.numero}</span>
        <div class="ind-texto"><p>${ind.descripcion}</p>${r?.comentario ? html`<p class="ind-comentario">${r.comentario}</p>` : ''}</div>
        <div class="ind-nota">${r ? html`<span class="nota-fija nota-${r.puntaje}">${r.puntaje}</span>` : html`<span class="nota-fija nota-vacia" title="Sin responder">—</span>`}</div>
      </div>`;
    }
    return html`<div class="indicador" data-indicador="${ind.id}">
      <span class="ind-num">${ind.numero}</span>
      <div class="ind-texto">
        <p id="desc-${ind.id}">${ind.descripcion}</p>
        <input type="text" class="ind-comentario-input" name="c-${ind.id}" maxlength="500" placeholder="Comentario (opcional)" value="${r?.comentario ?? ''}" aria-label="Comentario del indicador ${ind.numero}">
      </div>
      <div class="escala" role="radiogroup" aria-labelledby="desc-${ind.id}">
        ${escala.map((n) => html`<label class="punto"><input type="radio" name="p-${ind.id}" value="${n}" ${r?.puntaje === n ? html`checked` : ''}><span>${n}</span></label>`)}
      </div>
    </div>`;
  }

  function barraAcciones() {
    return html`<div class="barra-fija">
      <div class="barra-fija-info"><strong id="contador">0 de ${pauta.totalIndicadores}</strong> indicadores · <span id="parcial"></span></div>
      <div class="barra-fija-botones">
        <button type="button" class="btn" id="guardar" data-cargando="Guardando…">${icono('check-circle')} Guardar avance</button>
        <button type="button" class="btn btn-ok" id="finalizar" data-cargando="Finalizando…">${icono('send')} Finalizar evaluación</button>
      </div>
    </div>`;
  }

  function pintarDatos() {
    cont.querySelector('#datos').innerHTML = html`
      <div class="tarjeta">
        <div class="tarjeta-titulo"><h2>Datos de la clase observada ${badge(ev.estado)}</h2>
          ${editable() ? html`<button type="button" class="btn btn-chico" id="editar">${icono('edit')} Editar datos</button>` : ''}</div>
        <dl class="datos datos-2">
          <div><dt>Estudiante</dt><dd>${ev.estudiante.nombreCompleto} <small>(${ev.estudiante.rut})</small></dd></div>
          <div><dt>Evaluador</dt><dd>${ev.evaluador.nombreCompleto}</dd></div>
          <div><dt>Fecha</dt><dd>${fecha(ev.fechaClase)} · ${hora(ev.horaInicio)} a ${hora(ev.horaTermino)}</dd></div>
          <div><dt>Establecimiento y curso</dt><dd>${ev.establecimiento} · ${ev.curso}</dd></div>
          <div><dt>Planificación vinculada</dt><dd>${ev.planificacionId ? html`<a href="#/planificaciones/${ev.planificacionId}">Ver planificación #${ev.planificacionId}</a>` : 'Ninguna'}</dd></div>
          <div class="completo"><dt>Objetivo de la clase</dt><dd>${ev.objetivoClase}</dd></div>
          <div class="completo"><dt>Observaciones generales</dt><dd>${ev.observacionesGenerales || html`<small>Sin observaciones.</small>`}</dd></div>
        </dl>
      </div>`.toString();
    cont.querySelector('#editar')?.addEventListener('click', editarDatos);
  }

  // ----- lectura de los puntajes marcados -----
  const leerRespuestas = () => {
    const items = [];
    cont.querySelectorAll('[data-indicador]').forEach((fila) => {
      const indicadorId = Number(fila.dataset.indicador);
      const marcado = fila.querySelector('input[type=radio]:checked');
      if (!marcado) return;
      const comentario = fila.querySelector('.ind-comentario-input').value.trim();
      items.push({ indicadorId, puntaje: Number(marcado.value), ...(comentario ? { comentario } : {}) });
    });
    return items;
  };

  function actualizarContador() {
    if (!editable()) {
      const t = cont.querySelector('#contador-titulo');
      if (t) t.textContent = `(${ev.indicadoresRespondidos} de ${ev.indicadoresTotales} respondidos)`;
      return;
    }
    const items = leerRespuestas();
    const suma = items.reduce((a, x) => a + x.puntaje, 0);
    cont.querySelector('#contador').textContent = `${items.length} de ${pauta.totalIndicadores}`;
    cont.querySelector('#parcial').textContent = `puntaje parcial ${suma} de ${pauta.totalIndicadores * pauta.escalaMax}`
      + (items.length === pauta.totalIndicadores ? ` · promedio ${decimal(suma / pauta.totalIndicadores, 2)}` : '');
    cont.querySelector('#contador-titulo').textContent = `(${items.length} de ${pauta.totalIndicadores})`;
    cont.querySelectorAll('.indicador.falta').forEach((f) => { if (f.querySelector('input:checked')) f.classList.remove('falta'); });
  }

  async function guardar() {
    const items = leerRespuestas();
    if (items.length === 0) return null;
    const actualizado = await api.put(`/api/evaluaciones/${ev.id}/respuestas`, { respuestas: items });
    ev = actualizado;
    return actualizado;
  }

  function enlazar() {
    if (!editable()) return;
    cont.querySelectorAll('.pauta input').forEach((el) => el.addEventListener('input', actualizarContador));

    cont.querySelector('#guardar').addEventListener('click', async (e) => {
      const avisos = cont.querySelector('#avisos');
      avisos.innerHTML = '';
      if (leerRespuestas().length === 0) { toast('Marca al menos un indicador para guardar.', 'error'); return; }
      try {
        await conCarga(e.currentTarget, guardar);
        toast('Avance guardado.');
      } catch (err) { if (err.status !== 401) avisos.innerHTML = alertaError(err).toString(); }
    });

    cont.querySelector('#finalizar').addEventListener('click', async (e) => {
      const boton = e.currentTarget;
      const avisos = cont.querySelector('#avisos');
      avisos.innerHTML = '';
      const faltan = [...cont.querySelectorAll('[data-indicador]')].filter((f) => !f.querySelector('input:checked'));
      if (faltan.length > 0) {
        faltan.forEach((f) => f.classList.add('falta'));
        faltan[0].scrollIntoView({ behavior: 'smooth', block: 'center' });
        avisos.innerHTML = alerta('aviso', html`<strong>Faltan ${faltan.length} de ${pauta.totalIndicadores} indicadores.</strong> Para finalizar hay que responder todos (quedaron resaltados).`).toString();
        return;
      }
      const ok = await confirmar({
        titulo: 'Finalizar evaluación',
        mensaje: 'Se calcularán el puntaje total y el promedio, y la evaluación quedará visible para el estudiante y ya no se podrá modificar. ¿Continuar?',
        textoOk: 'Finalizar',
      });
      if (!ok) return;
      try {
        await conCarga(boton, async () => {
          await guardar();
          ev = await api.post(`/api/evaluaciones/${ev.id}/finalizar`);
        });
        toast(`Evaluación finalizada. Promedio ${promedio(ev.promedio)}.`);
        ev.respuestas.forEach((r) => respondidas.set(r.indicadorId, r));
        pintar();
        cont.closest('.principal')?.scrollTo({ top: 0, behavior: 'smooth' });
      } catch (err) {
        if (err.status !== 401) { avisos.innerHTML = alertaError(err).toString(); avisos.scrollIntoView({ behavior: 'smooth', block: 'center' }); }
      }
    });
  }

  async function editarDatos() {
    const planes = await planificacionesAutorizadas(ev.inscripcionId);
    modalFormulario({
      titulo: 'Editar datos de la clase', textoOk: 'Guardar cambios',
      campos: camposCabecera(ev, { planificaciones: planes }),
      alEnviar: (datos) => {
        const cuerpo = { ...datos };
        if (!cuerpo.planificacionId) delete cuerpo.planificacionId;
        return api.put(`/api/evaluaciones/${ev.id}`, cuerpo);
      },
      alListo: (actualizado) => {
        ev = { ...ev, ...actualizado, respuestas: ev.respuestas };
        pintarDatos();
        toast('Datos de la clase actualizados.');
      },
    });
  }

  pintar();
}
