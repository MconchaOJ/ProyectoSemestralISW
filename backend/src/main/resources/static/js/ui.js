// Utilidades de interfaz: plantillas seguras, iconos, toasts, modales, formularios y formatos.

// ---------- Plantillas seguras ----------
// html`...` escapa TODO valor interpolado (evita XSS con datos de la API). Para insertar HTML ya
// construido se usa otro html`...` anidado (o raw()).
class Seguro {
  constructor(texto) { this.texto = texto; }
  toString() { return this.texto; }
}
export const raw = (texto) => new Seguro(String(texto));

const ENTIDADES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
export const esc = (valor) => String(valor).replace(/[&<>"']/g, (c) => ENTIDADES[c]);

function convertir(valor) {
  if (valor instanceof Seguro) return valor.texto;
  if (Array.isArray(valor)) return valor.map(convertir).join('');
  if (valor === null || valor === undefined || valor === false) return '';
  return esc(valor);
}

export function html(cadenas, ...valores) {
  let salida = cadenas[0];
  valores.forEach((v, i) => { salida += convertir(v) + cadenas[i + 1]; });
  return new Seguro(salida);
}

// ---------- Iconos (trazos tipo Feather, incluidos en línea: sin librerías externas) ----------
const ICONOS = {
  home: '<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>',
  user: '<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
  users: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  lock: '<rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>',
  mail: '<path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/>',
  eye: '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>',
  'eye-off': '<path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/>',
  clipboard: '<path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>',
  check: '<polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>',
  'check-circle': '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>',
  file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>',
  clock: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
  chat: '<path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>',
  mic: '<path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/>',
  chart: '<line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>',
  logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>',
  menu: '<line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/>',
  plus: '<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>',
  search: '<circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>',
  x: '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>',
  send: '<line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>',
  edit: '<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>',
  shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
  calendar: '<rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>',
  book: '<path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>',
  sliders: '<line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/><line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/><line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/><line x1="1" y1="14" x2="7" y2="14"/><line x1="9" y1="8" x2="15" y2="8"/><line x1="17" y1="16" x2="23" y2="16"/>',
  upload: '<polyline points="16 16 12 12 8 16"/><line x1="12" y1="12" x2="12" y2="21"/><path d="M20.39 18.39A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.3"/>',
  download: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>',
  bell: '<path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/>',
  award: '<circle cx="12" cy="8" r="7"/><polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"/>',
  chevron: '<polyline points="6 9 12 15 18 9"/>',
  back: '<line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/>',
  alert: '<path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>',
  info: '<circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/>',
  inbox: '<polyline points="22 12 16 12 14 15 10 15 8 12 2 12"/><path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/>',
  layers: '<polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/>',
  key: '<path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.78 7.78 5.5 5.5 0 0 1 7.78-7.78zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/>',
  graduation: '<path d="M22 10L12 5 2 10l10 5 10-5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/>',
};

export function icono(nombre, clase = '') {
  const trazos = ICONOS[nombre] ?? ICONOS.info;
  return raw(`<svg class="icono ${esc(clase)}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${trazos}</svg>`);
}

/** Reemplaza cada elemento [data-icono="nombre"] por su SVG. */
export function pintarIconos(raiz = document) {
  raiz.querySelectorAll('[data-icono]').forEach((el) => {
    el.innerHTML = icono(el.dataset.icono).toString();
    el.removeAttribute('data-icono');
  });
}

// ---------- Catálogos de texto ----------
export const ROLES = {
  ESTUDIANTE: 'Estudiante',
  PROFESOR_GUIA: 'Profesor guía',
  PROFESOR_CURSO: 'Profesor del curso',
  COORDINADOR: 'Coordinador',
};
export const nombreRol = (rol) => ROLES[rol] ?? rol;

const ESTADOS = {
  BORRADOR: 'Borrador',
  EN_REVISION_GUIA: 'En revisión del guía',
  CON_OBSERVACIONES: 'Con observaciones',
  EN_AUTORIZACION: 'En autorización',
  AUTORIZADA: 'Autorizada',
  EN_CURSO: 'En curso',
  APROBADA: 'Aprobada',
  REPROBADA: 'Reprobada',
  FINALIZADA: 'Finalizada',
  OBSERVADA: 'Observada',
};
export const nombreEstado = (estado) => ESTADOS[estado] ?? estado;

export function badge(estado) {
  return html`<span class="badge badge-${estado}" title="${estado}">${nombreEstado(estado)}</span>`;
}

export const MODALIDADES = { INDIVIDUAL: 'Individual', DUPLA: 'Dupla', GRUPO: 'Grupo' };
export const CICLOS = { INICIAL: 'Ciclo inicial', INTERMEDIO: 'Ciclo intermedio', AVANZADO: 'Ciclo avanzado' };

// ---------- Formatos ----------
export function fecha(valor) {
  if (!valor) return '—';
  const [a, m, d] = String(valor).slice(0, 10).split('-');
  return a && m && d ? `${d}-${m}-${a}` : String(valor);
}
export function fechaHora(valor) {
  if (!valor) return '—';
  const [dia, hora] = String(valor).split('T');
  return `${fecha(dia)} ${(hora ?? '').slice(0, 5)}`.trim();
}
export const hora = (valor) => (valor ? String(valor).slice(0, 5) : '—');
export function decimal(valor, decimales = 1) {
  if (valor === null || valor === undefined || valor === '') return '—';
  return Number(valor).toFixed(decimales).replace('.', ',');
}
export const nota = (valor) => decimal(valor, 1);
export const promedio = (valor) => decimal(valor, 2);

// ---------- Cabecera de vista, cargando y vacíos ----------
export function cabecera({ titulo, subtitulo = '', hu = '', acciones = '' }) {
  return html`
    <header class="vista-cabecera">
      <div>
        ${hu ? html`<span class="chip-hu">${hu}</span>` : ''}
        <h1>${titulo}</h1>
        ${subtitulo ? html`<p class="subtitulo">${subtitulo}</p>` : ''}
      </div>
      ${acciones ? html`<div class="vista-acciones">${acciones}</div>` : ''}
    </header>`;
}

export const cargando = (texto = 'Cargando…') =>
  html`<div class="estado-carga" role="status"><span class="cargando"></span><span>${texto}</span></div>`;

export const vacio = (titulo, texto = '', ico = 'inbox') =>
  html`<div class="vacio">${icono(ico)}<p><strong>${titulo}</strong></p>${texto ? html`<p>${texto}</p>` : ''}</div>`;

// ---------- Errores de la API (400, 401, 403, 404, 409...) ----------
/** Convierte un error de la API en { texto, detalles[] } listo para mostrar. */
export function describirError(e) {
  const estado = e?.status;
  const detalles = (e?.errores ?? []).map((x) => `${x.campo}: ${x.mensaje}`);
  if (estado === 400) return { texto: e.message || 'Hay datos que corregir.', detalles };
  if (estado === 403) return { texto: 'No tienes permiso para esta acción.', detalles: e.message && !/rol no tiene permiso/i.test(e.message) ? [e.message] : [] };
  if (estado === 404) return { texto: e.message || 'No se encontró lo que buscas.', detalles: [] };
  if (estado === 409) return { texto: e.message || 'La acción no se puede realizar en el estado actual.', detalles: [] };
  if (estado === 401) return { texto: 'Tu sesión expiró. Ingresa nuevamente.', detalles: [] };
  return { texto: e?.message || 'Ocurrió un error inesperado.', detalles: [] };
}

export function alertaError(e) {
  const { texto, detalles } = describirError(e);
  return html`<div class="alerta alerta-error" role="alert">${icono('alert')}<div><strong>${texto}</strong>${detalles.length ? html`<ul>${detalles.map((d) => html`<li>${d}</li>`)}</ul>` : ''}</div></div>`;
}

export function alerta(tipo, contenido) {
  const ico = { error: 'alert', aviso: 'alert', info: 'info', ok: 'check-circle' }[tipo] ?? 'info';
  return html`<div class="alerta alerta-${tipo}" role="${tipo === 'error' ? 'alert' : 'status'}">${icono(ico)}<div>${contenido}</div></div>`;
}

/** Muestra un error de la API como toast (para acciones sueltas, como un botón de la tabla). */
export function toastError(e) {
  const { texto, detalles } = describirError(e);
  toast(detalles.length ? `${texto} ${detalles.join(' · ')}` : texto, 'error');
}

// ---------- Toasts ----------
export function toast(mensaje, tipo = 'ok') {
  let caja = document.getElementById('toasts');
  if (!caja) {
    caja = document.createElement('div');
    caja.id = 'toasts';
    caja.setAttribute('aria-live', 'polite');
    document.body.appendChild(caja);
  }
  const el = document.createElement('div');
  el.className = `toast toast-${tipo}`;
  el.innerHTML = html`${icono(tipo === 'error' ? 'alert' : 'check-circle')}<span>${mensaje}</span>`.toString();
  caja.appendChild(el);
  setTimeout(() => el.remove(), tipo === 'error' ? 7000 : 4000);
}

// ---------- Modales ----------
/**
 * Abre un modal. `cuerpo` y `pie` son html``. Devuelve { el, cerrar }. Esc y clic fuera cierran.
 */
export function abrirModal({ titulo, cuerpo, pie = '', chico = false }) {
  const fondo = document.createElement('div');
  fondo.className = 'modal-fondo';
  fondo.innerHTML = html`
    <div class="modal ${chico ? 'modal-chico' : ''}" role="dialog" aria-modal="true" aria-label="${titulo}">
      <div class="modal-cabecera"><h2>${titulo}</h2>
        <button type="button" class="btn btn-fantasma btn-chico" data-cerrar aria-label="Cerrar">${icono('x')}</button></div>
      <div class="modal-cuerpo">${cuerpo}</div>
      ${pie ? html`<div class="modal-pie">${pie}</div>` : ''}
    </div>`.toString();
  const anterior = document.activeElement;
  const cerrar = () => {
    document.removeEventListener('keydown', alTeclear);
    fondo.remove();
    if (anterior && anterior.focus) anterior.focus();
  };
  const alTeclear = (ev) => { if (ev.key === 'Escape') cerrar(); };
  document.addEventListener('keydown', alTeclear);
  fondo.addEventListener('mousedown', (ev) => { if (ev.target === fondo) cerrar(); });
  fondo.querySelectorAll('[data-cerrar]').forEach((b) => b.addEventListener('click', cerrar));
  document.body.appendChild(fondo);
  const primero = fondo.querySelector('input:not([type=hidden]), select, textarea');
  (primero ?? fondo.querySelector('.modal')).focus?.();
  return { el: fondo, cerrar };
}

/** Confirmación: devuelve una promesa con true (aceptó) o false. */
export function confirmar({ titulo, mensaje, textoOk = 'Aceptar', peligro = false }) {
  return new Promise((resolver) => {
    const m = abrirModal({
      titulo, chico: true,
      cuerpo: html`<p>${mensaje}</p>`,
      pie: html`<button type="button" class="btn" data-no>Cancelar</button>
        <button type="button" class="btn ${peligro ? 'btn-peligro' : 'btn-primario'}" data-si>${textoOk}</button>`,
    });
    let respondido = false;
    const responder = (v) => { if (!respondido) { respondido = true; resolver(v); } m.cerrar(); };
    m.el.querySelector('[data-si]').addEventListener('click', () => responder(true));
    m.el.querySelector('[data-no]').addEventListener('click', () => responder(false));
    m.el.querySelector('[data-cerrar]').addEventListener('click', () => responder(false));
    m.el.addEventListener('mousedown', (ev) => { if (ev.target === m.el) responder(false); });
    m.el.querySelector('[data-si]').focus();
  });
}

// ---------- Botones con estado de carga ----------
/** Ejecuta `accion` con el botón bloqueado y un spinner. Propaga el resultado o el error. */
export async function conCarga(boton, accion) {
  if (!boton) return accion();
  if (boton.getAttribute('aria-busy') === 'true') return undefined;
  const original = boton.innerHTML;
  boton.setAttribute('aria-busy', 'true');
  boton.disabled = true;
  boton.innerHTML = `<span class="cargando"></span> ${boton.dataset.cargando ?? 'Procesando…'}`;
  try {
    return await accion();
  } finally {
    boton.removeAttribute('aria-busy');
    boton.disabled = false;
    boton.innerHTML = original;
  }
}

// ---------- Formularios ----------
/**
 * Genera un campo de formulario.
 * tipo: text | email | password | number | date | time | textarea | select
 * opciones (select): [{ valor, texto }]
 */
export function campo({ nombre, etiqueta, tipo = 'text', valor = '', requerido = false, ayuda = '', opciones = null,
  completo = false, atributos = {}, vacioTexto = 'Seleccione…' }) {
  const attrs = Object.entries(atributos).map(([k, v]) => (v === true ? k : `${k}="${esc(v)}"`)).join(' ');
  const id = `f-${nombre}`;
  let control;
  if (tipo === 'select') {
    control = html`<select id="${id}" name="${nombre}" ${raw(attrs)} ${requerido ? raw('required') : ''}>
      <option value="">${vacioTexto}</option>
      ${(opciones ?? []).map((o) => html`<option value="${o.valor}" ${String(o.valor) === String(valor) ? raw('selected') : ''}>${o.texto}</option>`)}
    </select>`;
  } else if (tipo === 'textarea') {
    control = html`<textarea id="${id}" name="${nombre}" ${raw(attrs)} ${requerido ? raw('required') : ''}>${valor ?? ''}</textarea>`;
  } else {
    control = html`<input id="${id}" name="${nombre}" type="${tipo}" value="${valor ?? ''}" ${raw(attrs)} ${requerido ? raw('required') : ''}>`;
  }
  return html`<div class="campo ${completo ? 'completo' : ''}">
    <label for="${id}">${etiqueta}${requerido ? html` <span class="req" aria-hidden="true">*</span>` : ''}</label>
    ${control}
    ${ayuda ? html`<span class="ayuda">${ayuda}</span>` : ''}
    <span class="error-campo" data-error-de="${nombre}" role="alert" hidden></span>
  </div>`;
}

function ponerError(form, nombre, mensaje) {
  const control = form.elements[nombre];
  const caja = form.querySelector(`[data-error-de="${nombre}"]`);
  if (!caja) return false;
  caja.textContent = mensaje;
  caja.hidden = false;
  control?.classList?.add('invalido');
  return true;
}

export function limpiarErrores(form) {
  form.querySelectorAll('[data-error-de]').forEach((c) => { c.hidden = true; c.textContent = ''; });
  form.querySelectorAll('.invalido').forEach((c) => c.classList.remove('invalido'));
}

/** Validación en español antes de enviar. Marca cada campo y enfoca el primero con error. */
export function validarForm(form) {
  limpiarErrores(form);
  let primero = null;
  for (const el of form.elements) {
    if (!el.name || el.disabled || el.type === 'button' || el.type === 'submit') continue;
    const valor = (el.value ?? '').trim();
    let mensaje = '';
    if (el.required && !valor) mensaje = 'Este campo es obligatorio.';
    else if (valor && el.type === 'email' && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(valor)) mensaje = 'No es un correo electrónico válido.';
    else if (valor && el.pattern && !new RegExp(`^(?:${el.pattern})$`).test(valor)) mensaje = el.title || 'El formato no es válido.';
    else if (valor && el.minLength > 0 && valor.length < el.minLength) mensaje = `Debe tener al menos ${el.minLength} caracteres.`;
    else if (valor && el.type === 'number' && (Number.isNaN(Number(valor)) || (el.min !== '' && Number(valor) < Number(el.min)) || (el.max !== '' && Number(valor) > Number(el.max)))) {
      mensaje = el.min !== '' && el.max !== '' ? `Debe ser un número entre ${el.min} y ${el.max}.` : 'Ingresa un número válido.';
    }
    if (mensaje) {
      ponerError(form, el.name, mensaje);
      primero ??= el;
    }
  }
  primero?.focus();
  return primero === null;
}

/** Datos del formulario: texto recortado; vacío = null; number = Number. */
export function datosForm(form) {
  const datos = {};
  for (const el of form.elements) {
    if (!el.name || el.disabled || el.type === 'button' || el.type === 'submit') continue;
    if (el.type === 'radio' && !el.checked) continue;
    const v = (el.value ?? '').trim();
    if (v === '') datos[el.name] = null;
    else if (el.type === 'number' || el.dataset.numero !== undefined) datos[el.name] = Number(v);
    else datos[el.name] = v;
  }
  return datos;
}

/**
 * Lleva los errores de campo que devolvió el servidor (400) a cada input.
 * Devuelve los que no pudo asociar a un campo, para mostrarlos en una alerta.
 */
export function errorDeServidor(form, e) {
  limpiarErrores(form);
  const sueltos = [];
  let primero = null;
  (e.errores ?? []).forEach((x) => {
    if (ponerError(form, x.campo, x.mensaje)) primero ??= form.elements[x.campo];
    else sueltos.push(`${x.campo}: ${x.mensaje}`);
  });
  primero?.focus();
  return { sueltos, asociados: primero !== null };
}

/** Cajita de alerta de un formulario (data-alerta-form). */
export function alertaForm(form, e) {
  const caja = form.querySelector('[data-alerta-form]');
  if (!caja) return;
  const { sueltos, asociados } = errorDeServidor(form, e);
  const { texto } = describirError(e);
  const detalles = sueltos;
  caja.innerHTML = html`<div class="alerta alerta-error" role="alert">${icono('alert')}<div><strong>${asociados && e.status === 400 ? 'Revisa los campos marcados.' : texto}</strong>${detalles.length ? html`<ul>${detalles.map((d) => html`<li>${d}</li>`)}</ul>` : ''}</div></div>`.toString();
  caja.hidden = false;
}

export function limpiarAlertaForm(form) {
  const caja = form.querySelector('[data-alerta-form]');
  if (caja) { caja.hidden = true; caja.innerHTML = ''; }
}

export const cajaAlertaForm = () => html`<div data-alerta-form hidden></div>`;

// ---------- Otros ----------
export function debounce(fn, ms = 300) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}

export const nombreCompleto = (u) => (u ? `${u.nombre ?? ''} ${u.apellidos ?? ''}`.trim() : '');
export const iniciales = (u) => `${(u?.nombre ?? '?')[0]}${(u?.apellidos ?? '')[0] ?? ''}`.toUpperCase();

// ---------- Modal con formulario ----------
/**
 * Modal con un formulario ya validado. `campos` es una lista de campo(...). `alEnviar(datos)` hace la llamada
 * a la API (si falla, el error se muestra dentro del modal); `alListo(resultado)` corre al terminar bien.
 */
export function modalFormulario({ titulo, campos, textoOk = 'Guardar', textoCarga = 'Guardando…', chico = false,
  introduccion = '', alEnviar, alListo }) {
  const id = `form-${Math.random().toString(36).slice(2, 8)}`;
  const m = abrirModal({
    titulo, chico,
    cuerpo: html`<form id="${id}" novalidate>
      ${introduccion}
      <div data-alerta-form hidden></div>
      <div class="rejilla-form">${campos}</div>
    </form>`,
    pie: html`<button type="button" class="btn" data-cerrar>Cancelar</button>
      <button type="submit" form="${id}" class="btn btn-primario" data-cargando="${textoCarga}">${textoOk}</button>`,
  });
  const form = m.el.querySelector('form');
  enlazarFormulario(form, m.el.querySelector('button[type=submit]'), {
    alEnviar, alListo: async (r) => { m.cerrar(); if (alListo) await alListo(r); },
  });
  return { ...m, form };
}

/**
 * Conecta un formulario ya pintado: valida en español, arma los datos, llama a `alEnviar(datos)` con el botón en
 * estado de carga y, si la API devuelve un error, lo muestra en el propio formulario (data-alerta-form).
 */
export function enlazarFormulario(form, boton, { alEnviar, alListo }) {
  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    limpiarAlertaForm(form);
    if (!validarForm(form)) return;
    try {
      const resultado = await conCarga(boton, () => alEnviar(datosForm(form)));
      if (alListo) await alListo(resultado);
    } catch (e) {
      if (e.status !== 401) alertaForm(form, e);
    }
  });
}
