// Sección "Retroalimentación en voz" + "Transcripción a texto" (Fase E, punto 4; HU-11 y HU-12).
// Se monta entre la tarjeta de puntaje/promedio y "Indicadores de la pauta" en el detalle de una evaluación.
// SUPUESTO S-40: la transcripción inicial la genera el propio navegador (Web Speech API), no un servicio de pago.
import { api } from './api.js';
import { alerta, alertaError, conCarga, fechaHora, html, icono, toast } from './ui.js';

const soportaGrabacion = () => Boolean(window.MediaRecorder) && Boolean(navigator.mediaDevices?.getUserMedia);
const soportaReconocimiento = () => Boolean(window.SpeechRecognition || window.webkitSpeechRecognition);

/**
 * @param {{evaluacionId: number|string, esEvaluador: boolean}} opciones esEvaluador: el profesor guía dueño de
 *   esta evaluación (el único que puede grabar, subir o corregir la transcripción).
 */
export async function montarNotaVoz(cont, { evaluacionId, esEvaluador }) {
  cont.innerHTML = '';
  let nota = null;
  try {
    nota = await api.get(`/api/evaluaciones/${evaluacionId}/nota-voz`);
  } catch (e) {
    if (e.status !== 404) { cont.innerHTML = alertaError(e).toString(); return; }
  }

  if (nota) return pintarExistente(cont, { evaluacionId, esEvaluador, nota });
  if (esEvaluador) return pintarGrabador(cont, { evaluacionId });
  cont.innerHTML = html`<div class="tarjeta nota-voz">
    <h2>${icono('mic')} Retroalimentación en voz</h2>
    <p class="subtitulo">El profesor guía todavía no ha dejado una nota de voz para esta evaluación.</p>
  </div>`.toString();
}

// ---------- Ya existe una nota de voz: reproducirla, leerla y (si eres el evaluador) corregirla ----------
function pintarExistente(cont, { evaluacionId, esEvaluador, nota }) {
  cont.innerHTML = html`
    <div class="tarjeta nota-voz">
      <h2>${icono('mic')} Retroalimentación en voz</h2>
      <audio controls id="audio-nota" preload="none"></audio>
      <p class="subtitulo" id="estado-audio">Cargando el audio…</p>
      <h3>Transcripción a texto</h3>
      ${esEvaluador
        ? html`<textarea id="texto-transcripcion" rows="4" maxlength="8000">${nota.transcripcion ?? ''}</textarea>
            <div class="acciones-panel"><button type="button" class="btn btn-primario" id="guardar-transcripcion" data-cargando="Guardando…">Guardar transcripción corregida</button></div>`
        : html`<p class="transcripcion-lectura">${nota.transcripcion || 'Sin transcripción.'}</p>`}
      <p class="subtitulo">${nota.transcripcionCorregida ? 'Transcripción corregida por el evaluador.' : 'Transcripción automática, sin corregir.'}
        · grabada el ${fechaHora(nota.fechaGrabacion)} (${nota.duracionSegundos}s)</p>
      <div id="avisos-nota-voz"></div>
    </div>`.toString();

  const audio = cont.querySelector('#audio-nota');
  const estadoAudio = cont.querySelector('#estado-audio');
  api.archivo(`/api/evaluaciones/${evaluacionId}/nota-voz/audio`).then(({ blob }) => {
    audio.src = URL.createObjectURL(blob);
    estadoAudio.remove();
  }).catch(() => { estadoAudio.textContent = 'No se pudo cargar el audio.'; });

  if (!esEvaluador) return;
  const avisos = cont.querySelector('#avisos-nota-voz');
  const boton = cont.querySelector('#guardar-transcripcion');
  boton.addEventListener('click', async () => {
    avisos.innerHTML = '';
    const texto = cont.querySelector('#texto-transcripcion').value.trim();
    if (!texto) { avisos.innerHTML = alerta('error', 'La transcripción corregida no puede quedar vacía.').toString(); return; }
    try {
      await conCarga(boton, () => api.put(`/api/evaluaciones/${evaluacionId}/nota-voz`, { transcripcion: texto }));
      toast('Transcripción corregida guardada.');
      await montarNotaVoz(cont, { evaluacionId, esEvaluador });
    } catch (e) {
      if (e.status !== 401) avisos.innerHTML = alertaError(e).toString();
    }
  });
}

