// HU-01 · Mi cuenta: datos de la sesión, qué permite cada rol y (coordinador) el listado de roles.
// HU-26 (Sprint 2) · Cambiar la propia contraseña.
import { api, ApiError, sesion } from '../api.js';
import { DESCRIPCION_ROL } from '../datos.js';
import { rolesDe } from '../menu.js';
import {
  alerta, alertaError, cabecera, cajaAlertaForm, campo, enlazarFormulario, html, icono, iniciales, nombreRol, toast,
} from '../ui.js';

/**
 * Los errores 400 del servidor que corresponden a un campo se marcan en ese campo (en rojo, con su texto),
 * para que se vea de inmediato qué hay que corregir.
 */
const CAMPO_DEL_ERROR = [
  [/actual no es correcta/i, 'passwordActual', 'No es tu contraseña actual. Revísala e inténtalo de nuevo.'],
  [/distinta de la actual/i, 'passwordNueva', 'Debe ser distinta de tu contraseña actual.'],
];

export async function render(cont, ctx) {
  ctx.titulo('Mi cuenta');
  const yo = await api.get('/api/auth/me');
  const vence = new Date(sesion.expira());

  let roles = '';
  if (ctx.tiene('COORDINADOR')) {
    try {
      const lista = await api.get('/api/roles');
      roles = html`
        <div class="tarjeta">
          <h2>Roles del sistema</h2>
          <p class="subtitulo">Cada persona tiene un rol, que define lo que puede ver y hacer; algunas cuentas especiales tienen un segundo rol y suman los permisos de ambos. Solo el coordinador ve este listado.</p>
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
    ${cabecera({ hu: 'HU-01', titulo: 'Mi cuenta', subtitulo: 'Tus datos, lo que puedes hacer en el sistema y el cambio de tu contraseña.' })}
    <div class="rejilla-2">
      <div class="tarjeta">
        <div class="perfil">
          <span class="avatar-grande">${iniciales(yo)}</span>
          <div>
            <h2>${yo.nombre} ${yo.apellidos}</h2>
            <p class="subtitulo">${yo.email}</p>
            ${rolesDe(yo).map((r) => html`<span class="badge badge-rol">${nombreRol(r)}</span> `)}
            ${yo.activo ? html` <span class="badge badge-AUTORIZADA">Activo</span>` : html` <span class="badge badge-REPROBADA">Inactivo</span>`}
          </div>
        </div>
        <dl class="datos">
          <div><dt>Tu sesión</dt><dd>Vence el ${vence.toLocaleString('es-CL', { dateStyle: 'short', timeStyle: 'short' })} (dura 8 horas desde que ingresaste). Se cierra también al cerrar esta pestaña o con «Cerrar sesión».</dd></div>
          <div><dt>Contraseña</dt><dd>Puedes cambiarla más abajo cuando quieras. Si la olvidaste, contacta al coordinador de prácticas.</dd></div>
        </dl>
      </div>

      <div class="tarjeta">
        <h2>${icono('shield')} ${rolesDe(yo).length > 1 ? 'Con tus roles puedes' : 'Con tu rol puedes'}</h2>
        <ul class="lista-simple">${rolesDe(yo).flatMap((r) => DESCRIPCION_ROL[r] ?? []).map((t) => html`<li>${t}</li>`)}</ul>
        <p class="subtitulo">${rolesDe(yo).length > 1 ? 'El sistema solo te muestra lo que tus roles permiten.' : 'El sistema solo te muestra lo que tu rol permite.'}</p>
      </div>
    </div>
    <div class="tarjeta" id="cambiar-password">
      <h2>${icono('shield')} Cambiar mi contraseña</h2>
      <p class="subtitulo">Por seguridad, primero escribe tu contraseña actual. La nueva debe tener entre 8 y 72 caracteres; escríbela dos veces para evitar errores de tipeo.</p>
      <form id="form-password" novalidate>
        <div data-exito hidden></div>
        ${cajaAlertaForm()}
        <div class="rejilla-form">
          ${campo({ nombre: 'passwordActual', etiqueta: 'Contraseña actual', tipo: 'password', requerido: true, completo: true, atributos: { autocomplete: 'current-password' } })}
          ${campo({ nombre: 'passwordNueva', etiqueta: 'Contraseña nueva', tipo: 'password', requerido: true, ayuda: 'De 8 a 72 caracteres.', atributos: { autocomplete: 'new-password', minlength: 8, maxlength: 72 } })}
          ${campo({ nombre: 'passwordRepetida', etiqueta: 'Repite la contraseña nueva', tipo: 'password', requerido: true, ayuda: 'Igual a la anterior.', atributos: { autocomplete: 'new-password', maxlength: 72 } })}
        </div>
        <button type="submit" class="btn btn-primario" data-cargando="Guardando…">Cambiar contraseña</button>
      </form>
    </div>
    ${roles}`.toString();

  const form = cont.querySelector('#form-password');
  const exito = form.querySelector('[data-exito]');
  enlazarFormulario(form, form.querySelector('button[type=submit]'), {
    alEnviar: async () => {
      exito.hidden = true;
      // Valores tal cual (sin recortar espacios), igual que en el login
      const { passwordActual, passwordNueva, passwordRepetida } = form.elements;
      if (passwordNueva.value !== passwordRepetida.value) {
        throw new ApiError(400, 'Las contraseñas nuevas no coinciden.', [{ campo: 'passwordRepetida', mensaje: 'No coincide con la contraseña nueva. Escríbela de nuevo.' }]);
      }
      try {
        return await api.put('/api/auth/password', { passwordActual: passwordActual.value, passwordNueva: passwordNueva.value });
      } catch (e) {
        const regla = e.status === 400 && !e.errores?.length && CAMPO_DEL_ERROR.find(([patron]) => patron.test(e.message));
        if (regla) {
          const [, nombre, mensaje] = regla;
          form.elements[nombre].value = '';
          throw new ApiError(400, e.message, [{ campo: nombre, mensaje }]);
        }
        throw e;
      }
    },
    alListo: () => {
      form.reset();
      exito.innerHTML = alerta('ok', html`<strong>Tu contraseña se cambió correctamente.</strong> Desde ahora ingresa con la contraseña nueva. Tu sesión actual sigue abierta.`).toString();
      exito.hidden = false;
      exito.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      toast('Contraseña cambiada.');
    },
  });
}
