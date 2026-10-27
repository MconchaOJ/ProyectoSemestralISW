// HU-17 · El coordinador calibra el estilo de retroalimentación del asistente de planificación (chatbot).
//   #/estilo-asistente: un solo texto para todo el sistema (reglas, tono, expresiones y respuestas modelo).
//   El contexto de cada estudiante (semestre, prácticas aprobadas) lo agrega el sistema: no se edita aquí.
// HU-29 · En la misma pantalla, la base de conocimiento (documentos de referencia, uno por tipo).
// HU-30 · Y la validación: con ella abierta, el asistente solo responde a los estudiantes validadores; los
//         validadores responden la Escala SUS y la rúbrica de juicio experto, y aquí se ven los resultados.
import { api } from '../api.js';
import {
  alerta, alertaError, cabecera, confirmar, conCarga, decimal, fechaHora, html, icono, promedio, toast, toastError, vacio,
} from '../ui.js';

const kb = (bytes) => `${Math.max(1, Math.round(bytes / 1024)).toLocaleString('es-CL')} KB`;

/** HU-29: los 10 documentos de la propuesta RetroPlani Mate, cada uno con su archivo vigente o sin cargar. */
async function baseConocimiento(caja, ctx) {
  const docs = await api.get('/api/chatbot/base-conocimiento');
  if (!ctx.vigente()) return;
  const cargados = docs.filter((d) => d.cargado).length;
  caja.innerHTML = html`
    <div class="tarjeta-titulo"><h2 id="t-base">Base de conocimiento</h2><small class="ayuda">${cargados} de ${docs.length} documentos cargados</small></div>
    <p class="ayuda">Documentos de referencia de la carrera (Word o PDF con texto, máximo 10 MB). En cada mensaje el asistente recibe solo los fragmentos más relacionados con la planificación que revisa. Sin documentos funciona igual, solo con sus instrucciones.</p>
    <div class="tabla-contenedor"><table class="tabla">
      <thead><tr><th>Documento</th><th>Archivo vigente</th><th class="acciones">Acciones</th></tr></thead>
      <tbody>${docs.map((d) => html`<tr>
        <td><strong>${d.nombre}</strong><br><small>${d.uso}</small></td>
        <td>${d.cargado
          ? html`${d.nombreArchivo}<br><small>${kb(d.tamanoBytes)} · ${d.caracteres.toLocaleString('es-CL')} caracteres · ${d.subidoPor?.nombreCompleto ?? ''}, ${fechaHora(d.fechaSubida)}</small>`
          : html`<small>Sin cargar</small>`}</td>
        <td class="acciones">
          <input type="file" accept=".docx,.pdf" hidden data-archivo="${d.tipo}">
          <button type="button" class="btn btn-chico" data-subir="${d.tipo}" data-cargando="Subiendo…">${icono('upload')} ${d.cargado ? 'Reemplazar' : 'Subir'}</button>
          ${d.cargado ? html`<button type="button" class="btn btn-fantasma btn-chico" data-quitar="${d.tipo}" aria-label="Quitar ${d.nombre}">${icono('x')}</button>` : ''}
        </td></tr>`)}</tbody>
    </table></div>`.toString();

  caja.querySelectorAll('[data-subir]').forEach((boton) => {
    const entrada = caja.querySelector(`[data-archivo="${boton.dataset.subir}"]`);
    boton.addEventListener('click', () => entrada.click());
    entrada.addEventListener('change', async () => {
      if (!entrada.files.length) return;
      const datos = new FormData();
      datos.append('archivo', entrada.files[0]);
      try {
        await conCarga(boton, () => api.subir(`/api/chatbot/base-conocimiento/${boton.dataset.subir}`, datos));
        toast('Documento cargado: el asistente lo usa desde el próximo mensaje.');
        await baseConocimiento(caja, ctx);
      } catch (e) {
        if (e.status !== 401) toastError(e);
        entrada.value = '';
      }
    });
  });
  caja.querySelectorAll('[data-quitar]').forEach((boton) => boton.addEventListener('click', async () => {
    const ok = await confirmar({
      titulo: 'Quitar documento', mensaje: 'El asistente dejará de usarlo desde el próximo mensaje.', textoOk: 'Quitar', peligro: true,
    });
    if (!ok) return;
    try {
      await api.del(`/api/chatbot/base-conocimiento/${boton.dataset.quitar}`);
      await baseConocimiento(caja, ctx);
    } catch (e) {
      if (e.status !== 401) toastError(e);
    }
  }));
}

