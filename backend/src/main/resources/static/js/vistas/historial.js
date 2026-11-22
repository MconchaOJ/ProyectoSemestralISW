// HU-05 · Ver el historial del estudiante: recorrido por las prácticas con evaluaciones y planificaciones.
// HU-18 · Informes y productos de la práctica: el estudiante los sube a cada inscripción EN_CURSO (S-93) y
//   quedan en su recorrido; quien puede ver el historial los descarga.
//   Estudiante: #/historial (el suyo)   ·   Guía, profesor del curso, coordinador, jefe de carrera: #/historial/{estudianteId}
import { api, descargarArchivo } from '../api.js';
import { parametros } from '../datos.js';
import {
  MODALIDADES, alerta, alertaError, badge, cabecera, conCarga, confirmar, debounce, fecha, fechaHora, html, icono, iniciales, nota,
  promedio, toast, toastError, vacio,
} from '../ui.js';

/** HU-18: tipos de producto (mismo orden que TipoProducto en el backend). */
const TIPOS_PRODUCTO = {
  INFORME_FINAL: 'Informe final',
  DIARIO_REFLEXION: 'Diario de reflexión',
  PROYECTO_INTERVENCION: 'Proyecto de intervención',
  OTRO: 'Otro producto',
};

/** Sin distinguir mayúsculas ni tildes, para buscar como lo hace la base de datos. */
const normalizar = (t) => String(t ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

export async function render(cont, ctx) {
  const [id] = ctx.args;
  ctx.titulo('Historial');

  if (ctx.tiene('ESTUDIANTE')) {
    const h = await api.get('/api/estudiantes/me/historial');
    const mb = (await parametros())['archivos.documento_mb'];   // HU-27: tamaño máximo vigente
    return pintar(cont, ctx, h, { propio: true, mb });
  }
  if (id) {
    const h = await api.get(`/api/estudiantes/${id}/historial`);
    return pintar(cont, ctx, h, { propio: false });
  }
  return selector(cont, ctx);
}

/** Guía, profesor del curso, coordinador y jefe de carrera eligen a un estudiante. */
async function selector(cont, ctx) {
  const encabezado = cabecera({
    hu: 'HU-05', titulo: 'Ver el historial del estudiante',
    subtitulo: 'Elige a un estudiante para ver su recorrido completo por las prácticas y cómo evolucionan sus evaluaciones.',
  });
  let estudiantes;
  if (ctx.veTodo) { // coordinador o jefe de carrera: todos los estudiantes
    const lista = await api.get('/api/estudiantes');
    estudiantes = lista.map((e) => ({
      id: e.id, nombre: `${e.nombre} ${e.apellidos}`, detalle: `${e.rut} · semestre ${e.semestreActual}`,
      busqueda: normalizar(`${e.nombre} ${e.apellidos} ${e.rut} ${e.email}`),
    }));
  } else { // guía / profesor del curso: los estudiantes de sus inscripciones
    const inscripciones = await api.get('/api/inscripciones');
    const mapa = new Map();
    inscripciones.forEach((i) => {
      if (!mapa.has(i.estudiante.id)) {
        mapa.set(i.estudiante.id, {
          id: i.estudiante.id, nombre: i.estudiante.nombreCompleto, detalle: i.estudiante.rut,
          busqueda: normalizar(`${i.estudiante.nombreCompleto} ${i.estudiante.rut}`),
        });
      }
    });
    estudiantes = [...mapa.values()];
  }

  const listaVacia = estudiantes.length === 0;
  cont.innerHTML = html`${encabezado}
    ${listaVacia
      ? vacio('Todavía no tienes estudiantes asignados', 'Cuando el coordinador te asigne una inscripción, podrás ver el historial del estudiante.', 'users')
      : html`<label class="buscador">${icono('search')}<input type="search" id="buscar-estudiante"
              placeholder="Buscar por nombre, RUT o correo…" aria-label="Buscar estudiante"></label>
          <div class="tarjetas-estudiantes" id="lista-estudiantes"></div>`}`.toString();
  if (listaVacia) return;

  const buscador = cont.querySelector('#buscar-estudiante');
  const lista = cont.querySelector('#lista-estudiantes');
  const pintarLista = () => {
    const filtro = normalizar(buscador.value.trim());
    const visibles = filtro ? estudiantes.filter((e) => e.busqueda.includes(filtro)) : estudiantes;
    lista.innerHTML = visibles.length === 0
      ? vacio('Sin resultados', 'Prueba con otro nombre, RUT o correo.', 'search').toString()
      : visibles.map((e) => html`
          <a class="tarjeta-estudiante" href="#/historial/${e.id}">
            <span class="avatar-grande">${iniciales({ nombre: e.nombre.split(' ')[0], apellidos: e.nombre.split(' ')[1] ?? '' })}</span>
            <span><strong>${e.nombre}</strong><small>${e.detalle}</small></span>
            ${icono('clock')}
          </a>`).join('');
  };
  buscador.addEventListener('input', debounce(pintarLista, 150));
  pintarLista();
}

function pintar(cont, ctx, h, { propio, mb = 10 }) {
  const e = h.estudiante;
  ctx.titulo(`Historial de ${e.nombre}`);
  const insc = [...h.inscripciones].sort((a, b) => a.periodo.localeCompare(b.periodo) || a.asignatura.orden - b.asignatura.orden);
  const total = insc.length;
  const aprobadas = h.asignaturasAprobadas.length;
  const enCurso = insc.filter((i) => i.estado === 'EN_CURSO').length;

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-05', titulo: propio ? 'Mi historial de prácticas' : `Historial de ${e.nombre} ${e.apellidos}`,
      subtitulo: 'Recorrido completo por las prácticas: cómo evoluciona a lo largo del tiempo y no solo una nota final.',
      // HU-28 (y criterio 1 de HU-08): el mismo historial en PDF, con las mismas reglas de acceso
      acciones: html`<button type="button" class="btn" id="btn-historial-pdf" data-cargando="Generando…">${icono('download')} Descargar PDF</button>
        <button type="button" class="btn" id="btn-historial-xlsx" data-cargando="Generando…">${icono('download')} Descargar Excel</button>
        ${propio ? '' : html`<a class="btn btn-fantasma" href="#/historial">${icono('back')} Cambiar estudiante</a>`}`,
    })}

    <div class="tarjeta perfil-historial">
      <span class="avatar-grande">${iniciales(e)}</span>
      <div class="perfil-datos">
        <h2>${e.nombre} ${e.apellidos}</h2>
        <p class="subtitulo">${e.email} · RUT ${e.rut}</p>
      </div>
      <dl class="cifras">
        <div><dt>Semestre actual</dt><dd>${e.semestreActual}</dd></div>
        <div><dt>Ingreso</dt><dd>${e.anioIngreso}</dd></div>
        <div><dt>Prácticas aprobadas</dt><dd>${aprobadas} de 6</dd></div>
        <div><dt>En curso</dt><dd>${enCurso}</dd></div>
      </dl>
    </div>

    ${aprobadas > 0 ? html`<div class="tarjeta">
      <h2>Asignaturas aprobadas <small class="subtitulo">(contexto que usa el asistente de planificación)</small></h2>
      <ul class="chips">${h.asignaturasAprobadas.map((a) => html`<li class="chip"><strong>${a.nombre}</strong> <span>${a.periodo} · nota ${nota(a.notaFinal)}</span></li>`)}</ul>
    </div>` : ''}

    <h2 class="titulo-linea">Recorrido por período</h2>
    ${total === 0 ? vacio('Este estudiante aún no tiene inscripciones', '', 'clipboard') : html`<ol class="recorrido">${insc.map((i) => practica(i, propio, mb))}</ol>`}
    ${propio ? html`<input type="file" id="archivo-reemplazo-producto" accept=".docx,.pdf" hidden>` : ''}`.toString();

  activarProductos(cont, ctx, propio);

  // HU-08: el mismo informe por estudiante en Excel (integrado del trabajo de Alej), con las mismas reglas de acceso
  cont.querySelector('#btn-historial-xlsx').addEventListener('click', async (ev) => {
    try {
      const archivo = await conCarga(ev.currentTarget, () =>
        api.archivo(propio ? '/api/estudiantes/me/historial/xlsx' : `/api/estudiantes/${e.id}/historial/xlsx`));
      descargarArchivo({ ...archivo, nombre: archivo.nombre === 'archivo' ? 'historial.xlsx' : archivo.nombre });
      toast('Historial descargado en Excel.');
    } catch (err) {
      if (err.status !== 401) toast(err.message, 'error');
    }
  });

  cont.querySelector('#btn-historial-pdf').addEventListener('click', async (ev) => {
    try {
      const archivo = await conCarga(ev.currentTarget, () =>
        api.archivo(propio ? '/api/estudiantes/me/historial/pdf' : `/api/estudiantes/${e.id}/historial/pdf`));
      descargarArchivo({ ...archivo, nombre: archivo.nombre === 'archivo' ? 'historial.pdf' : archivo.nombre });
      toast('Historial descargado.');
    } catch (err) {
      if (err.status !== 401) toast(err.message, 'error');
    }
  });
}

function practica(i, propio, mb) {
  const evals = [...i.evaluaciones].sort((a, b) => String(a.fechaClase).localeCompare(String(b.fechaClase)));
  return html`<li class="practica estado-${i.estado}">
    <span class="practica-marca">${i.asignatura.orden}</span>
    <div class="tarjeta">
      <div class="tarjeta-titulo">
        <div>
          <h3>${i.asignatura.nombre}</h3>
          <small class="subtitulo">${i.periodo} · ${MODALIDADES[i.asignatura.modalidad]} · ${i.asignatura.requiereCentroEducativo ? 'en un colegio' : 'sin colegio'}</small>
        </div>
        <div class="practica-estado">${badge(i.estado)} ${i.notaFinal !== null && i.notaFinal !== undefined ? html`<strong class="nota-final">${nota(i.notaFinal)}</strong>` : ''}</div>
      </div>
      <p class="equipo">
        <span>Profesor guía: <strong>${i.profesorGuia ? i.profesorGuia.nombreCompleto : 'no lleva'}</strong></span>
        <span>Profesor del curso: <strong>${i.profesorCurso?.nombreCompleto ?? '—'}</strong></span>
        ${i.companeros?.length ? html`<span class="companeros">Trabajó en ${i.asignatura.modalidad === 'DUPLA' ? 'dupla' : 'grupo'} con: <strong>${i.companeros.join(', ')}</strong></span>` : ''}
      </p>

      ${evals.length > 0 ? html`
        <h4>Evaluaciones</h4>
        <ul class="evals">${evals.map((ev) => html`<li>
          <a href="#/evaluaciones/${ev.id}">${fecha(ev.fechaClase)}</a>
          <span class="ev-lugar">${ev.establecimiento} · ${ev.curso}</span>
          <span class="barra-prom mini" role="img" aria-label="Promedio ${promedio(ev.promedio)}"><span style="width:${Math.max(0, Math.min(100, ((Number(ev.promedio) - 1) / 6) * 100))}%"></span></span>
          <strong class="ev-prom">${promedio(ev.promedio)}</strong>
        </li>`)}</ul>` : ''}

      ${i.planificaciones.length > 0 ? html`
        <h4>Planificaciones</h4>
        <ul class="planes">${i.planificaciones.map((p) => html`<li>
          <a href="#/planificaciones/${p.id}">${p.titulo}</a>
          <small>${fecha(p.fechaClase)}</small>
          ${badge(p.estado)}
        </li>`)}</ul>` : ''}

      ${productos(i, propio, mb)}

      ${evals.length === 0 && i.planificaciones.length === 0
        ? html`<p class="subtitulo">${i.asignatura.requierePlanificacion ? 'Sin evaluaciones ni planificaciones registradas.' : 'Esta práctica no entrega planificaciones.'}</p>` : ''}
    </div>
  </li>`;
}

/** HU-18: lista de informes y productos de una inscripción y, para el estudiante en una práctica en curso, el formulario. */
function productos(i, propio, mb) {
  const lista = i.productos ?? [];
  const editable = propio && i.estado === 'EN_CURSO';
  if (lista.length === 0 && !editable) return '';
  return html`<div class="productos" data-inscripcion="${i.inscripcionId}">
    <h4>Informes y productos</h4>
    ${lista.length > 0 ? html`<ul class="documentos">${lista.map((p) => html`<li class="documento">
        <span class="documento-ico">${icono('file')}</span>
        <span class="documento-datos">
          <strong>${p.tipoNombre}: ${p.nombreArchivo}</strong>
          <small>subido el ${fechaHora(p.fechaSubida)}</small>
        </span>
        <button type="button" class="btn btn-chico" data-descargar-producto="${p.id}" data-nombre="${p.nombreArchivo}">${icono('download')} Descargar</button>
        ${editable ? html`<button type="button" class="btn btn-chico" data-reemplazar-producto="${p.id}" data-nombre="${p.nombreArchivo}">${icono('upload')} Reemplazar</button>
          <button type="button" class="btn btn-chico btn-fantasma" data-borrar-producto="${p.id}" data-nombre="${p.nombreArchivo}" aria-label="Borrar ${p.nombreArchivo}">${icono('x')} Borrar</button>` : ''}
      </li>`)}</ul>` : html`<p class="subtitulo">Todavía no subes informes ni productos de esta práctica.</p>`}
    ${editable ? html`<form class="form-documento form-producto" novalidate>
        <select name="tipo" aria-label="Tipo de producto">${Object.entries(TIPOS_PRODUCTO).map(([v, t]) => html`<option value="${v}">${t}</option>`)}</select>
        <label class="campo-archivo-linea"><input type="file" name="archivo" accept=".docx,.pdf" required aria-label="Archivo del producto"></label>
        <button type="submit" class="btn btn-primario" data-cargando="Subiendo…">${icono('upload')} Subir</button>
        <p class="ayuda-campo">Informe final, diario de reflexión, proyecto de intervención u otro. Word (.docx) o PDF, máximo ${mb} MB.</p>
        <span class="error-campo" role="alert" hidden></span>
      </form>` : ''}
  </div>`;
}

/** HU-18: subir, descargar, reemplazar y borrar productos (delegación de eventos; al cambiar algo se vuelve a pintar). */
function activarProductos(cont, ctx, propio) {
  const recargar = () => render(cont, ctx);
  const base = (el) => `/api/inscripciones/${el.closest('[data-inscripcion]').dataset.inscripcion}/productos`;

  cont.querySelectorAll('.form-producto').forEach((form) => form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const error = form.querySelector('.error-campo');
    error.hidden = true;
    const archivo = form.archivo.files[0];
    if (!archivo) {
      error.textContent = 'Selecciona un archivo para subir.';
      error.hidden = false;
      return;
    }
    const datos = new FormData();
    datos.append('tipo', form.tipo.value);
    datos.append('archivo', archivo);
    try {
      await conCarga(form.querySelector('button[type="submit"]'), () => api.subir(base(form), datos));
      toast(`«${archivo.name}» quedó en tu recorrido.`);
      await recargar();
    } catch (e) {
      error.textContent = e.message || 'No se pudo subir el archivo.';
      error.hidden = false;
    }
  }));

  const reemplazo = cont.querySelector('#archivo-reemplazo-producto');
  let reemplazando = null;
  reemplazo?.addEventListener('change', async () => {
    const archivo = reemplazo.files[0];
    if (!archivo || !reemplazando) return;
    const { url, nombre } = reemplazando;
    reemplazando = null;
    const datos = new FormData();
    datos.append('archivo', archivo);
    try {
      await api.subir(url, datos, 'PUT');
      toast(`«${nombre}» se reemplazó por «${archivo.name}».`);
      await recargar();
    } catch (e) {
      toastError(e);
    } finally {
      reemplazo.value = '';
    }
  });

  cont.querySelectorAll('.productos').forEach((bloque) => bloque.addEventListener('click', async (ev) => {
    const bajar = ev.target.closest('[data-descargar-producto]');
    if (bajar) {
      try {
        const archivo = await conCarga(bajar, () => api.archivo(`${base(bajar)}/${bajar.dataset.descargarProducto}`));
        descargarArchivo({ ...archivo, nombre: archivo.nombre === 'archivo' ? bajar.dataset.nombre : archivo.nombre });
      } catch (e) {
        toastError(e);
      }
      return;
    }
    if (!propio) return;
    const cambiar = ev.target.closest('[data-reemplazar-producto]');
    if (cambiar) {
      reemplazando = { url: `${base(cambiar)}/${cambiar.dataset.reemplazarProducto}`, nombre: cambiar.dataset.nombre };
      reemplazo.click();
      return;
    }
    const quitar = ev.target.closest('[data-borrar-producto]');
    if (quitar) {
      const ok = await confirmar({
        titulo: 'Borrar producto', peligro: true, textoOk: 'Borrar',
        mensaje: `¿Borrar «${quitar.dataset.nombre}»? El archivo se elimina y no se puede recuperar.`,
      });
      if (!ok) return;
      try {
        await api.del(`${base(quitar)}/${quitar.dataset.borrarProducto}`);
        toast(`«${quitar.dataset.nombre}» se borró.`);
        await recargar();
      } catch (e) {
        toastError(e);
      }
    }
  }));
}
