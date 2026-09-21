// Estructura de la aplicación: cabecera, barra lateral por rol y enrutador por hash (#/inicio, #/planificaciones...).
import { api, sesion } from './api.js';
import { alertaError, cargando, html, icono, iniciales, nombreRol, pintarIconos, vacio } from './ui.js';
import { PROXIMAMENTE, SPRINT1, itemActivo, rutaPermitida, visiblesPara } from './menu.js';

if (!sesion.vigente()) {
  sesion.limpiar();
  location.replace('index.html');
}

// Cada vista es un módulo aparte que se carga la primera vez que se visita.
const VISTAS = {
  inicio: () => import('./vistas/inicio.js'),
  cuenta: () => import('./vistas/cuenta.js'),
  estudiantes: () => import('./vistas/gestion.js'),
  profesores: () => import('./vistas/gestion.js'),
  inscripciones: () => import('./vistas/gestion.js'),
  evaluaciones: () => import('./vistas/evaluaciones.js'),
  planificaciones: () => import('./vistas/planificaciones.js'),
  historial: () => import('./vistas/historial.js'),
};

const $ = (id) => document.getElementById(id);
const principal = $('principal');
const menu = $('menu');
const movil = window.matchMedia('(max-width: 900px)');

let usuario = sesion.usuario();
let vez = 0; // identifica la navegación en curso: una vista lenta no pisa a la siguiente

// ---------- Cabecera ----------
function pintarCabecera() {
  $('usuario-nombre').textContent = `${usuario.nombre} ${usuario.apellidos}`;
  $('usuario-rol').textContent = nombreRol(usuario.rol);
  $('avatar').textContent = iniciales(usuario);
  $('ico-salir').innerHTML = icono('logout').toString();
  $('btn-lateral').innerHTML = icono('menu').toString();
}

$('btn-salir').addEventListener('click', () => {
  sesion.limpiar();
  location.replace('index.html');
});

// ---------- Barra lateral ----------
function itemHtml(item, activo) {
  if (item.sprint) { // historia futura: deshabilitada
    return html`<div class="item deshabilitado" aria-disabled="true" title="${item.hu} · ${item.nombre} (Sprint ${item.sprint})">
      <span class="item-ico">${icono(item.icono)}</span>
      <span class="item-texto"><span class="item-nombre">${item.nombre}</span><span class="item-hu">${item.hu} <span class="etiqueta-prox">Próximamente</span></span></span>
    </div>`;
  }
  return html`<a class="item ${activo ? 'activo' : ''}" href="#/${item.ruta}" data-ruta="${item.ruta}" title="${item.hu} · ${item.nombre}" ${activo ? html`aria-current="page"` : ''}>
    <span class="item-ico">${icono(item.icono)}</span>
    <span class="item-texto"><span class="item-nombre">${item.nombre}</span><span class="item-hu">${item.hu}</span></span>
  </a>`;
}

function pintarMenu() {
  const s1 = visiblesPara(SPRINT1, usuario.rol);
  const prox = visiblesPara(PROXIMAMENTE, usuario.rol);
  menu.innerHTML = html`
    <div class="grupo">
      <a class="item" href="#/inicio" data-ruta="inicio" title="Inicio">
        <span class="item-ico">${icono('home')}</span>
        <span class="item-texto"><span class="item-nombre">Inicio</span></span>
      </a>
    </div>
    <div class="grupo">
      <button type="button" class="grupo-titulo" aria-expanded="true">Funciones del Sprint 1 ${icono('chevron', 'plegable')}</button>
      <div class="grupo-items">${s1.map((i) => itemHtml(i, false))}</div>
    </div>
    ${prox.length ? html`<div class="grupo">
      <button type="button" class="grupo-titulo" aria-expanded="true">Próximamente ${icono('chevron', 'plegable')}</button>
      <div class="grupo-items">${prox.map((i) => itemHtml(i, false))}</div>
    </div>` : ''}`.toString();
}

menu.addEventListener('click', (ev) => {
  const titulo = ev.target.closest('.grupo-titulo');
  if (titulo) {
    const grupo = titulo.parentElement;
    grupo.classList.toggle('plegado');
    titulo.setAttribute('aria-expanded', String(!grupo.classList.contains('plegado')));
    return;
  }
  if (ev.target.closest('a.item')) cerrarMovil();
});

function marcarActivo(seccion) {
  const activo = seccion === 'inicio' ? { ruta: 'inicio' } : itemActivo(seccion, usuario.rol);
  menu.querySelectorAll('a.item').forEach((a) => {
    const es = Boolean(activo) && a.dataset.ruta === activo.ruta;
    a.classList.toggle('activo', es);
    if (es) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
  });
}

