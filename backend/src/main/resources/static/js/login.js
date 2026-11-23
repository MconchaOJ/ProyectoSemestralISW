// Pantalla de login (HU-01): pide el token a POST /api/auth/login y lo guarda en sessionStorage.
import { api, sesion } from './api.js';
import { alerta, conCarga, icono, pintarIconos } from './ui.js';

// Si ya hay una sesión vigente no hace falta volver a ingresar.
if (sesion.vigente()) {
  location.replace('app.html');
}

pintarIconos();

const form = document.getElementById('form-login');
const caja = document.getElementById('alerta');
const email = document.getElementById('email');
const clave = document.getElementById('password');
const toggle = document.getElementById('toggle-clave');
const boton = document.getElementById('btn-ingresar');

toggle.innerHTML = icono('eye').toString();
toggle.addEventListener('click', () => {
  const visible = clave.type === 'password';
  clave.type = visible ? 'text' : 'password';
  toggle.innerHTML = icono(visible ? 'eye-off' : 'eye').toString();
  toggle.setAttribute('aria-pressed', String(visible));
  toggle.setAttribute('aria-label', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
  clave.focus();
});

if (new URLSearchParams(location.search).has('expirada')) {
  mostrar('info', 'Tu sesión expiró o ya no es válida. Ingresa nuevamente.');
}

function mostrar(tipo, texto) {
  caja.innerHTML = alerta(tipo, texto).toString();
  caja.hidden = false;
}

function errorCampo(id, texto) {
  const el = document.getElementById(`error-${id}`);
  el.textContent = texto;
  el.hidden = !texto;
  document.getElementById(id).classList.toggle('invalido', Boolean(texto));
}

[email, clave].forEach((el) => el.addEventListener('input', () => errorCampo(el.id, '')));

form.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  caja.hidden = true;
  const correo = email.value.trim();
  const password = clave.value;

  errorCampo('email', correo ? '' : 'Ingresa tu correo electrónico.');
  errorCampo('password', password ? '' : 'Ingresa tu contraseña.');
  if (!correo || !password) {
    (correo ? clave : email).focus();
    return;
  }

  boton.dataset.cargando = 'Ingresando…';
  try {
    await conCarga(boton, async () => {
      const respuesta = await api.login(correo, password);
      sesion.guardar(respuesta);
      location.replace('app.html#/inicio');
    });
  } catch (e) {
    if (e.status === 401) mostrar('error', 'Correo o contraseña incorrectos.');
    else if (e.status === 400) mostrar('error', 'Completa el correo y la contraseña.');
    else mostrar('error', e.message || 'No se pudo iniciar sesión.');
    clave.select();
  }
});

// Recuperación de la clave por correo (S-97 a S-99). El servidor responde siempre el mismo mensaje, exista o no
// el correo: la página tampoco confirma ni niega nada.
const olvide = document.getElementById('btn-olvide');
const panel = document.getElementById('panel-recuperar');
const formRecuperar = document.getElementById('form-recuperar');
const cajaRecuperar = document.getElementById('alerta-recuperar');
const emailRecuperar = document.getElementById('email-recuperar');
const botonRecuperar = document.getElementById('btn-recuperar');

function abrirRecuperar() {
  panel.hidden = false;
  olvide.setAttribute('aria-expanded', 'true');
  if (!emailRecuperar.value) emailRecuperar.value = email.value.trim();
  emailRecuperar.focus();
}

olvide.addEventListener('click', () => {
  if (panel.hidden) abrirRecuperar();
  else { panel.hidden = true; olvide.setAttribute('aria-expanded', 'false'); }
});

// restablecer-clave.html enlaza aquí con ?recuperar=1 cuando el enlace venció
if (new URLSearchParams(location.search).has('recuperar')) abrirRecuperar();

emailRecuperar.addEventListener('input', () => errorCampo('email-recuperar', ''));

formRecuperar.addEventListener('submit', async (ev) => {
  ev.preventDefault();
  cajaRecuperar.hidden = true;
  const correo = emailRecuperar.value.trim();
  errorCampo('email-recuperar', correo ? '' : 'Ingresa tu correo electrónico.');
  if (!correo) { emailRecuperar.focus(); return; }

  botonRecuperar.dataset.cargando = 'Enviando…';
  try {
    await conCarga(botonRecuperar, async () => {
      const r = await api.recuperarClave(correo);
      cajaRecuperar.innerHTML = alerta('ok', r?.mensaje || 'Si el correo está registrado, te enviamos un enlace.').toString();
      cajaRecuperar.hidden = false;
    });
  } catch (e) {
    cajaRecuperar.innerHTML = alerta('error', e.status === 400 ? 'Revisa el correo escrito.' : (e.message || 'No se pudo enviar la solicitud.')).toString();
    cajaRecuperar.hidden = false;
  }
});