/**
 * HU-30: abrir o cerrar la validación y elegir a los validadores entre los estudiantes que hoy cursan una práctica con
 * asistente. Los validadores responden la Escala SUS y la rúbrica desde su asistente (ver resultadosValidacion).
 */
async function validacion(caja, ctx) {
  const [estado, inscripciones] = await Promise.all([api.get('/api/chatbot/validacion'), api.get('/api/inscripciones')]);
  if (!ctx.vigente()) return;
  const marcados = new Set(estado.validadores.map((e) => e.id));
  const candidatos = new Map();
  inscripciones.filter((i) => i.asignatura.chatbotHabilitado && i.estado === 'EN_CURSO')
    .forEach((i) => candidatos.set(i.estudiante.id, { ...i.estudiante, asignatura: i.asignatura.nombre }));
  estado.validadores.forEach((e) => { if (!candidatos.has(e.id)) candidatos.set(e.id, e); });
  const lista = [...candidatos.values()].sort((a, b) => a.nombreCompleto.localeCompare(b.nombreCompleto));

  caja.innerHTML = html`
    <div class="tarjeta-titulo"><h2 id="t-validacion">Validación con estudiantes</h2></div>
    ${estado.abierta
      ? alerta('aviso', html`<strong>Validación abierta:</strong> el asistente solo responde a ${marcados.size === 1 ? 'el estudiante marcado' : `los ${marcados.size} estudiantes marcados`}; los demás ven «aún no disponible».`)
      : alerta('info', 'Validación cerrada: el asistente responde a todos los estudiantes de Práctica Pedagógica y Profesional.')}
    <label class="check"><input type="checkbox" id="f-abierta" ${estado.abierta ? 'checked' : ''}> Validación abierta</label>
    ${lista.length === 0
      ? vacio('No hay estudiantes en prácticas con asistente', '', 'users')
      : html`<p class="ayuda">Estudiantes validadores (cursan hoy una práctica con asistente):</p>
        <ul class="lista-validadores">${lista.map((e) => html`<li><label class="check">
          <input type="checkbox" data-validador="${e.id}" ${marcados.has(e.id) ? 'checked' : ''}>
          ${e.nombreCompleto}${e.asignatura ? html` <small>· ${e.asignatura}</small>` : ''}</label></li>`)}</ul>`}
    <p class="ayuda">Los validadores responden la Escala SUS y la rúbrica de juicio experto desde su asistente («Validación del asistente»); los resultados aparecen abajo.</p>`.toString();

  const cambiar = async (accion) => {
    try {
      await accion();
    } catch (e) {
      if (e.status !== 401) toastError(e);
    }
    await validacion(caja, ctx);
  };
  caja.querySelector('#f-abierta').addEventListener('change', (ev) =>
    cambiar(() => api.put('/api/chatbot/validacion', { abierta: ev.target.checked })));
  caja.querySelectorAll('[data-validador]').forEach((c) => c.addEventListener('change', () =>
    cambiar(() => api.put(`/api/chatbot/validacion/validadores/${c.dataset.validador}`, { validador: c.checked }))));
}

