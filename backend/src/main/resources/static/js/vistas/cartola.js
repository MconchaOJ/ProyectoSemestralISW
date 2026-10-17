// HU-31 · Cartola de prácticas (solo jefe de carrera): PDF con las inscripciones cerradas entre dos fechas.
// Reutiliza GET /api/reportes/cartola-practicas; esta pantalla solo pide las fechas y descarga el archivo.
import { ApiError, api, descargarArchivo } from '../api.js';
import { alerta, cabecera, cajaAlertaForm, campo, enlazarFormulario, html, icono, toast } from '../ui.js';

/** AAAA-MM-DD en la hora local (toISOString usaría UTC y podría correr el día). */
const dia = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

export async function render(cont, ctx) {
  ctx.titulo('Cartola de prácticas');
  const hoy = new Date();

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-31', titulo: 'Cartola de prácticas',
      subtitulo: 'Un PDF con las prácticas cerradas (aprobadas o reprobadas) entre dos fechas, ambas incluidas: evidencia para la acreditación.',
    })}
    <div class="tarjeta">
      ${alerta('info', 'Se filtra por la fecha de cierre de cada inscripción. Las prácticas EN CURSO todavía no tienen fecha de cierre y no aparecen.')}
      <form id="form-cartola" novalidate>
        ${cajaAlertaForm()}
        <div class="rejilla-form">
          ${campo({ nombre: 'desde', etiqueta: 'Desde', tipo: 'date', requerido: true, valor: `${hoy.getFullYear()}-01-01` })}
          ${campo({ nombre: 'hasta', etiqueta: 'Hasta', tipo: 'date', requerido: true, valor: dia(hoy) })}
        </div>
        <button type="submit" class="btn btn-primario" data-cargando="Generando…">${icono('download')} Descargar PDF</button>
      </form>
    </div>`.toString();

  const form = cont.querySelector('#form-cartola');
  enlazarFormulario(form, form.querySelector('button[type=submit]'), {
    alEnviar: ({ desde, hasta }) => {
      if (desde > hasta) {
        throw new ApiError(400, 'La fecha inicial no puede ser posterior a la final.', [{ campo: 'hasta', mensaje: 'Debe ser igual o posterior a «Desde».' }]);
      }
      return api.archivo(`/api/reportes/cartola-practicas?desde=${desde}&hasta=${hasta}`);
    },
    alListo: (archivo) => {
      descargarArchivo({ ...archivo, nombre: archivo.nombre === 'archivo' ? 'cartola-practicas.pdf' : archivo.nombre });
      toast('Cartola descargada.');
    },
  });
}
