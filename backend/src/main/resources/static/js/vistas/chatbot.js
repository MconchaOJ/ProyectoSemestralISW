// HU-15 · Asistente de planificación (chatbot), HU-16 · el profesor guía lee la conversación y HU-20 · el jefe de
// carrera la lee también.
//   Estudiante: #/chatbot (sus prácticas con asistente) y #/chatbot/{inscripcionId} (conversa)
//   Profesor guía: #/chatbot (sus estudiantes de Práctica Pedagógica y Profesional) y #/chatbot/{inscripcionId} (lee)
//   Jefe de carrera: #/chatbot (todos los estudiantes de esas prácticas, con filtro) y #/chatbot/{inscripcionId} (lee)
// HU-30 · Estudiante validador: #/chatbot/validacion (responde la Escala SUS y la rúbrica de juicio experto)
import { api } from '../api.js';
import {
  alerta, alertaError, badge, cabecera, conCarga, fechaHora, html, icono, raw, toast, vacio, esc, iniciales,
} from '../ui.js';

export async function render(cont, ctx) {
  const [id] = ctx.args;
  ctx.titulo(ctx.tiene('JEFE_CARRERA') ? 'Conversaciones del asistente' : 'Asistente de planificación');
  if (id === 'validacion' && ctx.tiene('ESTUDIANTE')) return formularioValidacion(cont, ctx);
  if (id) return conversacion(cont, ctx, id);
  return selector(cont, ctx);
}

/** Las inscripciones con asistente: el estudiante ve las suyas; el guía, las de sus estudiantes; el jefe, todas. */
async function selector(cont, ctx) {
  const todas = await api.get('/api/inscripciones');
  const jefe = ctx.tiene('JEFE_CARRERA');
  const conAsistente = todas.filter((i) => i.asignatura.chatbotHabilitado
    && (jefe || ctx.tiene('ESTUDIANTE') || ctx.actuaComo('PROFESOR_GUIA', i)));
  const estudiante = ctx.tiene('ESTUDIANTE');
  if (jefe) return listaJefe(cont, conAsistente);

  // un estudiante con una sola práctica en curso con asistente entra directo a conversar
  const enCurso = conAsistente.filter((i) => i.estado === 'EN_CURSO');
  if (estudiante && enCurso.length === 1) return conversacion(cont, ctx, enCurso[0].id);

  const validacion = estudiante ? await avisoValidador() : '';
  cont.innerHTML = html`
    ${cabecera({
      titulo: 'Asistente de planificación',
      subtitulo: estudiante
        ? 'Conversa sobre una planificación que ya escribiste para recibir retroalimentación. Disponible en Práctica Pedagógica y Práctica Profesional.'
        : 'Conversaciones de tus estudiantes de Práctica Pedagógica y Profesional con el asistente (solo lectura).',
    })}
    ${validacion}
    ${conAsistente.length === 0
      ? vacio(estudiante ? 'Aún no tienes acceso al asistente' : 'No tienes estudiantes con asistente',
        estudiante ? 'El asistente se habilita cuando cursas Práctica Pedagógica o Práctica Profesional.'
          : 'Aparecerán aquí tus estudiantes de Práctica Pedagógica y Profesional.', 'chat')
      : html`<div class="tarjetas-estudiantes">${conAsistente.map((i) => html`
          <a class="tarjeta-estudiante" href="#/chatbot/${i.id}">
            <span class="avatar-grande">${estudiante ? icono('chat') : iniciales({ nombre: i.estudiante.nombreCompleto.split(' ')[0], apellidos: i.estudiante.nombreCompleto.split(' ')[1] ?? '' })}</span>
            <span><strong>${estudiante ? i.asignatura.nombre : i.estudiante.nombreCompleto}</strong>
              <small>${estudiante ? i.periodo : `${i.asignatura.nombre} · ${i.periodo}`}</small> ${badge(i.estado)}</span>
            ${icono('chat')}
          </a>`)}</div>`}`.toString();
}

