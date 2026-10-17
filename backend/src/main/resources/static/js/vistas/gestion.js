// HU-02 · Registrar estudiantes e inscribirlos en una práctica.
//   Coordinador: pestañas Estudiantes, Profesores e Inscripciones (crear, buscar, cerrar).
//   Otros roles: "Mis inscripciones" (solo lectura; el servidor entrega a cada uno lo suyo; al jefe de carrera, todas).
import { api } from '../api.js';
import { asignaturas } from '../datos.js';
import {
  MODALIDADES, alertaError, badge, cabecera, campo, cargando, debounce, esc, html, icono, modalFormulario,
  nombreRol, nota, toast, vacio,
} from '../ui.js';

const PESTANAS = [
  { ruta: 'estudiantes', texto: 'Estudiantes', icono: 'users' },
  { ruta: 'profesores', texto: 'Profesores', icono: 'graduation' },
  { ruta: 'inscripciones', texto: 'Inscripciones', icono: 'clipboard' },
];

export async function render(cont, ctx) {
  if (!ctx.tiene('COORDINADOR')) return renderMisInscripciones(cont, ctx);

  const activa = PESTANAS.find((p) => p.ruta === ctx.seccion) ?? PESTANAS[0];
  ctx.titulo(activa.texto);
  cont.innerHTML = html`
    ${cabecera({ hu: 'HU-02', titulo: 'Registrar estudiantes e inscribirlos en una práctica',
      subtitulo: 'Da de alta a estudiantes y profesores, e inscribe a cada estudiante en una de las 6 prácticas.' })}
    <nav class="pestanas" aria-label="Secciones">
      ${PESTANAS.map((p) => html`<a class="pestana ${p.ruta === activa.ruta ? 'activa' : ''}" href="#/${p.ruta}" ${p.ruta === activa.ruta ? html`aria-current="page"` : ''}>${icono(p.icono)} ${p.texto}</a>`)}
    </nav>
    <div id="zona"></div>`.toString();
  const zona = cont.querySelector('#zona');
  if (activa.ruta === 'estudiantes') await tabEstudiantes(zona, ctx);
  else if (activa.ruta === 'profesores') await tabProfesores(zona, ctx);
  else await tabInscripciones(zona, ctx, { coordinador: true });
}

