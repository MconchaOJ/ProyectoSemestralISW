// HU-15 · Asistente de planificación (chatbot) y HU-16 · el profesor guía lee la conversación.
//   Estudiante: #/chatbot (sus prácticas con asistente) y #/chatbot/{inscripcionId} (conversa)
//   Profesor guía: #/chatbot (sus estudiantes de Práctica Pedagógica y Profesional) y #/chatbot/{inscripcionId} (lee)
import { api } from '../api.js';
import {
  alerta, alertaError, badge, cabecera, conCarga, fechaHora, html, icono, raw, vacio, esc, iniciales,
} from '../ui.js';

export async function render(cont, ctx) {
  const [id] = ctx.args;
  ctx.titulo('Asistente de planificación');
  if (id) return conversacion(cont, ctx, id);
  return selector(cont, ctx);
}

/** Las inscripciones con asistente: el estudiante ve las suyas; el guía, las de sus estudiantes. */
async function selector(cont, ctx) {
  const todas = await api.get('/api/inscripciones');
  const conAsistente = todas.filter((i) => i.asignatura.chatbotHabilitado
    && (ctx.tiene('ESTUDIANTE') || ctx.actuaComo('PROFESOR_GUIA', i)));
  const estudiante = ctx.tiene('ESTUDIANTE');

  // un estudiante con una sola práctica en curso con asistente entra directo a conversar
  const enCurso = conAsistente.filter((i) => i.estado === 'EN_CURSO');
  if (estudiante && enCurso.length === 1) return conversacion(cont, ctx, enCurso[0].id);

  cont.innerHTML = html`
    ${cabecera({
      titulo: 'Asistente de planificación',
      subtitulo: estudiante
        ? 'Conversa sobre una planificación que ya escribiste para recibir retroalimentación. Disponible en Práctica Pedagógica y Práctica Profesional.'
        : 'Conversaciones de tus estudiantes de Práctica Pedagógica y Profesional con el asistente (solo lectura).',
    })}
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
  const planes = c.puedeEscribir ? await api.get('/api/planificaciones', { inscripcionId }) : [];
  ctx.titulo(propio ? 'Asistente de planificación' : `Chat de ${c.estudiante}`);
  const mensajes = [...c.mensajes];

  const aviso = propio
    ? (c.profesorGuia
      ? alerta('info', html`Tu profesor guía, <strong>${c.profesorGuia.nombreCompleto}</strong>, puede leer esta conversación.`)
      : alerta('info', 'Tu profesor guía puede leer esta conversación.'))
    : alerta('info', html`Solo lectura: eres el profesor guía de ${c.estudiante} en esta práctica. El estudiante sabe que puedes leer la conversación.`);

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