const tarjetaEstudiante = (i) => html`
  <a class="tarjeta-estudiante" href="#/chatbot/${i.id}">
    <span class="avatar-grande">${iniciales({ nombre: i.estudiante.nombreCompleto.split(' ')[0], apellidos: i.estudiante.nombreCompleto.split(' ')[1] ?? '' })}</span>
    <span><strong>${i.estudiante.nombreCompleto}</strong>
      <small>${i.asignatura.nombre} · ${i.periodo}</small> ${badge(i.estado)}</span>
    ${icono('chat')}
  </a>`;

/** HU-20: el jefe de carrera ve todas las prácticas con asistente y filtra por estudiante, asignatura o período. */
function listaJefe(cont, inscripciones) {
  const orden = [...inscripciones].sort((a, b) => b.periodo.localeCompare(a.periodo)
    || a.estudiante.nombreCompleto.localeCompare(b.estudiante.nombreCompleto));
  cont.innerHTML = html`
    ${cabecera({
      titulo: 'Conversaciones del asistente',
      subtitulo: 'Conversaciones de los estudiantes de Práctica Pedagógica y Profesional con el asistente de planificación (solo lectura).',
    })}
    ${orden.length === 0
      ? vacio('Aún no hay prácticas con asistente', 'Aparecerán aquí las inscripciones de Práctica Pedagógica y Profesional.', 'chat')
      : html`<label for="f-filtro" class="sr-only">Filtrar</label>
        <input type="search" id="f-filtro" class="chat-filtro" placeholder="Filtrar por estudiante, asignatura o período…">
        <div class="tarjetas-estudiantes" id="lista-chats"></div>`}`.toString();
  const lista = cont.querySelector('#lista-chats');
  if (!lista) return;
  const texto = (i) => `${i.estudiante.nombreCompleto} ${i.asignatura.nombre} ${i.periodo}`.toLowerCase()
    .normalize('NFD').replace(/[̀-ͯ]/g, '');
  const pintar = (filtro) => {
    const f = filtro.trim().toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
    const visibles = orden.filter((i) => texto(i).includes(f));
    lista.innerHTML = visibles.length
      ? visibles.map(tarjetaEstudiante).join('')
      : vacio('Sin resultados', 'Ninguna práctica coincide con el filtro.', 'search').toString();
  };
  pintar('');
  cont.querySelector('#f-filtro').addEventListener('input', (ev) => pintar(ev.target.value));
}

