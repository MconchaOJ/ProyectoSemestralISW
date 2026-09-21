// Pantalla de inicio (#/inicio): saludo + accesos rápidos a la izquierda, LaCastilla.jpg grande a la derecha.
import { api } from '../api.js';
import { ACCESOS } from '../menu.js';
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
  const accesos = ACCESOS[ctx.rol] ?? [];

  cont.innerHTML = html`
    <section class="inicio">
      <div class="inicio-col-izq">
        <div class="inicio-saludo">
          <small>${saludo()}</small>
          <h1>${u.nombre} ${u.apellidos}</h1>
          <p>Pedagogía en Educación Matemática · Facultad de Educación y Humanidades · Universidad del Bío-Bío</p>
          <span class="badge badge-rol">${nombreRol(ctx.rol)}</span>
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
  accesos.forEach(async (a, i) => {
    if (!a.pendiente) return;
    try {
      const lista = await api.get(a.pendiente.ruta, a.pendiente.params);
      const enlace = cont.querySelector(`[data-acceso="${i}"]`);
      if (ctx.vigente() && enlace && lista.length > 0) {
        enlace.insertAdjacentHTML('beforeend', html`<span class="cuenta" title="Pendientes de tu acción">${lista.length}</span>`.toString());
      }
    } catch { /* sin contador */ }
  });
}