// ============================ ESTUDIANTES ============================
async function tabEstudiantes(zona, ctx) {
  zona.innerHTML = html`
    <div class="barra-herramientas">
      <label class="buscador">${icono('search')}<input type="search" id="buscar" placeholder="Buscar por nombre, RUT o correo…" aria-label="Buscar estudiante"></label>
      <button type="button" class="btn btn-primario" id="nuevo">${icono('plus')} Registrar estudiante</button>
    </div>
    <div id="lista"></div>`.toString();
  const lista = zona.querySelector('#lista');
  const buscar = zona.querySelector('#buscar');
  let pedido = 0;

  async function cargar() {
    const mio = ++pedido;
    lista.innerHTML = cargando('Cargando estudiantes…').toString();
    try {
      const datos = await api.get('/api/estudiantes', { buscar: buscar.value });
      if (mio !== pedido) return;
      lista.innerHTML = datos.length === 0
        ? vacio('No hay estudiantes para mostrar', buscar.value ? 'Prueba con otro nombre, RUT o correo.' : 'Registra al primero con el botón de arriba.', 'users').toString()
        : html`<div class="tabla-contenedor"><table class="tabla">
            <thead><tr><th>Estudiante</th><th>RUT</th><th>Correo</th><th class="num">Semestre</th><th class="num">Ingreso</th><th>Estado</th><th></th></tr></thead>
            <tbody>${datos.map((e) => html`<tr>
              <td><strong>${e.nombre} ${e.apellidos}</strong></td><td>${e.rut}</td><td>${e.email}</td>
              <td class="num">${e.semestreActual}</td><td class="num">${e.anioIngreso}</td>
              <td>${e.activo ? html`<span class="badge badge-AUTORIZADA">Activo</span>` : html`<span class="badge badge-REPROBADA">Inactivo</span>`}</td>
              <td class="acciones"><a class="btn btn-chico" href="#/historial/${e.id}">${icono('clock')} Historial</a></td>
            </tr>`)}</tbody></table></div>
          <p class="pie-tabla">${datos.length} estudiante${datos.length === 1 ? '' : 's'}</p>`.toString();
    } catch (e) {
      if (mio === pedido) lista.innerHTML = alertaError(e).toString();
    }
  }

  buscar.addEventListener('input', debounce(cargar, 300));
  zona.querySelector('#nuevo').addEventListener('click', () => {
    modalFormulario({
      titulo: 'Registrar estudiante',
      textoOk: 'Registrar', textoCarga: 'Registrando…',
      introduccion: html`<p class="subtitulo">El coordinador define la contraseña inicial (S-23). Usa datos ficticios en la demostración.</p>`,
      campos: [
        campo({ nombre: 'nombre', etiqueta: 'Nombre', requerido: true, atributos: { maxlength: 80, autocomplete: 'off' } }),
        campo({ nombre: 'apellidos', etiqueta: 'Apellidos', requerido: true, atributos: { maxlength: 120, autocomplete: 'off' } }),
        campo({ nombre: 'email', etiqueta: 'Correo electrónico', tipo: 'email', requerido: true, atributos: { maxlength: 150, autocomplete: 'off' } }),
        campo({ nombre: 'rut', etiqueta: 'RUT', requerido: true, ayuda: 'Con o sin puntos; se valida el dígito verificador.', atributos: { maxlength: 12, placeholder: '12345678-5', autocomplete: 'off' } }),
        campo({ nombre: 'password', etiqueta: 'Contraseña inicial', tipo: 'password', requerido: true, ayuda: 'De 8 a 72 caracteres.', atributos: { minlength: 8, maxlength: 72, autocomplete: 'new-password' } }),
        campo({ nombre: 'semestreActual', etiqueta: 'Semestre actual', tipo: 'number', requerido: true, atributos: { min: 1, max: 12 } }),
        campo({ nombre: 'anioIngreso', etiqueta: 'Año de ingreso', tipo: 'number', requerido: true, atributos: { min: 2000, max: 2100, placeholder: '2024' } }),
      ],
      alEnviar: (datos) => api.post('/api/estudiantes', datos),
      alListo: (e) => { toast(`Estudiante ${e.nombre} ${e.apellidos} registrado.`); cargar(); },
    });
  });
  await cargar();
}

