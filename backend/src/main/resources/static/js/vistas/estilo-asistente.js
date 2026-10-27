// HU-17 · El coordinador calibra el estilo de retroalimentación del asistente de planificación (chatbot).
//   #/estilo-asistente: un solo texto para todo el sistema (reglas, tono, expresiones y respuestas modelo).
//   El contexto de cada estudiante (semestre, prácticas aprobadas) lo agrega el sistema: no se edita aquí.
// HU-29 · En la misma pantalla, la base de conocimiento (documentos de referencia, uno por tipo).
// HU-30 · Y la validación: con ella abierta, el asistente solo responde a los estudiantes validadores.
import { api } from '../api.js';
import {
  alerta, alertaError, cabecera, confirmar, conCarga, fechaHora, html, icono, toast, toastError, vacio,
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
 * asistente. La Escala SUS y la rúbrica de los jueces se responden fuera del sistema.
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
    <p class="ayuda">La Escala SUS y la rúbrica de los jueces expertos se responden fuera del sistema.</p>`.toString();

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
    <section class="tarjeta asistente-seccion" id="validacion-chatbot" aria-labelledby="t-validacion"></section>`.toString();
  await Promise.all([baseConocimiento(cont.querySelector('#base-conocimiento'), ctx),
    validacion(cont.querySelector('#validacion-chatbot'), ctx)]);

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