// Colapsar (escritorio) o abrir como cajón (móvil)
function leerPreferencia() {
  try { return localStorage.getItem('pem.lateral-chico') === '1'; } catch { return false; }
}
function guardarPreferencia(valor) {
  try { localStorage.setItem('pem.lateral-chico', valor ? '1' : '0'); } catch { /* sin almacenamiento */ }
}
function cerrarMovil() {
  document.body.classList.remove('lateral-abierto');
  $('btn-lateral').setAttribute('aria-expanded', String(!movil.matches && !document.body.classList.contains('lateral-chico')));
}
$('btn-lateral').addEventListener('click', () => {
  if (movil.matches) {
    const abierto = document.body.classList.toggle('lateral-abierto');
    $('btn-lateral').setAttribute('aria-expanded', String(abierto));
  } else {
    const chico = document.body.classList.toggle('lateral-chico');
    guardarPreferencia(chico);
    $('btn-lateral').setAttribute('aria-expanded', String(!chico));
  }
});
$('velo').addEventListener('click', cerrarMovil);
document.addEventListener('keydown', (ev) => { if (ev.key === 'Escape') cerrarMovil(); });
if (leerPreferencia()) document.body.classList.add('lateral-chico');
$('btn-lateral').setAttribute('aria-expanded', String(!movil.matches && !leerPreferencia()));

// ---------- Enrutador ----------
function leerHash() {
  const crudo = location.hash.replace(/^#\/?/, '');
  const [ruta, consulta = ''] = crudo.split('?');
  const [seccion = 'inicio', ...args] = ruta.split('/').filter(Boolean).map(decodeURIComponent);
  return { seccion, args, consulta: new URLSearchParams(consulta) };
}

async function navegar() {
  const mia = ++vez;
  const { seccion, args, consulta } = leerHash();
  if (!location.hash || location.hash === '#' || location.hash === '#/') {
    history.replaceState(null, '', '#/inicio');
  }

  const cont = document.createElement('div');
  cont.className = 'vista';
  principal.classList.toggle('es-inicio', seccion === 'inicio');
  principal.replaceChildren(cont);
  principal.scrollTop = 0;
  marcarActivo(seccion);
  cerrarMovil();

  if (!VISTAS[seccion]) {
    cont.innerHTML = vacio('No encontramos esa página', 'Usa el menú de la izquierda para volver a una función del sistema.', 'search').toString();
    document.title = 'No encontrada · Sistema de Prácticas PEM';
    return;
  }
  if (!rutaPermitida(seccion, usuario.rol)) {
    cont.innerHTML = html`<div class="alerta alerta-error" role="alert">${icono('lock')}<div><strong>No tienes permiso para esta acción.</strong><br>Tu rol (${nombreRol(usuario.rol)}) no puede usar esta función.</div></div>`.toString();
    document.title = 'Sin permiso · Sistema de Prácticas PEM';
    return;
  }

  cont.innerHTML = cargando().toString();
  const contexto = {
    seccion, args, consulta, usuario, rol: usuario.rol,
    ir: (hash) => { location.hash = hash; },
    recargar: () => navegar(),
    vigente: () => mia === vez,
    titulo: (texto) => { if (mia === vez) document.title = `${texto} · Sistema de Prácticas PEM`; },
  };
  document.title = 'Sistema de Prácticas PEM';
  try {
    const modulo = await VISTAS[seccion]();
    if (mia !== vez) return;
    cont.innerHTML = '';
    await modulo.render(cont, contexto);
  } catch (e) {
    if (mia !== vez) return;
    console.error(e);
    cont.innerHTML = alertaError(e.status ? e : { message: 'No se pudo mostrar esta vista.' }).toString();
  }
}

window.addEventListener('hashchange', navegar);
movil.addEventListener('change', () => document.body.classList.remove('lateral-abierto'));

// ---------- Arranque ----------
if (sesion.vigente()) {
  pintarCabecera();
  pintarMenu();
  pintarIconos();
  navegar();

  // El rol se lee de la base en cada petición: se confirma con el servidor y se refresca si cambió.
  api.get('/api/auth/me').then((yo) => {
    const cambio = yo.rol !== usuario.rol || yo.nombre !== usuario.nombre || yo.apellidos !== usuario.apellidos;
    usuario = { ...usuario, ...yo };
    sesion.guardar({ token: sesion.token(), usuario, expiraEnSegundos: Math.max(1, (sesion.expira() - Date.now()) / 1000) });
    if (cambio) { pintarCabecera(); pintarMenu(); navegar(); }
  }).catch(() => { /* 401 ya redirige al login; otros errores no impiden usar la app */ });
}
