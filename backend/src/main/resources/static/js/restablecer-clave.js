// Restablecer la clave con el enlace del correo (S-97 a S-99): lee el token de la URL y llama a
// POST /api/auth/restablecer-clave. Si el enlace no sirve, ofrece pedir uno nuevo.
import { api, sesion } from './api.js';
import { alerta, conCarga, html, icono, pintarIconos } from './ui.js';

pintarIconos();

const form = document.getElementById('form-restablecer');
const caja = document.getElementById('alerta');
const nueva = document.getElementById('nueva');
const confirmacion = document.getElementById('confirmacion');
const toggle = document.getElementById('toggle-clave');
const boton = document.getElementById('btn-guardar');

const token = new URLSearchParams(location.search).get('token')?.trim() ?? '';
// Se saca el token de la barra de direcciones y del historial apenas se lee
history.replaceState(null, '', location.pathname);

function mostrar(tipo, contenido) {
  caja.innerHTML = alerta(tipo, contenido).toString();
  caja.hidden = false;
}

function enlaceInvalido() {
  form.hidden = true;
  mostrar('error', html`El enlace para restablecer la clave no es válido, ya se usó o venció.
    <br><a href="index.html?recuperar=1">Pedir un enlace nuevo</a>`);
}

function errorCampo(id, texto) {
  const el = document.getElementById(`error-${id}`);
  el.textContent = texto;
  el.hidden = !texto;
  document.getElementById(id).classList.toggle('invalido', Boolean(texto));
}

if (!token) {
  enlaceInvalido();
}

toggle.innerHTML = icono('eye').toString();
toggle.addEventListener('click', () => {
  const visible = nueva.type === 'password';
  nueva.type = confirmacion.type = visible ? 'text' : 'password';
  toggle.innerHTML = icono(visible ? 'eye-off' : 'eye').toString();
  toggle.setAttribute('aria-pressed', String(visible));
  toggle.setAttribute('aria-label', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
  nueva.focus();
});

[nueva, confirmacion].forEach((el) => el.addEventListener('input', () => errorCampo(el.id, '')));

form.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  caja.hidden = true;
  const clave = nueva.value;

  // Misma regla que HU-26 (el servidor la vuelve a revisar)
  const errorNueva = !clave ? 'Ingresa la clave nueva.'
    : clave.length < 8 ? 'Debe tener al menos 8 caracteres.'
      : clave.length > 72 ? 'Debe tener como máximo 72 caracteres.' : '';
  const errorConf = !errorNueva && confirmacion.value !== clave ? 'Las dos claves no coinciden.' : '';
  errorCampo('nueva', errorNueva);
  errorCampo('confirmacion', errorConf);
  if (errorNueva || errorConf) {
    (errorNueva ? nueva : confirmacion).focus();
    return;
  }

  boton.dataset.cargando = 'Guardando…';
  try {
    await conCarga(boton, () => api.restablecerClave(token, clave));
    form.hidden = true;
    sesion.limpiar();
    mostrar('ok', html`Tu clave quedó cambiada. <a href="index.html">Ingresar con la clave nueva</a>`);
  } catch (e) {
    const campoClave = e.errores?.some((x) => x.campo === 'nuevaClave');
    if (e.status === 400 && campoClave) errorCampo('nueva', 'Debe tener entre 8 y 72 caracteres.');
    else if (e.status === 400) enlaceInvalido();
    else mostrar('error', e.message || 'No se pudo cambiar la clave.');
  }
});
