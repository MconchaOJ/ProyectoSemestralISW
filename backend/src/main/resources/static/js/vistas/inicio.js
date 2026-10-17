// Pantalla de inicio (#/inicio): saludo + accesos rápidos a la izquierda, LaCastilla.jpg grande a la derecha.
import { api } from '../api.js';
import { accesosPara } from '../menu.js';
import { html, icono, nombreRol } from '../ui.js';

function saludo() {
  const h = new Date().getHours();
  if (h < 6) return 'Buenas noches';
  if (h < 13) return 'Buenos días';
  if (h < 20) return 'Buenas tardes';
  return 'Buenas noches';
}

export async function render(cont, ctx) {
  ctx.titulo('Inicio');
  const u = ctx.usuario;
  const accesos = accesosPara(ctx.roles);

  cont.innerHTML = html`
    <section class="inicio">
      <div class="inicio-col-izq">
        <div class="inicio-saludo">
          <small>${saludo()}</small>
          <h1>${u.nombre} ${u.apellidos}</h1>
          <p>Pedagogía en Educación Matemática · Facultad de Educación y Humanidades · Universidad del Bío-Bío</p>
          ${ctx.roles.map((r) => html`<span class="badge badge-rol">${nombreRol(r)}</span> `)}
        </div>

        <h2 class="accesos-titulo">Accesos rápidos</h2>
        ${accesos.map((a, i) => html`
          <a class="acceso" href="${a.hash}" data-acceso="${i}">
            <span class="acceso-ico">${icono(a.icono)}</span>
            <span><strong>${a.titulo}</strong><small>${a.texto}</small></span>
          </a>`)}
      </div>

      <div class="hero">
        <img src="img/LaCastilla.jpg" alt="Campus La Castilla, Facultad de Educación y Humanidades" onerror="this.remove()">
        <div class="hero-franja">
          ${icono('graduation')}
          <div><strong>Pedagogía en Educación Matemática</strong><small>Campus La Castilla · Universidad del Bío-Bío</small></div>
        </div>
      </div>
    </section>`.toString();

  // Contador de tareas pendientes (planificaciones por revisar/autorizar). Si falla, simplemente no se muestra.
  // Solo cuenta las de inscripciones donde la cuenta actúa con ese rol: el jefe de carrera que también es guía
  // recibe todas las planificaciones, pero solo le toca revisar las de sus propios estudiantes.
  let inscripciones = null;
  accesos.forEach(async (a, i) => {
    if (!a.pendiente) return;
    try {
      inscripciones ??= api.get('/api/inscripciones').then((l) => new Map(l.map((x) => [x.id, x])));
      const [todas, porId] = await Promise.all([api.get(a.pendiente.ruta, a.pendiente.params), inscripciones]);
      const lista = todas.filter((p) => ctx.actuaComo(a.rol, porId.get(p.inscripcionId)));
      const enlace = cont.querySelector(`[data-acceso="${i}"]`);
      if (ctx.vigente() && enlace && lista.length > 0) {
        enlace.insertAdjacentHTML('beforeend', html`<span class="cuenta" title="Pendientes de tu acción">${lista.length}</span>`.toString());
      }
    } catch { /* sin contador */ }
  });
}
