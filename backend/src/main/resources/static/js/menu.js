// Mapa de funciones del sistema: cada una con los roles que la ven. La barra lateral se arma desde aquí.
// Son solo los nombres de las funciones a las que el usuario tiene acceso, sin mencionar sprints ni historias
// de usuario: el sistema se presenta terminado, no como un tablero de desarrollo.
//
// Una cuenta puede tener dos roles (cuentas especiales, por ejemplo el jefe de carrera que también es profesor guía):
// el menú muestra la UNIÓN de las funciones de sus roles, sin repetir las que comparten la misma ruta.

const JEFE = 'JEFE_CARRERA';
export const TODOS = ['ESTUDIANTE', 'PROFESOR_GUIA', 'PROFESOR_CURSO', 'COORDINADOR', 'TUTOR'];
const COORD = ['COORDINADOR'];
const NO_COORD = ['ESTUDIANTE', 'PROFESOR_GUIA', 'PROFESOR_CURSO', 'TUTOR'];
// El jefe de carrera lee lo mismo que el coordinador (sin modificar nada) y además pide la cartola.
const LECTURA = [...TODOS, JEFE];

/** Roles de una cuenta: el principal primero y, si tiene, el segundo. */
export const rolesDe = (usuario) => [usuario.rol, usuario.rolSecundario].filter(Boolean);

/** Funciones disponibles: funcionan contra la API real. `alias` = otras rutas que activan el mismo ítem. */
export const FUNCIONES = [
  { ruta: 'cartola', icono: 'download', nombre: 'Cartola de prácticas', roles: [JEFE] },
  { ruta: 'cuenta', icono: 'shield', nombre: 'Mi cuenta', roles: LECTURA },
  {
    ruta: 'estudiantes', alias: ['profesores', 'inscripciones'], icono: 'users',
    nombre: 'Estudiantes e inscripciones', roles: COORD,
  },
  // Antes que "Mis inscripciones": con dos roles (jefe + guía) gana este nombre, porque ve todas
  { ruta: 'inscripciones', icono: 'users', nombre: 'Inscripciones', roles: [JEFE] },
  { ruta: 'inscripciones', icono: 'users', nombre: 'Mis inscripciones', roles: NO_COORD },
  { ruta: 'pautas', icono: 'clipboard', nombre: 'Pautas de evaluación', roles: COORD },
  { ruta: 'evaluaciones', icono: 'check', nombre: 'Evaluaciones con pauta digital', roles: LECTURA },
  { ruta: 'planificaciones', icono: 'file', nombre: 'Planificación de clases', roles: LECTURA },
  { ruta: 'historial', icono: 'clock', nombre: 'Historial del estudiante', roles: LECTURA },
  // HU-20: el jefe de carrera lee los chats (S-66). Antes que el del guía: con dos roles (jefe + guía) gana este nombre
  { ruta: 'chatbot', icono: 'chat', nombre: 'Conversaciones del asistente', roles: [JEFE] },
  // HU-15 (el estudiante conversa) y HU-16 (su profesor guía lee). SUPUESTO S-60: ni el coordinador, ni el profesor
  // del curso, ni el tutor ven los chats.
  { ruta: 'chatbot', icono: 'chat', nombre: 'Asistente de planificación', roles: ['ESTUDIANTE', 'PROFESOR_GUIA'] },
  // HU-17: el coordinador calibra el estilo del asistente
  { ruta: 'estilo-asistente', icono: 'sliders', nombre: 'Estilo del asistente', roles: COORD },
];

const tieneAlguno = (item, roles) => item.roles.some((r) => roles.includes(r));

/** Ítems que ve una cuenta con estos roles (lista), sin repetir rutas: el primero que aparece gana. */
export function visiblesPara(lista, roles) {
  const vistas = new Set();
  return lista.filter((i) => {
    if (!tieneAlguno(i, roles) || vistas.has(i.ruta)) return false;
    vistas.add(i.ruta);
    return true;
  });
}

/** Rutas permitidas según los roles (las sub-rutas como #/planificaciones/12 heredan la de su ítem). */
export function rutaPermitida(seccion, roles) {
  if (seccion === 'inicio') return true;
  return FUNCIONES.some((i) => (i.ruta === seccion || i.alias?.includes(seccion)) && tieneAlguno(i, roles));
}

/** Ítem del menú que debe verse activo para una sección. */
export function itemActivo(seccion, roles) {
  return visiblesPara(FUNCIONES, roles).find((i) => i.ruta === seccion || i.alias?.includes(seccion)) ?? null;
}

/** Tarjetas de acceso rápido de la pantalla de inicio, por rol (con dos roles se suman: ver accesosPara). */
export const ACCESOS = {
  COORDINADOR: [
    { icono: 'users', titulo: 'Registrar un estudiante', texto: 'Alta de estudiantes y profesores.', hash: '#/estudiantes' },
    { icono: 'clipboard', titulo: 'Inscribir en una práctica', texto: 'Asigna guía y profesor del curso.', hash: '#/inscripciones' },
    { icono: 'clock', titulo: 'Historial de un estudiante', texto: 'Recorrido por las 6 prácticas.', hash: '#/historial' },
    { icono: 'file', titulo: 'Seguimiento de planificaciones', texto: 'Estado de cada clase planificada.', hash: '#/planificaciones' },
    { icono: 'clipboard', titulo: 'Pautas de evaluación', texto: 'Crear y editar pautas (con versiones).', hash: '#/pautas' },
    { icono: 'sliders', titulo: 'Estilo del asistente', texto: 'Calibra cómo retroalimenta el chatbot.', hash: '#/estilo-asistente' },
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
  // HU-07: el tutor de Práctica Profesional evalúa con sus propias pautas
  TUTOR: [
    { icono: 'check', titulo: 'Evaluar una clase', texto: 'Pautas de tutor (Especialidad y Orientación).', hash: '#/evaluaciones/nueva' },
    { icono: 'users', titulo: 'Mis estudiantes', texto: 'Inscripciones de las que eres tutor.', hash: '#/inscripciones' },
    { icono: 'clock', titulo: 'Historial de mis estudiantes', texto: 'Recorrido completo.', hash: '#/historial' },
  ],
  JEFE_CARRERA: [
    { icono: 'download', titulo: 'Cartola de prácticas', texto: 'PDF de las prácticas cerradas entre dos fechas.', hash: '#/cartola' },
    { icono: 'clock', titulo: 'Historial de un estudiante', texto: 'Recorrido de cualquier estudiante (solo lectura).', hash: '#/historial' },
    { icono: 'check', titulo: 'Evaluaciones de clases', texto: 'Todas las evaluaciones (solo lectura).', hash: '#/evaluaciones' },
    { icono: 'chat', titulo: 'Conversaciones del asistente', texto: 'Chats de Práctica Pedagógica y Profesional.', hash: '#/chatbot' },
  ],
};

/**
 * Accesos rápidos de una cuenta: los de cada uno de sus roles, el principal primero y sin repetir destino.
 * Cada acceso lleva `rol`: el rol por el que aparece (el contador de pendientes cuenta solo lo de ese rol).
 */
export function accesosPara(roles) {
  const vistos = new Set();
  return roles.flatMap((rol) => (ACCESOS[rol] ?? []).map((a) => ({ ...a, rol })))
    .filter((a) => !vistos.has(a.hash) && vistos.add(a.hash));
}