// ============================ PROFESORES ============================
async function tabProfesores(zona) {
  zona.innerHTML = html`
    <div class="barra-herramientas">
      <label class="filtro">Rol
        <select id="rol"><option value="">Todos</option><option value="PROFESOR_GUIA">Profesor guía</option><option value="PROFESOR_CURSO">Profesor del curso</option><option value="TUTOR">Tutor</option></select>
      </label>
      <button type="button" class="btn btn-primario" id="nuevo">${icono('plus')} Registrar profesor</button>
    </div>
    <div id="lista"></div>`.toString();
  const lista = zona.querySelector('#lista');
  const filtro = zona.querySelector('#rol');

  async function cargar() {
    lista.innerHTML = cargando('Cargando profesores…').toString();
    try {
      const datos = await api.get('/api/profesores', { rol: filtro.value });
      lista.innerHTML = datos.length === 0
        ? vacio('No hay profesores registrados', 'Registra un profesor guía o un profesor del curso.', 'graduation').toString()
        : html`<div class="tabla-contenedor"><table class="tabla">
            <thead><tr><th>Profesor</th><th>Correo</th><th>Rol</th><th>Estado</th></tr></thead>
            <tbody>${datos.map((p) => html`<tr>
              <td><strong>${p.nombre} ${p.apellidos}</strong></td><td>${p.email}</td>
              <td>${[p.rol, p.rolSecundario].filter(Boolean).map((r) => html`<span class="badge badge-rol">${nombreRol(r)}</span> `)}</td>
              <td>${p.activo ? html`<span class="badge badge-AUTORIZADA">Activo</span>` : html`<span class="badge badge-REPROBADA">Inactivo</span>`}</td>
            </tr>`)}</tbody></table></div>
          <p class="pie-tabla">${datos.length} profesor${datos.length === 1 ? '' : 'es'}</p>`.toString();
    } catch (e) {
      lista.innerHTML = alertaError(e).toString();
    }
  }

  filtro.addEventListener('change', cargar);
  zona.querySelector('#nuevo').addEventListener('click', () => {
    modalFormulario({
      titulo: 'Registrar profesor', textoOk: 'Registrar', textoCarga: 'Registrando…',
      campos: [
        campo({ nombre: 'nombre', etiqueta: 'Nombre', requerido: true, atributos: { maxlength: 80, autocomplete: 'off' } }),
        campo({ nombre: 'apellidos', etiqueta: 'Apellidos', requerido: true, atributos: { maxlength: 120, autocomplete: 'off' } }),
        campo({ nombre: 'email', etiqueta: 'Correo electrónico', tipo: 'email', requerido: true, atributos: { maxlength: 150, autocomplete: 'off' } }),
        campo({ nombre: 'password', etiqueta: 'Contraseña inicial', tipo: 'password', requerido: true, ayuda: 'De 8 a 72 caracteres.', atributos: { minlength: 8, maxlength: 72, autocomplete: 'new-password' } }),
        campo({ nombre: 'rol', etiqueta: 'Rol', tipo: 'select', requerido: true, completo: true,
          opciones: [{ valor: 'PROFESOR_GUIA', texto: 'Profesor guía (colaborador del colegio)' }, { valor: 'PROFESOR_CURSO', texto: 'Profesor del curso (académico UBB)' },
            { valor: 'TUTOR', texto: 'Tutor (UBB, solo Práctica Profesional)' }] }),
      ],
      alEnviar: (datos) => api.post('/api/profesores', datos),
      alListo: (p) => { toast(`Profesor ${p.nombre} ${p.apellidos} registrado.`); cargar(); },
    });
  });
  await cargar();
}

// ============================ INSCRIPCIONES ============================
/** Tabla de inscripciones; `acciones(i)` devuelve el html de la última columna. */
export function tablaInscripciones(lista, { acciones = null } = {}) {
  return html`<div class="tabla-contenedor"><table class="tabla">
    <thead><tr><th>Estudiante</th><th>Práctica</th><th>Período</th><th>Profesor guía</th><th>Profesor del curso</th><th>Estado</th><th class="num">Nota</th>${acciones ? html`<th></th>` : ''}</tr></thead>
    <tbody>${lista.map((i) => html`<tr>
      <td><strong>${i.estudiante.nombreCompleto}</strong><br><small>${i.estudiante.rut}</small></td>
      <td>${i.asignatura.nombre}<br><small>${MODALIDADES[i.asignatura.modalidad] ?? i.asignatura.modalidad}</small></td>
      <td>${i.periodo}</td>
      <td>${i.profesorGuia ? i.profesorGuia.nombreCompleto : html`<small>No lleva guía</small>`}
        ${i.tutor ? html`<br><small>Tutor: ${i.tutor.nombreCompleto}</small>` : i.asignatura.llevaTutor ? html`<br><small>Sin tutor asignado</small>` : ''}</td>
      <td>${i.profesorCurso?.nombreCompleto ?? '—'}</td>
      <td>${badge(i.estado)}</td>
      <td class="num">${nota(i.notaFinal)}</td>
      ${acciones ? html`<td class="acciones">${acciones(i)}</td>` : ''}
    </tr>`)}</tbody></table></div>
    <p class="pie-tabla">${lista.length} inscripci${lista.length === 1 ? 'ón' : 'ones'}</p>`;
}