/** Texto del asistente: se escapa todo y solo se reconoce **negrita** (el resto queda como texto con saltos de línea). */
const formatear = (texto) => raw(esc(texto).replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>'));

function burbuja(m) {
  const delEstudiante = m.autor === 'ESTUDIANTE';
  return html`<li class="chat-mensaje ${delEstudiante ? 'chat-estudiante' : 'chat-asistente'}">
    <div class="chat-burbuja">
      ${m.planificacionTitulo ? html`<span class="chat-adjunto">${icono('file')} Planificación: ${m.planificacionTitulo}</span>` : ''}
      <div class="chat-texto">${delEstudiante ? m.contenido : formatear(m.contenido)}</div>
    </div>
    <small class="chat-meta">${delEstudiante ? 'Estudiante' : 'Asistente'} · ${fechaHora(m.fecha)}</small>
  </li>`;
}

/** La planificación que el asistente está leyendo: la última que el estudiante adjuntó en la conversación. */
const enRevision = (mensajes) => [...mensajes].reverse().find((m) => m.planificacionId)?.planificacionTitulo ?? null;

async function conversacion(cont, ctx, inscripcionId) {
  const c = await api.get(`/api/inscripciones/${inscripcionId}/chatbot`);
  const propio = ctx.tiene('ESTUDIANTE');
  const esGuia = ctx.tiene('PROFESOR_GUIA') && c.profesorGuia?.id === ctx.usuario.id;
  const planes = c.puedeEscribir ? await api.get('/api/planificaciones', { inscripcionId }) : [];
  ctx.titulo(propio ? 'Asistente de planificación' : `Chat de ${c.estudiante}`);
  const mensajes = [...c.mensajes];

  let aviso;
  if (propio) {
    const guia = c.profesorGuia
      ? html`Tu profesor guía, <strong>${c.profesorGuia.nombreCompleto}</strong>, puede leer esta conversación.`
      : 'Tu profesor guía puede leer esta conversación.';
    // HU-20, criterios 3 y 4 (SUPUESTO S-66): el estudiante sabe que el jefe de carrera lee y cuándo lo hizo
    const jefe = c.avisoJefeCarrera
      ? html`<br>${c.avisoJefeCarrera} ${c.ultimaLecturaJefeCarrera ? `Última lectura: ${fechaHora(c.ultimaLecturaJefeCarrera)}.` : 'Aún no la ha leído.'}`
      : '';
    aviso = html`${alerta('info', html`${guia}${jefe}`)}${await avisoValidador()}`;
  } else if (esGuia) {
    aviso = alerta('info', html`Solo lectura: eres el profesor guía de ${c.estudiante} en esta práctica. El estudiante sabe que puedes leer la conversación.`);
  } else {
    // HU-20 (SUPUESTO S-66): el jefe de carrera lee en solo lectura
    aviso = alerta('info', html`Solo lectura: como jefe de carrera puedes leer esta conversación.${c.profesorGuia ? html` Profesor guía: ${c.profesorGuia.nombreCompleto}.` : ''}`);
  }

  cont.innerHTML = html`
    ${cabecera({
      titulo: propio ? 'Asistente de planificación' : `Conversación de ${c.estudiante}`,
      subtitulo: propio
        ? `${c.asignatura} · ${c.periodo}. Retroalimenta una planificación que ya escribiste: no diseña clases ni pone notas.`
        : `${c.asignatura} · ${c.periodo}. El asistente retroalimenta las planificaciones del estudiante: no las diseña ni pone notas.`,
      acciones: html`<a class="btn btn-fantasma" href="#/chatbot">${icono('back')} Volver</a>`,
    })}
    ${aviso}
    ${c.disponible ? '' : alerta('aviso', c.motivoNoDisponible)}
    <div class="tarjeta chat">
      <ol class="chat-lista" id="chat-lista" aria-live="polite"></ol>
      ${c.puedeEscribir ? html`
        <form class="chat-form" id="chat-form" novalidate>
          <div data-alerta-form hidden></div>
          <div class="chat-planificacion">
            <label for="f-planificacion">Planificación a revisar</label>
            <select id="f-planificacion">
              <option value="">${enRevision(mensajes) ? 'Seguir con la planificación actual' : 'Ninguna (solo conversar)'}</option>
              ${planes.map((p) => html`<option value="${p.id}">${p.titulo}</option>`)}
            </select>
            <small class="ayuda" id="en-revision"></small>
          </div>
          <label for="f-texto" class="sr-only">Tu mensaje</label>
          <textarea id="f-texto" rows="3" maxlength="4000" placeholder="Escribe tu mensaje… (Ctrl + Enter para enviar)"></textarea>
          <div class="chat-acciones">
            <small class="ayuda">El asistente lee el formulario y los documentos (Word o PDF) de la planificación que elijas.</small>
            <button type="submit" class="btn btn-primario" data-cargando="El asistente está respondiendo…">${icono('send')} Enviar</button>
          </div>
        </form>` : ''}
    </div>`.toString();

  const lista = cont.querySelector('#chat-lista');
  const pintar = () => {
    lista.innerHTML = mensajes.length === 0
      ? html`<li class="chat-vacio">${vacio(propio ? 'Todavía no hay mensajes' : 'El estudiante aún no ha conversado con el asistente',
        propio ? 'Elige tu planificación y cuéntale al asistente qué quieres revisar.' : '', 'chat')}</li>`.toString()
      : mensajes.map(burbuja).join('');
    lista.scrollTop = lista.scrollHeight;
    const nota = cont.querySelector('#en-revision');
    if (nota) {
      const actual = enRevision(mensajes);
      nota.textContent = actual ? `El asistente está leyendo: ${actual}` : '';
    }
  };
  pintar();

  const form = cont.querySelector('#chat-form');
  if (!form) return;
  const texto = form.querySelector('#f-texto');
  const plan = form.querySelector('#f-planificacion');
  const boton = form.querySelector('button[type=submit]');
  const caja = form.querySelector('[data-alerta-form]');

  texto.addEventListener('keydown', (ev) => {
    if (ev.key === 'Enter' && (ev.ctrlKey || ev.metaKey)) form.requestSubmit();
  });
  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    caja.hidden = true;
    const valor = texto.value.trim();
    if (!valor) { texto.focus(); return; }
    const cuerpo = { texto: valor };
    if (plan.value) cuerpo.planificacionId = Number(plan.value);
    try {
      const r = await conCarga(boton, () => api.post(`/api/inscripciones/${inscripcionId}/chatbot/mensajes`, cuerpo));
      if (!ctx.vigente()) return;
      mensajes.push(r.mensajeEstudiante, r.respuesta);
      texto.value = '';
      plan.value = '';
      plan.options[0].textContent = enRevision(mensajes) ? 'Seguir con la planificación actual' : 'Ninguna (solo conversar)';
      pintar();
      texto.focus();
    } catch (e) {
      if (e.status === 401) return;
      caja.innerHTML = alertaError(e).toString();
      caja.hidden = false;
    }
  });
}