/** HU-30: resultados de la Escala SUS y de la rúbrica de juicio experto (promedios y cada respuesta). */
async function resultadosValidacion(caja, ctx) {
  const r = await api.get('/api/chatbot/validacion/resultados');
  if (!ctx.vigente()) return;
  const ins = r.instrumentos;
  const titulo = html`<div class="tarjeta-titulo"><h2 id="t-resultados">Resultados de la validación</h2>
    <small class="ayuda">${r.sus.respuestas} Escala${r.sus.respuestas === 1 ? '' : 's'} SUS · ${r.rubrica.respuestas} rúbrica${r.rubrica.respuestas === 1 ? '' : 's'}</small></div>`;
  if (r.sus.respuestas === 0 && r.rubrica.respuestas === 0) {
    caja.innerHTML = html`${titulo}${vacio('Aún no hay respuestas', 'Los validadores responden la Escala SUS y la rúbrica desde su asistente.', 'inbox')}`.toString();
    return;
  }
  const rotulos = ins.itemsSus.map((_, i) => html`<th title="${ins.itemsSus[i]}">${i + 1}</th>`);
  caja.innerHTML = html`
    ${titulo}
    <h3 class="validacion-subtitulo">Escala SUS (usabilidad, 0 a 100)</h3>
    ${r.sus.respuestas === 0 ? html`<p class="ayuda">Nadie la ha respondido todavía.</p>` : html`
      <p><strong>Puntaje SUS promedio: ${promedio(r.sus.promedioPuntaje)}</strong> <small class="ayuda">(68 es el promedio de referencia de la escala; los ítems pares son negativos: ahí, un número bajo es bueno)</small></p>
      <div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>Validador</th><th>Fecha</th>${rotulos}<th>SUS</th></tr></thead>
        <tbody>${r.sus.detalle.map((d) => html`<tr><td>${d.validador.nombreCompleto}</td><td>${fechaHora(d.fecha)}</td>
          ${d.items.map((v) => html`<td>${v}</td>`)}<td><strong>${decimal(d.puntaje, 1)}</strong></td></tr>`)}
          <tr><td colspan="2"><strong>Promedio</strong></td>${r.sus.promedioPorItem.map((v) => html`<td>${promedio(v)}</td>`)}<td><strong>${promedio(r.sus.promedioPuntaje)}</strong></td></tr>
        </tbody>
      </table></div>`}
    <h3 class="validacion-subtitulo">Rúbrica de juicio experto (1 a 4)</h3>
    ${r.rubrica.respuestas === 0 ? html`<p class="ayuda">Nadie la ha respondido todavía.</p>` : html`
      <div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>Criterio</th><th>Promedio</th></tr></thead>
        <tbody>${ins.criterios.map((c, i) => html`<tr><td>${c.numero}. ${c.nombre}</td><td><strong>${promedio(r.rubrica.promedioPorCriterio[i])}</strong></td></tr>`)}</tbody>
      </table></div>
      ${r.rubrica.detalle.map((d) => html`<details class="validacion-detalle">
        <summary>${d.validador.nombreCompleto} · ${fechaHora(d.fecha)}</summary>
        <ul>${d.criterios.map((c) => html`<li><strong>${c.numero}. ${ins.criterios[c.numero - 1].nombre}: ${c.puntaje}</strong> (${ins.nivelesRubrica[c.puntaje - 1]})${c.comentario ? html` — ${c.comentario}` : ''}</li>`)}</ul>
        ${[d.fortalezas, d.debilidades, d.mejoras, d.recomendacion].map((t, i) => (t
          ? html`<p><strong>${ins.preguntasAbiertas[i]}</strong><br>${t}</p>` : ''))}
      </details>`)}`}`.toString();
}