async function tabInscripciones(zona, ctx, { coordinador }) {
  const [catalogo, estudiantes] = await Promise.all([
    asignaturas(),
    coordinador ? api.get('/api/estudiantes') : Promise.resolve([]),
  ]);

  zona.innerHTML = html`
    <div class="barra-herramientas">
      ${coordinador ? html`<label class="filtro">Estudiante
        <select id="f-estudiante"><option value="">Todos</option>${estudiantes.map((e) => html`<option value="${e.id}">${e.nombre} ${e.apellidos}</option>`)}</select></label>` : ''}
      <label class="filtro">Práctica
        <select id="f-asignatura"><option value="">Todas</option>${catalogo.map((a) => html`<option value="${a.id}">${a.orden}. ${a.nombre}</option>`)}</select></label>
      <label class="filtro">Período
        <input type="text" id="f-periodo" placeholder="2026-2" maxlength="6" style="width:7em"></label>
      ${coordinador ? html`<button type="button" class="btn btn-primario" id="nueva">${icono('plus')} Nueva inscripción</button>` : ''}
    </div>
    <div id="lista"></div>`.toString();

  const lista = zona.querySelector('#lista');
  const filtros = {
    estudianteId: zona.querySelector('#f-estudiante'),
    asignaturaId: zona.querySelector('#f-asignatura'),
    periodo: zona.querySelector('#f-periodo'),
  };
  let pedido = 0;

  async function cargar() {
    const mio = ++pedido;
    lista.innerHTML = cargando('Cargando inscripciones…').toString();
    try {
      const params = Object.fromEntries(Object.entries(filtros).filter(([, el]) => el).map(([k, el]) => [k, el.value]));
      const datos = await api.get('/api/inscripciones', params);
      if (mio !== pedido) return;
      lista.innerHTML = datos.length === 0
        ? vacio('No hay inscripciones con esos filtros', coordinador ? 'Crea una con "Nueva inscripción".' : 'Cuando el coordinador te inscriba o te asigne estudiantes, aparecerán aquí.', 'clipboard').toString()
        : tablaInscripciones(datos, { acciones: (i) => accionesInscripcion(i, ctx, coordinador) }).toString();
      lista.querySelectorAll('[data-cerrar-inscripcion]').forEach((b) => {
        b.addEventListener('click', () => cerrarInscripcion(datos.find((x) => String(x.id) === b.dataset.cerrarInscripcion), cargar));
      });
      lista.querySelectorAll('[data-tutor-inscripcion]').forEach((b) => {
        b.addEventListener('click', () => asignarTutor(datos.find((x) => String(x.id) === b.dataset.tutorInscripcion), cargar));
      });
    } catch (e) {
      if (mio === pedido) lista.innerHTML = alertaError(e).toString();
    }
  }

  Object.values(filtros).forEach((el) => {
    if (!el) return;
    el.addEventListener(el.tagName === 'SELECT' ? 'change' : 'input', el.tagName === 'SELECT' ? cargar : debounce(cargar, 300));
  });
  zona.querySelector('#nueva')?.addEventListener('click', () => nuevaInscripcion(estudiantes, catalogo, cargar));
  await cargar();
}

function accionesInscripcion(i, ctx, coordinador) {
  const historial = ctx.tiene('ESTUDIANTE') ? '#/historial' : `#/historial/${i.estudiante.id}`;
  const enlaces = [html`<a class="btn btn-chico" href="${historial}">${icono('clock')} Historial</a>`];
  if (i.estado === 'EN_CURSO') {
    if (ctx.tiene('ESTUDIANTE') && i.asignatura.requierePlanificacion) {
      enlaces.push(html` <a class="btn btn-chico" href="#/planificaciones/nueva?inscripcionId=${i.id}">${icono('file')} Planificar</a>`);
    }
    // solo donde es el guía o el tutor asignado (cuentas con dos roles; HU-07)
    if (ctx.actuaComo('PROFESOR_GUIA', i) || ctx.actuaComo('TUTOR', i)) {
      enlaces.push(html` <a class="btn btn-chico" href="#/evaluaciones/nueva?inscripcionId=${i.id}">${icono('check')} Evaluar</a>`);
    }
    if (coordinador && i.asignatura.llevaTutor) {
      enlaces.push(html` <button type="button" class="btn btn-chico" data-tutor-inscripcion="${i.id}">Tutor</button>`);
    }
    if (coordinador) {
      enlaces.push(html` <button type="button" class="btn btn-chico" data-cerrar-inscripcion="${i.id}">Cerrar</button>`);
    }
  }
  return enlaces;
}