// ---------- HU-30 · Validación del asistente (estudiante validador) ----------

/** Si el estudiante es validador y le falta responder, un aviso con el enlace al formulario. */
async function avisoValidador() {
  try {
    const v = await api.get('/api/chatbot/validacion/mi-validacion');
    if (!v.validador || (v.susRespondida && v.rubricaRespondida)) return '';
    return alerta('aviso', html`Participas en la <strong>validación del asistente</strong>: cuando lo hayas usado, responde la Escala SUS y la rúbrica. <a href="#/chatbot/validacion">Ir a la validación</a>`);
  } catch {
    return '';   // el aviso es secundario: si falla, la conversación se muestra igual
  }
}

/** Una fila con su número, su texto y la escala de puntos (mismos estilos que la pauta de evaluación). */
function filaEscala(nombre, numero, texto, desde, hasta, extra = '') {
  const puntos = [];
  for (let n = desde; n <= hasta; n += 1) puntos.push(n);
  return html`<div class="indicador">
    <span class="ind-num">${numero}</span>
    <div class="ind-texto"><p id="d-${nombre}">${texto}</p>${extra}</div>
    <div class="escala" role="radiogroup" aria-labelledby="d-${nombre}">
      ${puntos.map((n) => html`<label class="punto"><input type="radio" name="${nombre}" value="${n}"><span>${n}</span></label>`)}
    </div>
  </div>`;
}

const leyenda = (niveles) => html`<p class="ayuda">${niveles.map((t, i) => `${i + 1} = ${t}`).join(' · ')}</p>`;

