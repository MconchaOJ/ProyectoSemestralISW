// HU-08 · Informes por asignatura y por estudiante (coordinador y jefe de carrera), en PDF o Excel.
// Reutiliza GET /api/reportes/asignaturas/{id} y GET /api/reportes/estudiantes/{id}: esta pantalla solo pide los
// filtros y descarga el archivo.
import { api, descargarArchivo } from '../api.js';
import { asignaturas } from '../datos.js';
import { alerta, cabecera, cajaAlertaForm, campo, enlazarFormulario, html, icono, toast } from '../ui.js';

const FORMATOS = [{ valor: 'pdf', texto: 'PDF' }, { valor: 'xlsx', texto: 'Excel (.xlsx)' }];

/** Periodo en curso: el semestre 1 va de marzo a julio y el 2 de agosto a diciembre (como S-48). */
function periodoActual(hoy = new Date()) {
  return `${hoy.getFullYear()}-${hoy.getMonth() < 7 ? 1 : 2}`;
}

export async function render(cont, ctx) {
  ctx.titulo('Informes');
  const [lista, estudiantes] = await Promise.all([asignaturas(), api.get('/api/estudiantes')]);

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-08', titulo: 'Informes',
      subtitulo: 'Síntesis de evaluaciones y planificaciones por asignatura o por estudiante, para no juntar a mano la información y tenerla como evidencia para la acreditación.',
    })}
    <div class="tarjeta">
      <h2>Informe por asignatura y periodo</h2>
      ${alerta('info', 'Resumen de las inscripciones, promedio de notas finales, distribución de notas y de puntajes por pauta, y una fila por estudiante. Solo cuenta evaluaciones finalizadas.')}
      <form id="form-informe-asignatura" novalidate>
        ${cajaAlertaForm()}
        <div class="rejilla-form">
          ${campo({ nombre: 'asignatura', etiqueta: 'Asignatura', tipo: 'select', requerido: true, opciones: lista.map((a) => ({ valor: a.id, texto: a.nombre })) })}
          ${campo({ nombre: 'periodo', etiqueta: 'Periodo', requerido: true, valor: periodoActual(), ayuda: 'AAAA-S, por ejemplo 2025-2.', atributos: { pattern: '[0-9]{4}-[12]', title: 'Debe tener el formato AAAA-S, por ejemplo 2025-2.' } })}
          ${campo({ nombre: 'formatoAsignatura', etiqueta: 'Formato', tipo: 'select', requerido: true, valor: 'pdf', opciones: FORMATOS })}
        </div>
        <button type="submit" class="btn btn-primario" data-cargando="Generando…">${icono('download')} Descargar informe</button>
      </form>
    </div>

    <div class="tarjeta">
      <h2>Informe por estudiante</h2>
      ${alerta('info', 'Todas las prácticas del estudiante con sus evaluaciones finalizadas, sus planificaciones y una síntesis por práctica.')}
      <form id="form-informe-estudiante" novalidate>
        ${cajaAlertaForm()}
        <div class="rejilla-form">
          ${campo({ nombre: 'estudiante', etiqueta: 'Estudiante', tipo: 'select', requerido: true, opciones: estudiantes.map((e) => ({ valor: e.id, texto: `${e.apellidos}, ${e.nombre} (${e.rut})` })).sort((a, b) => a.texto.localeCompare(b.texto)) })}
          ${campo({ nombre: 'formatoEstudiante', etiqueta: 'Formato', tipo: 'select', requerido: true, valor: 'pdf', opciones: FORMATOS })}
        </div>
        <button type="submit" class="btn btn-primario" data-cargando="Generando…">${icono('download')} Descargar informe</button>
      </form>
    </div>`.toString();

  const descargar = (respaldo) => (archivo) => {
    descargarArchivo({ ...archivo, nombre: archivo.nombre === 'archivo' ? respaldo : archivo.nombre });
    toast('Informe descargado.');
  };

  const formAsignatura = cont.querySelector('#form-informe-asignatura');
  enlazarFormulario(formAsignatura, formAsignatura.querySelector('button[type=submit]'), {
    alEnviar: ({ asignatura, periodo, formatoAsignatura }) =>
      api.archivo(`/api/reportes/asignaturas/${asignatura}?periodo=${encodeURIComponent(periodo)}&formato=${formatoAsignatura}`),
    alListo: descargar('informe-asignatura'),
  });

  const formEstudiante = cont.querySelector('#form-informe-estudiante');
  enlazarFormulario(formEstudiante, formEstudiante.querySelector('button[type=submit]'), {
    alEnviar: ({ estudiante, formatoEstudiante }) =>
      api.archivo(`/api/reportes/estudiantes/${estudiante}?formato=${formatoEstudiante}`),
    alListo: descargar('informe-estudiante'),
  });
}