// ---------- Todavía no hay nota de voz y eres el evaluador: grabarla ----------
function pintarGrabador(cont, { evaluacionId }) {
  const puedeGrabar = soportaGrabacion();
  const puedeTranscribir = soportaReconocimiento();

  cont.innerHTML = html`
    <div class="tarjeta nota-voz">
      <h2>${icono('mic')} Retroalimentación en voz</h2>
      ${!puedeGrabar
        ? alerta('aviso', 'Tu navegador no permite grabar audio desde esta página (prueba con Chrome de escritorio). El resto de la evaluación sigue funcionando con normalidad.')
        : html`<div class="grabador">
            <button type="button" class="btn btn-primario" id="grabar">${icono('mic')} Grabar nota de voz</button>
            <button type="button" class="btn btn-peligro" id="detener" hidden>${icono('x')} Detener grabación</button>
            <span id="estado-grabacion"></span>
          </div>
          <audio controls id="audio-previa" hidden></audio>`}
      <h3>Transcripción a texto</h3>
      ${!puedeTranscribir ? html`<p class="subtitulo">Tu navegador no transcribe automáticamente: escribe el texto a mano.</p>` : ''}
      <textarea id="texto-transcripcion" rows="4" maxlength="8000"
        placeholder="${puedeTranscribir ? 'Aparece aquí mientras grabas; puedes corregirla antes de guardar.' : 'Escribe aquí la retroalimentación de la nota de voz.'}"></textarea>
      <div class="acciones-panel">
        <button type="button" class="btn btn-ok" id="guardar-nota-voz" data-cargando="Guardando…" disabled>${icono('check-circle')} Guardar nota de voz</button>
      </div>
      <div id="avisos-nota-voz"></div>
    </div>`.toString();

  const avisos = cont.querySelector('#avisos-nota-voz');
  const texto = cont.querySelector('#texto-transcripcion');
  const btnGuardar = cont.querySelector('#guardar-nota-voz');
  let blobFinal = null;
  let segundos = 0;

  if (puedeGrabar) {
    const btnGrabar = cont.querySelector('#grabar');
    const btnDetener = cont.querySelector('#detener');
    const estado = cont.querySelector('#estado-grabacion');
    const audioPrevia = cont.querySelector('#audio-previa');
    let mediaRecorder = null;
    let chunks = [];
    let cronometro = null;
    let reconocimiento = null;

    btnGrabar.addEventListener('click', async () => {
      avisos.innerHTML = '';
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        chunks = [];
        segundos = 0;
        blobFinal = null;
        btnGuardar.disabled = true;
        mediaRecorder = new MediaRecorder(stream);
        mediaRecorder.ondataavailable = (ev) => { if (ev.data.size > 0) chunks.push(ev.data); };
        mediaRecorder.onstop = () => {
          blobFinal = new Blob(chunks, { type: mediaRecorder.mimeType || 'audio/webm' });
          audioPrevia.src = URL.createObjectURL(blobFinal);
          audioPrevia.hidden = false;
          btnGuardar.disabled = false;
          stream.getTracks().forEach((t) => t.stop());
        };
        mediaRecorder.start();
        estado.textContent = 'Grabando… 0s';
        cronometro = setInterval(() => { segundos += 1; estado.textContent = `Grabando… ${segundos}s`; }, 1000);
        btnGrabar.hidden = true;
        btnDetener.hidden = false;

        if (puedeTranscribir) {
          const Reconocedor = window.SpeechRecognition || window.webkitSpeechRecognition;
          reconocimiento = new Reconocedor();
          reconocimiento.lang = 'es-CL';
          reconocimiento.continuous = true;
          reconocimiento.interimResults = true;
          let definitivo = '';
          reconocimiento.onresult = (ev) => {
            let interino = '';
            for (let i = ev.resultIndex; i < ev.results.length; i += 1) {
              if (ev.results[i].isFinal) definitivo += `${ev.results[i][0].transcript} `;
              else interino += ev.results[i][0].transcript;
            }
            texto.value = (definitivo + interino).trim();
          };
          reconocimiento.onerror = () => { /* el audio se sigue grabando igual; el texto se puede escribir a mano */ };
          reconocimiento.start();
        }
      } catch {
        avisos.innerHTML = alerta('error', 'No se pudo acceder al micrófono. Revisa los permisos del navegador para este sitio.').toString();
      }
    });

    btnDetener.addEventListener('click', () => {
      clearInterval(cronometro);
      estado.textContent = `Grabación detenida (${segundos}s). Puedes escucharla abajo antes de guardar.`;
      btnDetener.hidden = true;
      btnGrabar.hidden = false;
      btnGrabar.textContent = '';
      btnGrabar.innerHTML = `${icono('mic')} Grabar de nuevo`;
      mediaRecorder?.stop();
      reconocimiento?.stop();
    });
  }

  btnGuardar.addEventListener('click', async () => {
    avisos.innerHTML = '';
    if (!blobFinal) { avisos.innerHTML = alerta('error', 'Graba primero la nota de voz.').toString(); return; }
    if (!texto.value.trim()) { avisos.innerHTML = alerta('error', 'Escribe o corrige la transcripción antes de guardar.').toString(); return; }
    const datos = new FormData();
    datos.append('audio', blobFinal, 'nota-voz.webm');
    datos.append('transcripcion', texto.value.trim());
    datos.append('duracionSegundos', String(Math.max(1, segundos)));
    btnGuardar.disabled = true;
    const original = btnGuardar.innerHTML;
    btnGuardar.innerHTML = `<span class="cargando"></span> ${btnGuardar.dataset.cargando}`;
    try {
      await api.subir(`/api/evaluaciones/${evaluacionId}/nota-voz`, datos);
      toast('Nota de voz guardada.');
      await montarNotaVoz(cont, { evaluacionId, esEvaluador: true });
    } catch (e) {
      if (e.status !== 401) avisos.innerHTML = alertaError(e).toString();
      btnGuardar.disabled = false;
      btnGuardar.innerHTML = original;
    }
  });
}