async function nuevaInscripcion(estudiantes, catalogo, alTerminar) {
  const [guias, cursos] = await Promise.all([
    api.get('/api/profesores', { rol: 'PROFESOR_GUIA' }),
    api.get('/api/profesores', { rol: 'PROFESOR_CURSO' }),
  ]);
  const nombre = (p) => ({ valor: p.id, texto: `${p.nombre} ${p.apellidos}` });

  const m = modalFormulario({
    titulo: 'Nueva inscripción', textoOk: 'Inscribir', textoCarga: 'Inscribiendo…',
    campos: [
      campo({ nombre: 'estudianteId', etiqueta: 'Estudiante', tipo: 'select', requerido: true, completo: true, atributos: { 'data-numero': true },
        opciones: estudiantes.map((e) => ({ valor: e.id, texto: `${e.nombre} ${e.apellidos} (${e.rut})` })) }),
      campo({ nombre: 'asignaturaId', etiqueta: 'Práctica', tipo: 'select', requerido: true, atributos: { 'data-numero': true },
        opciones: catalogo.map((a) => ({ valor: a.id, texto: `${a.orden}. ${a.nombre}` })) }),
      campo({ nombre: 'periodo', etiqueta: 'Período (año-semestre)', requerido: true, ayuda: 'Formato AAAA-S, por ejemplo 2026-2.',
        atributos: { pattern: '[0-9]{4}-[12]', title: 'Debe tener el formato AAAA-S, por ejemplo 2026-2.', maxlength: 6, placeholder: '2026-2', autocomplete: 'off' } }),
      campo({ nombre: 'profesorGuiaId', etiqueta: 'Profesor guía', tipo: 'select', atributos: { 'data-numero': true, disabled: true },
        vacioTexto: 'Elige primero la práctica', opciones: guias.map(nombre) }),
      campo({ nombre: 'profesorCursoId', etiqueta: 'Profesor del curso', tipo: 'select', requerido: true, atributos: { 'data-numero': true },
        opciones: cursos.map(nombre) }),
      html`<p class="ayuda completo" id="info-asignatura" style="grid-column:1/-1"></p>`,
    ],
    alEnviar: (datos) => {
      const cuerpo = { ...datos };
      if (!cuerpo.profesorGuiaId) delete cuerpo.profesorGuiaId;
      return api.post('/api/inscripciones', cuerpo);
    },
    alListo: (i) => { toast(`${i.estudiante.nombreCompleto} inscrito(a) en ${i.asignatura.nombre}.`); alTerminar(); },
  });

  // El guía solo existe donde se va a un colegio (S-34): se habilita según la práctica elegida.
  const selAsig = m.form.elements.asignaturaId;
  const selGuia = m.form.elements.profesorGuiaId;
  const info = m.el.querySelector('#info-asignatura');
  selAsig.addEventListener('change', () => {
    const a = catalogo.find((x) => String(x.id) === selAsig.value);
    if (!a) { selGuia.disabled = true; selGuia.value = ''; info.textContent = ''; return; }
    selGuia.disabled = !a.requiereCentroEducativo;
    selGuia.required = a.requiereCentroEducativo;
    if (!a.requiereCentroEducativo) selGuia.value = '';
    selGuia.options[0].textContent = a.requiereCentroEducativo ? 'Seleccione…' : 'No aplica: esta práctica no va a un colegio';
    info.textContent = `Modalidad: ${MODALIDADES[a.modalidad]}. ${a.requiereCentroEducativo ? 'Se realiza en un colegio: el profesor guía es obligatorio.' : 'No se realiza en un colegio: no lleva profesor guía.'}${a.requierePlanificacion ? '' : ' No entrega planificaciones.'}`;
  });
}

