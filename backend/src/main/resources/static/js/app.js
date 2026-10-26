// Estructura de la aplicación: cabecera, barra lateral por rol y enrutador por hash (#/inicio, #/planificaciones...).
import { api, sesion } from './api.js';
import { alertaError, cargando, html, icono, iniciales, nombreRol, pintarIconos, vacio } from './ui.js';
import { FUNCIONES, accesosPara, itemActivo, rolesDe, rutaPermitida, visiblesPara } from './menu.js';

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
  cartola: () => import('./vistas/cartola.js'),
  pautas: () => import('./vistas/pautas.js'),
  chatbot: () => import('./vistas/chatbot.js'),
  'estilo-asistente': () => import('./vistas/estilo-asistente.js'),
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
  $('usuario-rol').textContent = rolesDe(usuario).map(nombreRol).join(' · ');
  $('avatar').textContent = iniciales(usuario);
  $('ico-salir').innerHTML = icono('logout').toString();
  $('btn-lateral').innerHTML = icono('menu').toString();
}

$('btn-salir').addEventListener('click', () => {
  sesion.limpiar();
  location.replace('index.html');
});

// ---------- Barra lateral ----------
// Solo los nombres de las funciones a las que el usuario tiene acceso según su rol: un sistema real
// terminado no menciona sprints, historias de usuario ni funciones futuras en su menú.
function itemHtml(item) {
  return html`<a class="item" href="#/${item.ruta}" data-ruta="${item.ruta}" title="${item.nombre}">
    <span class="item-ico">${icono(item.icono)}</span>
    <span class="item-texto"><span class="item-nombre">${item.nombre}</span></span>
  </a>`;
}

/** Accesos rápidos (los mismos de Inicio) como enlaces directos del menú, para saltar de una función a otra. */
function accesoHtml(acceso) {
  return html`<a class="item item-acceso" href="${acceso.hash}" title="${acceso.titulo}">
    <span class="item-ico">${icono(acceso.icono)}</span>
    <span class="item-texto"><span class="item-nombre">${acceso.titulo}</span></span>
  </a>`;
}

function pintarMenu() {
  const funciones = visiblesPara(FUNCIONES, rolesDe(usuario));
  const accesos = accesosPara(rolesDe(usuario));
  menu.innerHTML = html`
    <div class="grupo">
      <a class="item" href="#/inicio" data-ruta="inicio" title="Inicio">
        <span class="item-ico">${icono('home')}</span>
        <span class="item-texto"><span class="item-nombre">Inicio</span></span>
      </a>
      ${funciones.map((i) => itemHtml(i))}
    </div>
    ${accesos.length ? html`<div class="grupo">
      <div class="grupo-titulo">Accesos rápidos</div>
      <div class="grupo-items">${accesos.map((a) => accesoHtml(a))}</div>
    </div>` : ''}`.toString();
}

menu.addEventListener('click', (ev) => {
  if (ev.target.closest('a.item')) cerrarMovil();
});

function marcarActivo(seccion) {
  const activo = seccion === 'inicio' ? { ruta: 'inicio' } : itemActivo(seccion, rolesDe(usuario));
  menu.querySelectorAll('a.item[data-ruta]').forEach((a) => {
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
  const roles = rolesDe(usuario);
  if (!rutaPermitida(seccion, roles)) {
    cont.innerHTML = html`<div class="alerta alerta-error" role="alert">${icono('lock')}<div><strong>No tienes permiso para esta acción.</strong><br>Tu rol (${roles.map(nombreRol).join(' · ')}) no puede usar esta función.</div></div>`.toString();
    document.title = 'Sin permiso · Sistema de Prácticas PEM';
    return;
  }

  cont.innerHTML = cargando().toString();
  // Una cuenta puede tener dos roles: las vistas preguntan con tiene(rol), no comparando ctx.rol (el principal).
  // Con el segundo rol no basta TENER el rol para actuar sobre una inscripción: hay que ser su guía o su profesor
  // del curso (el jefe de carrera ve todas, pero como guía solo actúa en las suyas). actuaComo lo resuelve.
  const tiene = (r) => roles.includes(r);
  const contexto = {
    seccion, args, consulta, usuario, rol: usuario.rol, roles, tiene,
    veTodo: tiene('COORDINADOR') || tiene('JEFE_CARRERA'),
    actuaComo: (rol, i) => {
      if (!tiene(rol) || !i) return false;
      if (rol === 'PROFESOR_GUIA') return i.profesorGuia?.id === usuario.id;
      if (rol === 'PROFESOR_CURSO') return i.profesorCurso?.id === usuario.id;
      if (rol === 'TUTOR') return i.tutor?.id === usuario.id;
      return true; // ESTUDIANTE: el servidor ya le entrega solo lo suyo
    },
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
    // Un 4xx de la API (sin permiso, no existe) es una respuesta esperada y ya se muestra en pantalla: a la consola
    // solo van los errores inesperados (fallos de JavaScript o del servidor).
    if (!e.status || e.status >= 500) console.error(e);
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
    const cambio = yo.rol !== usuario.rol || yo.rolSecundario !== usuario.rolSecundario || yo.nombre !== usuario.nombre || yo.apellidos !== usuario.apellidos;
    usuario = { ...usuario, ...yo };
    sesion.guardar({ token: sesion.token(), usuario, expiraEnSegundos: Math.max(1, (sesion.expira() - Date.now()) / 1000) });
    if (cambio) { pintarCabecera(); pintarMenu(); navegar(); }
  }).catch(() => { /* 401 ya redirige al login; otros errores no impiden usar la app */ });
}
