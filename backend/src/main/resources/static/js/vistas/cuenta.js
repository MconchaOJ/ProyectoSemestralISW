// HU-01 · Iniciar sesión con roles: datos de la sesión, qué permite cada rol y (coordinador) el listado de roles.
import { api, sesion } from '../api.js';
import { DESCRIPCION_ROL } from '../datos.js';
import { alertaError, cabecera, html, icono, iniciales, nombreRol } from '../ui.js';

export async function render(cont, ctx) {
  ctx.titulo('Mi cuenta y rol');
  const yo = await api.get('/api/auth/me');
  const vence = new Date(sesion.expira());

  let roles = '';
  if (ctx.rol === 'COORDINADOR') {
    try {
      const lista = await api.get('/api/roles');
      roles = html`
        <div class="tarjeta">
          <h2>Roles del sistema</h2>
          <p class="subtitulo">Solo el coordinador puede consultar este listado (<code>GET /api/roles</code>). Cada persona tiene un único rol.</p>
          <div class="tabla-contenedor"><table class="tabla">
            <thead><tr><th>Rol</th><th>Descripción</th></tr></thead>
            <tbody>${lista.map((r) => html`<tr><td><span class="badge badge-rol">${nombreRol(r.nombre)}</span></td><td>${r.descripcion}</td></tr>`)}</tbody>
          </table></div>
        </div>`;
    } catch (e) {
      roles = alertaError(e);
    }
  }

  cont.innerHTML = html`
    ${cabecera({ hu: 'HU-01', titulo: 'Iniciar sesión con roles', subtitulo: 'Tu sesión y lo que puedes hacer en el sistema según tu rol.' })}
    <div class="rejilla-2">
      <div class="tarjeta">
        <div class="perfil">
          <span class="avatar-grande">${iniciales(yo)}</span>
          <div>
            <h2>${yo.nombre} ${yo.apellidos}</h2>
            <p class="subtitulo">${yo.email}</p>
            <span class="badge badge-rol">${nombreRol(yo.rol)}</span>
            ${yo.activo ? html` <span class="badge badge-AUTORIZADA">Activo</span>` : html` <span class="badge badge-REPROBADA">Inactivo</span>`}
          </div>
        </div>
        <dl class="datos">
          <div><dt>Sesión vence</dt><dd>${vence.toLocaleString('es-CL', { dateStyle: 'short', timeStyle: 'short' })} (el token dura 8 horas desde que ingresaste)</dd></div>
          <div><dt>Almacenamiento</dt><dd>El token JWT se guarda solo en esta pestaña (<code>sessionStorage</code>) y se borra al cerrar sesión.</dd></div>
          <div><dt>Contraseña</dt><dd>La define el coordinador. Si la olvidaste, contáctalo.</dd></div>
        </dl>
      </div>

      <div class="tarjeta">
        <h2>${icono('shield')} Con tu rol puedes</h2>
        <ul class="lista-simple">${(DESCRIPCION_ROL[yo.rol] ?? []).map((t) => html`<li>${t}</li>`)}</ul>
        <p class="subtitulo">El servidor revisa tu rol en cada petición: si no tienes permiso para algo, responde 403 aunque conozcas la dirección.</p>
      </div>
    </div>
    ${roles}`.toString();
}