async function formularioValidacion(cont, ctx) {
  ctx.titulo('Validación del asistente');
  const v = await api.get('/api/chatbot/validacion/mi-validacion');
  if (!ctx.vigente()) return;
  const ins = v.instrumentos;
  const volver = html`<a class="btn btn-fantasma" href="#/chatbot">${icono('back')} Volver al asistente</a>`;
  if (!v.validador) {
    cont.innerHTML = html`${cabecera({ titulo: 'Validación del asistente', acciones: volver })}
      ${vacio('No participas en la validación', 'El coordinador elige a los estudiantes que validan el asistente.', 'users')}`.toString();
    return;
  }

  cont.innerHTML = html`
    ${cabecera({
      titulo: 'Validación del asistente',
      subtitulo: 'Responde después de usar el asistente con tus planificaciones. Cada instrumento se responde una sola vez y el coordinador ve las respuestas con tu nombre.',
      acciones: volver,
    })}
    <section class="tarjeta asistente-seccion" aria-labelledby="t-sus">
      <div class="tarjeta-titulo"><h2 id="t-sus">1. Escala SUS (usabilidad)</h2></div>
      ${v.susRespondida
        ? alerta('ok', `Respondiste la Escala SUS el ${fechaHora(v.susRespondida)}. ¡Gracias!`)
        : html`<form id="form-sus" novalidate>
          <div data-alerta-form hidden></div>
          <p class="ayuda">Marca tu nivel de acuerdo con cada afirmación sobre tu experiencia con el asistente.</p>
          ${leyenda(ins.escalaSus)}
          ${ins.itemsSus.map((t, i) => filaEscala(`sus-${i + 1}`, i + 1, t, 1, 5))}
          <div class="estilo-pie"><span></span><button type="submit" class="btn btn-primario" data-cargando="Enviando…">${icono('send')} Enviar Escala SUS</button></div>
        </form>`}
    </section>
    <section class="tarjeta asistente-seccion" aria-labelledby="t-rubrica">
      <div class="tarjeta-titulo"><h2 id="t-rubrica">2. Rúbrica de juicio experto</h2></div>
      ${v.rubricaRespondida
        ? alerta('ok', `Respondiste la rúbrica el ${fechaHora(v.rubricaRespondida)}. ¡Gracias!`)
        : html`<form id="form-rubrica" novalidate>
          <div data-alerta-form hidden></div>
          <p class="ayuda">Evalúa el desempeño global del asistente en todas tus conversaciones. Bajo cada criterio va lo que corresponde al nivel más alto.</p>
          ${leyenda(ins.nivelesRubrica)}
          ${ins.criterios.map((c) => filaEscala(`cr-${c.numero}`, c.numero, html`<strong>${c.nombre}</strong>`, 1, 4,
            html`<small class="ayuda">Nivel 4: ${c.logroAlto}</small>
              <input type="text" class="ind-comentario-input" name="com-${c.numero}" maxlength="1000" placeholder="Comentario (opcional)" aria-label="Comentario del criterio ${c.numero}">`))}
          <h3 class="validacion-subtitulo">Preguntas abiertas (opcionales)</h3>
          ${ins.preguntasAbiertas.map((p, i) => html`<label for="abierta-${i}">${p}</label>
            <textarea id="abierta-${i}" rows="3" maxlength="4000"></textarea>`)}
          <div class="estilo-pie"><span></span><button type="submit" class="btn btn-primario" data-cargando="Enviando…">${icono('send')} Enviar rúbrica</button></div>
        </form>`}
    </section>`.toString();

  /** El valor marcado en cada grupo de radios, o null si falta alguno. */
  const marcados = (form, prefijo, cantidad) => {
    const valores = [];
    for (let n = 1; n <= cantidad; n += 1) {
      const r = form.querySelector(`input[name="${prefijo}-${n}"]:checked`);
      if (!r) return null;
      valores.push(Number(r.value));
    }
    return valores;
  };
  const alEnviar = (form, armar, ruta, gracias) => form?.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const caja = form.querySelector('[data-alerta-form]');
    caja.hidden = true;
    const cuerpo = armar(form);
    if (!cuerpo) {
      caja.innerHTML = alerta('error', 'Responde todos los ítems antes de enviar.').toString();
      caja.hidden = false;
      return;
    }
    try {
      await conCarga(form.querySelector('button[type=submit]'), () => api.post(ruta, cuerpo));
      if (!ctx.vigente()) return;
      toast(gracias);
      await formularioValidacion(cont, ctx);
    } catch (e) {
      if (e.status === 401) return;
      caja.innerHTML = alertaError(e).toString();
      caja.hidden = false;
    }
  });

  alEnviar(cont.querySelector('#form-sus'), (form) => {
    const items = marcados(form, 'sus', ins.itemsSus.length);
    return items && { items };
  }, '/api/chatbot/validacion/sus', 'Escala SUS enviada.');
  alEnviar(cont.querySelector('#form-rubrica'), (form) => {
    const puntajes = marcados(form, 'cr', ins.criterios.length);
    if (!puntajes) return null;
    const abierta = (i) => form.querySelector(`#abierta-${i}`).value;
    return {
      criterios: puntajes.map((puntaje, i) => ({ puntaje, comentario: form.querySelector(`[name="com-${i + 1}"]`).value })),
      fortalezas: abierta(0), debilidades: abierta(1), mejoras: abierta(2), recomendacion: abierta(3),
    };
  }, '/api/chatbot/validacion/rubrica', 'Rúbrica enviada.');
}