/** HU-07: asignar, cambiar o quitar el tutor de una inscripción de Práctica Profesional (solo el coordinador). */
async function asignarTutor(inscripcion, alTerminar) {
  if (!inscripcion) return;
  const tutores = await api.get('/api/profesores', { rol: 'TUTOR' });
  modalFormulario({
    titulo: 'Tutor de la práctica', chico: true, textoOk: 'Guardar', textoCarga: 'Guardando…',
    introduccion: html`<p><strong>${inscripcion.estudiante.nombreCompleto}</strong><br>${inscripcion.asignatura.nombre} · ${inscripcion.periodo}</p>`,
    campos: [
      campo({ nombre: 'tutorId', etiqueta: 'Tutor', tipo: 'select', completo: true, atributos: { 'data-numero': true },
        valor: inscripcion.tutor?.id ?? '', vacioTexto: 'Sin tutor',
        ayuda: tutores.length ? 'Las evaluaciones que ya hizo un tutor anterior quedan a su nombre.' : 'No hay tutores registrados: regístralos en la pestaña Profesores.',
        opciones: tutores.filter((t) => t.activo).map((t) => ({ valor: t.id, texto: `${t.nombre} ${t.apellidos}` })) }),
    ],
    alEnviar: (datos) => api.put(`/api/inscripciones/${inscripcion.id}/tutor`, { tutorId: datos.tutorId || null }),
    alListo: (i) => { toast(i.tutor ? `Tutor asignado: ${i.tutor.nombreCompleto}.` : 'La inscripción quedó sin tutor.'); alTerminar(); },
  });
}

function cerrarInscripcion(inscripcion, alTerminar) {
  if (!inscripcion) return;
  modalFormulario({
    titulo: 'Cerrar inscripción', chico: true, textoOk: 'Cerrar inscripción', textoCarga: 'Cerrando…',
    introduccion: html`<p><strong>${inscripcion.estudiante.nombreCompleto}</strong><br>${inscripcion.asignatura.nombre} · ${inscripcion.periodo}</p>`,
    campos: [
      campo({ nombre: 'estado', etiqueta: 'Resultado', tipo: 'select', requerido: true, completo: true,
        opciones: [{ valor: 'APROBADA', texto: 'Aprobada' }, { valor: 'REPROBADA', texto: 'Reprobada' }] }),
      campo({ nombre: 'notaFinal', etiqueta: 'Nota final', tipo: 'number', requerido: true, completo: true, ayuda: 'Entre 1,0 y 7,0 con un decimal (usa punto: 5.8).',
        atributos: { min: 1, max: 7, step: 0.1 } }),
    ],
    alEnviar: (datos) => api.put(`/api/inscripciones/${inscripcion.id}/cierre`, datos),
    alListo: () => { toast('Inscripción cerrada.'); alTerminar(); },
  });
}

// ============================ OTROS ROLES ============================
async function renderMisInscripciones(cont, ctx) {
  ctx.titulo(ctx.veTodo ? 'Inscripciones' : 'Mis inscripciones');
  const titulo = ctx.tiene('ESTUDIANTE') ? 'Mis inscripciones en las prácticas'
    : ctx.veTodo ? 'Inscripciones de todos los estudiantes' : 'Inscripciones de mis estudiantes';
  cont.innerHTML = html`
    ${cabecera({ hu: 'HU-02', titulo,
      subtitulo: ctx.tiene('ESTUDIANTE')
        ? 'Las prácticas que cursas y las que ya aprobaste. La inscripción la registra el coordinador.'
        : ctx.veTodo
          ? `Solo lectura: la inscripción la registra el coordinador.${ctx.tiene('PROFESOR_GUIA') ? ' Donde eres el profesor guía aparece el botón «Evaluar».' : ''}`
          : `Solo ves los estudiantes que el coordinador te asignó. La inscripción la registra el coordinador.${ctx.tiene('TUTOR') ? ' Donde eres tutor aparece el botón «Evaluar».' : ''}` })}
    <div id="zona"></div>`.toString();
  await tabInscripciones(cont.querySelector('#zona'), ctx, { coordinador: false });
}
