// Sección "Documentos" reutilizable (Fase E, punto 3): subir y listar los archivos adjuntos de una
// planificación o de una evaluación. `base` ya trae el id del recurso, por ejemplo
// "/api/planificaciones/3/documentos" o "/api/evaluaciones/7/documentos".
// HU-21/HU-22: quien puede modificarlos (el estudiante dueño en borrador o con observaciones; el evaluador mientras la
// inscripción esté en curso) ve además «Reemplazar» (PUT, mismo documento) y «Borrar» (DELETE, con confirmación).
import { parametros } from './datos.js';
import { api, descargarArchivo } from './api.js';
import { confirmar, fechaHora, html, icono, toast, toastError, vacio } from './ui.js';

function tamano(bytes) {
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${Math.ceil(bytes / 1024)} KB`;
}

function filaDocumento(d, puedeModificar) {
  return html`<li class="documento">
    <span class="documento-ico">${icono('file')}</span>
    <span class="documento-datos">
      <strong>${d.nombreArchivo}</strong>
      <small>${tamano(d.tamanoBytes)} · subido por ${d.subidoPor.nombreCompleto} · ${fechaHora(d.fechaSubida)}</small>
    </span>
    <button type="button" class="btn btn-chico" data-descargar="${d.id}" data-nombre="${d.nombreArchivo}">${icono('download')} Descargar</button>
    ${puedeModificar ? html`<button type="button" class="btn btn-chico" data-reemplazar="${d.id}" data-nombre="${d.nombreArchivo}">${icono('upload')} Reemplazar</button>
      <button type="button" class="btn btn-chico btn-fantasma" data-borrar="${d.id}" data-nombre="${d.nombreArchivo}" aria-label="Borrar ${d.nombreArchivo}">${icono('x')} Borrar</button>` : ''}
  </li>`;
}

/**
 * Pinta la tarjeta "Documentos" dentro de `cont` (un elemento ya en el DOM).
 * `puedeModificar` (booleano o promesa de booleano): muestra «Reemplazar» y «Borrar» en cada documento.
 * @param {{base: string, puedeSubir: boolean, puedeModificar?: boolean|Promise<boolean>, tiposAceptados?: string, ayuda?: string}} opciones
 */
export async function montarDocumentos(cont, { base, puedeSubir, puedeModificar: modificar = false, tiposAceptados = '.docx,.pdf', ayuda: ayudaBase = 'Word (.docx) o PDF, máximo 10 MB.' }) {
  const puedeModificar = await Promise.resolve(modificar).catch(() => false);
  // HU-27: el tamaño máximo vigente lo fija el coordinador
  const mb = (await parametros())['archivos.documento_mb'];
  const ayuda = ayudaBase.replace(/máximo \d+ MB/, `máximo ${mb} MB`);
  cont.innerHTML = html`<div class="tarjeta">
    <div class="tarjeta-titulo"><h2>Documentos</h2></div>
    ${puedeSubir ? html`<form id="form-documento" class="form-documento" novalidate>
        <label class="campo-archivo-linea">
          <input type="file" id="archivo-documento" accept="${tiposAceptados}" required>
        </label>
        <button type="submit" class="btn btn-primario" data-cargando="Subiendo…">${icono('upload')} Subir documento</button>
        <p class="ayuda-campo">${ayuda}</p>
        <span class="error-campo" id="error-documento" role="alert" hidden></span>
      </form>` : ''}
    <ul class="documentos" id="lista-documentos"><li class="subtitulo">Cargando…</li></ul>
    ${puedeModificar ? html`<input type="file" id="archivo-reemplazo" accept="${tiposAceptados}" hidden>` : ''}
  </div>`.toString();

  const lista = cont.querySelector('#lista-documentos');

  async function cargar() {
    lista.innerHTML = '<li class="subtitulo">Cargando…</li>';
    try {
      const datos = await api.get(base);
      lista.innerHTML = datos.length === 0
        ? `<li>${vacio('Sin documentos todavía', puedeSubir ? 'Sube el primero con el formulario de arriba.' : '', 'file')}</li>`
        : datos.map((d) => filaDocumento(d, puedeModificar).toString()).join('');
    } catch (e) {
      lista.innerHTML = `<li class="subtitulo">No se pudo cargar la lista de documentos.</li>`;
    }
  }

  // HU-21/HU-22: reemplazar (elige el archivo nuevo y lo envía con PUT) y borrar (con confirmación)
  const reemplazo = cont.querySelector('#archivo-reemplazo');
  let reemplazando = null;
  reemplazo?.addEventListener('change', async () => {
    const archivo = reemplazo.files[0];
    if (!archivo || !reemplazando) return;
    const { id, nombre } = reemplazando;
    reemplazando = null;
    const datos = new FormData();
    datos.append('archivo', archivo);
    try {
      await api.subir(`${base}/${id}`, datos, 'PUT');
      toast(`«${nombre}» se reemplazó por «${archivo.name}».`);
      await cargar();
    } catch (e) {
      toastError(e);
    } finally {
      reemplazo.value = '';
    }
  });

  lista.addEventListener('click', async (ev) => {
    const cambiar = ev.target.closest('[data-reemplazar]');
    if (cambiar) {
      reemplazando = { id: cambiar.dataset.reemplazar, nombre: cambiar.dataset.nombre };
      reemplazo.click();
      return;
    }
    const quitar = ev.target.closest('[data-borrar]');
    if (quitar) {
      const ok = await confirmar({
        titulo: 'Borrar documento', peligro: true, textoOk: 'Borrar',
        mensaje: `¿Borrar «${quitar.dataset.nombre}»? El archivo se elimina y no se puede recuperar.`,
      });
      if (!ok) return;
      try {
        await api.del(`${base}/${quitar.dataset.borrar}`);
        toast(`«${quitar.dataset.nombre}» se borró.`);
        await cargar();
      } catch (e) {
        toastError(e);
      }
      return;
    }
    const boton = ev.target.closest('[data-descargar]');
    if (!boton) return;
    boton.disabled = true;
    const original = boton.innerHTML;
    try {
      const { blob, nombre } = await api.archivo(`${base}/${boton.dataset.descargar}`);
      descargarArchivo({ blob, nombre: nombre === 'archivo' ? boton.dataset.nombre : nombre });
    } catch (e) {
      toastError(e);
    } finally {
      boton.disabled = false;
      boton.innerHTML = original;
    }
  });

  if (puedeSubir) {
    const form = cont.querySelector('#form-documento');
    const input = cont.querySelector('#archivo-documento');
    const error = cont.querySelector('#error-documento');
    form.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      error.hidden = true;
      const archivo = input.files[0];
      if (!archivo) {
        error.textContent = 'Selecciona un archivo para subir.';
        error.hidden = false;
        return;
      }
      const boton = form.querySelector('button[type="submit"]');
      const original = boton.innerHTML;
      boton.disabled = true;
      boton.innerHTML = `<span class="cargando"></span> ${boton.dataset.cargando}`;
      const datos = new FormData();
      datos.append('archivo', archivo);
      try {
        await api.subir(base, datos);
        form.reset();
        await cargar();
      } catch (e) {
        error.textContent = e.message || 'No se pudo subir el archivo.';
        error.hidden = false;
      } finally {
        boton.disabled = false;
        boton.innerHTML = original;
      }
    });
  }

  await cargar();
}
