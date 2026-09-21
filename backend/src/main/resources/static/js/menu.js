// Mapa de funciones del sistema: las historias del Sprint 1 (sprint-backlog.md) y las próximas
// (product-backlog.md), cada una con los roles que la ven. La barra lateral se arma desde aquí.

export const TODOS = ['ESTUDIANTE', 'PROFESOR_GUIA', 'PROFESOR_CURSO', 'COORDINADOR'];
const COORD = ['COORDINADOR'];
const GUIA = ['PROFESOR_GUIA'];
const CURSO = ['PROFESOR_CURSO'];
const ESTU = ['ESTUDIANTE'];
const NO_COORD = ['ESTUDIANTE', 'PROFESOR_GUIA', 'PROFESOR_CURSO'];

/** Historias del Sprint 1: funcionan contra la API real. `alias` = otras rutas que activan el mismo ítem. */
export const SPRINT1 = [
  { hu: 'HU-01', ruta: 'cuenta', icono: 'shield', nombre: 'Iniciar sesión con roles', roles: TODOS },
  {
    hu: 'HU-02', ruta: 'estudiantes', alias: ['profesores', 'inscripciones'], icono: 'users',
    nombre: 'Registrar estudiantes e inscribirlos en una práctica', roles: COORD,
  },
  { hu: 'HU-02', ruta: 'inscripciones', icono: 'users', nombre: 'Mis inscripciones en las prácticas', roles: NO_COORD },
  { hu: 'HU-03', ruta: 'evaluaciones', icono: 'check', nombre: 'Evaluar una clase con la pauta digital (indicadores 1 a 7)', roles: TODOS },
  { hu: 'HU-04', ruta: 'planificaciones', icono: 'file', nombre: 'Planificar una clase y que la aprueben en dos etapas', roles: TODOS },
  { hu: 'HU-05', ruta: 'historial', icono: 'clock', nombre: 'Ver el historial del estudiante', roles: TODOS },
];

/** Historias futuras (no están en el Sprint 1): se muestran deshabilitadas con la etiqueta "Próximamente". */
export const PROXIMAMENTE = [
  { hu: 'HU-06', sprint: 2, icono: 'sliders', nombre: 'Configurar pautas de evaluación', roles: COORD },
  { hu: 'HU-07', sprint: 2, icono: 'check', nombre: 'Pautas de Práctica Pedagógica y Profesional (con tutor)', roles: GUIA },
  { hu: 'HU-08', sprint: 2, icono: 'chart', nombre: 'Informes por asignatura y por estudiante', roles: COORD },
  { hu: 'HU-09', sprint: 2, icono: 'award', nombre: 'Nota final ponderada de la asignatura', roles: CURSO },
  { hu: 'HU-10', sprint: 2, icono: 'users', nombre: 'Prácticas en dupla y en grupo', roles: COORD },
  { hu: 'HU-11', sprint: 3, icono: 'mic', nombre: 'Notas de voz', roles: GUIA },
  { hu: 'HU-12', sprint: 3, icono: 'mic', nombre: 'Pasar la nota de voz a texto', roles: GUIA },
  { hu: 'HU-13', sprint: 3, icono: 'download', nombre: 'Exportar notas finales para Adecca', roles: CURSO },
  { hu: 'HU-14', sprint: 3, icono: 'chart', nombre: 'Reportes de monitoreo (panel del coordinador)', roles: COORD },
  { hu: 'HU-19', sprint: 3, icono: 'bell', nombre: 'Avisos por correo de cambios de estado', roles: ['PROFESOR_GUIA', 'PROFESOR_CURSO'] },
  { hu: 'HU-15', sprint: 4, icono: 'chat', nombre: 'Asistente de retroalimentación (chatbot)', roles: ESTU },
  { hu: 'HU-16', sprint: 4, icono: 'chat', nombre: 'El profesor guía ve el chat y la actividad del estudiante', roles: GUIA },
  { hu: 'HU-17', sprint: 4, icono: 'sliders', nombre: 'Calibrar el estilo de retroalimentación del chatbot', roles: COORD },
  { hu: 'HU-20', sprint: 4, icono: 'chat', nombre: 'El jefe de carrera consulta los chats', roles: COORD },
  { hu: 'HU-18', sprint: 5, icono: 'upload', nombre: 'Registrar informes y productos de la práctica', roles: ESTU },
];

export const visiblesPara = (lista, rol) => lista.filter((i) => i.roles.includes(rol));

/** Rutas permitidas por rol (las sub-rutas como #/planificaciones/12 heredan la de su ítem). */
export function rutaPermitida(seccion, rol) {
  if (seccion === 'inicio') return true;
  return SPRINT1.some((i) => (i.ruta === seccion || i.alias?.includes(seccion)) && i.roles.includes(rol));
}

/** Ítem del menú que debe verse activo para una sección. */
export function itemActivo(seccion, rol) {
  return visiblesPara(SPRINT1, rol).find((i) => i.ruta === seccion || i.alias?.includes(seccion)) ?? null;
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
