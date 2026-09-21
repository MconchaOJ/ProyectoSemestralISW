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
