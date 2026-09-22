// Mapa de funciones del sistema: cada una con los roles que la ven. La barra lateral se arma desde aquí.
// Son solo los nombres de las funciones a las que el usuario tiene acceso, sin mencionar sprints ni historias
// de usuario: el sistema se presenta terminado, no como un tablero de desarrollo.

export const TODOS = ['ESTUDIANTE', 'PROFESOR_GUIA', 'PROFESOR_CURSO', 'COORDINADOR'];
const COORD = ['COORDINADOR'];
const NO_COORD = ['ESTUDIANTE', 'PROFESOR_GUIA', 'PROFESOR_CURSO'];

/** Funciones disponibles: funcionan contra la API real. `alias` = otras rutas que activan el mismo ítem. */
export const FUNCIONES = [
  { ruta: 'cuenta', icono: 'shield', nombre: 'Mi cuenta', roles: TODOS },
  {
    ruta: 'estudiantes', alias: ['profesores', 'inscripciones'], icono: 'users',
    nombre: 'Estudiantes e inscripciones', roles: COORD,
  },
  { ruta: 'inscripciones', icono: 'users', nombre: 'Mis inscripciones', roles: NO_COORD },
  { ruta: 'evaluaciones', icono: 'check', nombre: 'Evaluaciones con pauta digital', roles: TODOS },
  { ruta: 'planificaciones', icono: 'file', nombre: 'Planificación de clases', roles: TODOS },
  { ruta: 'historial', icono: 'clock', nombre: 'Historial del estudiante', roles: TODOS },
];

export const visiblesPara = (lista, rol) => lista.filter((i) => i.roles.includes(rol));

/** Rutas permitidas por rol (las sub-rutas como #/planificaciones/12 heredan la de su ítem). */
export function rutaPermitida(seccion, rol) {
  if (seccion === 'inicio') return true;
  return FUNCIONES.some((i) => (i.ruta === seccion || i.alias?.includes(seccion)) && i.roles.includes(rol));
}

/** Ítem del menú que debe verse activo para una sección. */
export function itemActivo(seccion, rol) {
  return visiblesPara(FUNCIONES, rol).find((i) => i.ruta === seccion || i.alias?.includes(seccion)) ?? null;
}

/** Tarjetas de acceso rápido de la pantalla de inicio, por rol. */
export const ACCESOS = {
  COORDINADOR: [
    { icono: 'users', titulo: 'Registrar un estudiante', texto: 'Alta de estudiantes y profesores.', hash: '#/estudiantes' },
    { icono: 'clipboard', titulo: 'Inscribir en una práctica', texto: 'Asigna guía y profesor del curso.', hash: '#/inscripciones' },
    { icono: 'clock', titulo: 'Historial de un estudiante', texto: 'Recorrido por las 6 prácticas.', hash: '#/historial' },
    { icono: 'file', titulo: 'Seguimiento de planificaciones', texto: 'Estado de cada clase planificada.', hash: '#/planificaciones' },
  ],
  PROFESOR_GUIA: [
    { icono: 'file', titulo: 'Planificaciones por revisar', texto: 'Aprueba u observa antes de la clase.', hash: '#/planificaciones?estado=EN_REVISION_GUIA', pendiente: { ruta: '/api/planificaciones', params: { estado: 'EN_REVISION_GUIA' } } },
    { icono: 'check', titulo: 'Evaluar una clase', texto: 'Pauta digital de 20 indicadores.', hash: '#/evaluaciones/nueva' },
    { icono: 'clock', titulo: 'Historial de mis estudiantes', texto: 'Cómo evolucionan en el tiempo.', hash: '#/historial' },
  ],
  PROFESOR_CURSO: [
    { icono: 'file', titulo: 'Planificaciones por autorizar', texto: 'Segunda etapa del flujo.', hash: '#/planificaciones?estado=EN_AUTORIZACION', pendiente: { ruta: '/api/planificaciones', params: { estado: 'EN_AUTORIZACION' } } },
    { icono: 'users', titulo: 'Mis inscripciones', texto: 'Estudiantes a tu cargo.', hash: '#/inscripciones' },
    { icono: 'clock', titulo: 'Historial de mis estudiantes', texto: 'Recorrido completo.', hash: '#/historial' },
  ],
  ESTUDIANTE: [
    { icono: 'file', titulo: 'Planificar una clase', texto: 'Crea tu borrador y envíalo a revisión.', hash: '#/planificaciones/nueva' },
    { icono: 'check', titulo: 'Mis evaluaciones', texto: 'Resultados de las clases observadas.', hash: '#/evaluaciones' },
    { icono: 'clock', titulo: 'Mi historial', texto: 'Tu recorrido por las prácticas.', hash: '#/historial' },
    { icono: 'users', titulo: 'Mis inscripciones', texto: 'Prácticas que cursas y aprobaste.', hash: '#/inscripciones' },
  ],
};