export async function render(cont, ctx) {
  ctx.titulo('Configuración del asistente');
  let datos = await api.get('/api/chatbot/instrucciones');

  cont.innerHTML = html`
    ${cabecera({
      titulo: 'Configuración del asistente de planificación',
      subtitulo: 'Instrucciones con que el asistente retroalimenta las planificaciones de Práctica Pedagógica y Profesional: reglas, tono, expresiones a usar y a evitar, y respuestas modelo.',
    })}
    ${alerta('info', html`Los cambios rigen desde el <strong>siguiente mensaje</strong> de cualquier conversación; los mensajes ya guardados no cambian. El sistema agrega por su cuenta el contexto de cada estudiante (semestre, prácticas aprobadas, evaluaciones), y el texto viaja en cada mensaje: mientras más largo, más cuota del proveedor consume.`)}
    <form class="tarjeta" id="form-estilo" novalidate>
      <div data-alerta-form hidden></div>
      <p class="ayuda" id="estado-texto"></p>
      <label for="f-instrucciones">Instrucciones del asistente</label>
      <textarea id="f-instrucciones" class="estilo-texto" rows="26" maxlength="${datos.maximoCaracteres}" spellcheck="true"></textarea>
      <div class="estilo-pie">
        <small class="ayuda" id="contador" aria-live="polite"></small>
        <div class="estilo-acciones">
          <button type="button" class="btn btn-fantasma" id="btn-base">${icono('back')} Volver al texto base</button>
          <button type="button" class="btn" id="btn-descartar">Descartar cambios</button>
          <button type="submit" class="btn btn-primario" data-cargando="Guardando…">${icono('check')} Guardar</button>
        </div>
      </div>
    </form>
    <section class="tarjeta asistente-seccion" id="base-conocimiento" aria-labelledby="t-base"></section>
    <section class="tarjeta asistente-seccion" id="validacion-chatbot" aria-labelledby="t-validacion"></section>
    <section class="tarjeta asistente-seccion" id="resultados-validacion" aria-labelledby="t-resultados"></section>`.toString();
  await Promise.all([baseConocimiento(cont.querySelector('#base-conocimiento'), ctx),
    validacion(cont.querySelector('#validacion-chatbot'), ctx),
    resultadosValidacion(cont.querySelector('#resultados-validacion'), ctx)]);

  const form = cont.querySelector('#form-estilo');
  const area = form.querySelector('#f-instrucciones');
  const caja = form.querySelector('[data-alerta-form]');
  const estado = form.querySelector('#estado-texto');
  const contador = form.querySelector('#contador');
  const guardar = form.querySelector('button[type=submit]');

  const largo = () => area.value.trim().length;
  const pintarEstado = () => {
    estado.textContent = datos.editadoPor
      ? `Última edición: ${datos.editadoPor.nombreCompleto}, ${fechaHora(datos.fechaEdicion)}.${datos.esTextoBase ? ' Es igual al texto base.' : ''}`
      : 'Texto base de RetroPlani Mate (propuesta del coordinador de prácticas), sin editar.';
  };
  const pintarContador = () => {
    const n = largo();
    const fuera = n < datos.minimoCaracteres || n > datos.maximoCaracteres;
    contador.textContent = `${n.toLocaleString('es-CL')} de ${datos.maximoCaracteres.toLocaleString('es-CL')} caracteres`
      + (n < datos.minimoCaracteres ? ` (mínimo ${datos.minimoCaracteres})` : '')
      + (area.value !== datos.texto ? ' · cambios sin guardar' : '');
    contador.classList.toggle('texto-error', fuera);
  };
  const cargar = (texto) => { area.value = texto; pintarContador(); };

  pintarEstado();
  cargar(datos.texto);
  area.addEventListener('input', pintarContador);

  form.querySelector('#btn-descartar').addEventListener('click', () => { caja.hidden = true; cargar(datos.texto); });
  form.querySelector('#btn-base').addEventListener('click', async () => {
    const ok = await confirmar({
      titulo: 'Volver al texto base',
      mensaje: 'Se reemplazará el texto del editor por el texto base de RetroPlani Mate. No se guarda hasta que presiones «Guardar».',
      textoOk: 'Reemplazar',
    });
    if (ok) { caja.hidden = true; cargar(datos.textoBase); area.focus(); }
  });

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    caja.hidden = true;
    try {
      datos = await conCarga(guardar, () => api.put('/api/chatbot/instrucciones', { texto: area.value }));
      if (!ctx.vigente()) return;
      cargar(datos.texto);
      pintarEstado();
      toast('Instrucciones guardadas: rigen desde el próximo mensaje.');
    } catch (e) {
      if (e.status === 401) return;
      caja.innerHTML = alertaError(e).toString();
      caja.hidden = false;
    }
  });
}
