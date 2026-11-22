// Consultas compartidas entre vistas (catálogos que casi no cambian) y textos comunes.
import { api } from './api.js';

let asignaturasEnMemoria = null;

/** Las 6 asignaturas de práctica (se piden una sola vez por sesión). */
export function asignaturas() {
  if (!asignaturasEnMemoria) {
    asignaturasEnMemoria = api.get('/api/asignaturas').catch((e) => { asignaturasEnMemoria = null; throw e; });
  }
  return asignaturasEnMemoria;
}

/**
 * HU-27: parámetros del sistema como { clave: valor }. Se piden cada vez (sin guardarlos) para que un cambio del
 * coordinador se vea en la pantalla siguiente; si la petición falla, rigen los valores de siempre.
 */
export async function parametros() {
  const porDefecto = {
    'planificacion.dias_anticipacion': 3, 'panel.dias_atencion': 3, 'archivos.documento_mb': 10, 'archivos.audio_mb': 25,
    'evaluacion.exigencia_pct': 60,
  };
  try {
    return { ...porDefecto, ...Object.fromEntries((await api.get('/api/parametros')).map((p) => [p.clave, p.valor])) };
  } catch {
    return porDefecto;
  }
}

/** "Camila Rojas Soto · Trabajo Colaborativo en el Aula (2026-2)" */
export const etiquetaInscripcion = (i) =>
  `${i.estudiante.nombreCompleto} · ${i.asignatura.nombre} (${i.periodo})`;

export const opcionesInscripcion = (lista) =>
  lista.map((i) => ({ valor: i.id, texto: etiquetaInscripcion(i) }));

export const DESCRIPCION_ROL = {
  ESTUDIANTE: [
    'Ver tus inscripciones en las prácticas y tu historial completo.',
    'Escribir tus planificaciones de clase y enviarlas a revisión.',
    'Ver las evaluaciones finalizadas de tus clases.',
  ],
  PROFESOR_GUIA: [
    'Revisar las planificaciones de tus estudiantes: aprobarlas u observarlas con un comentario.',
    'Evaluar las clases con la pauta digital (20 indicadores, escala 1 a 7).',
    'Ver el historial completo de los estudiantes que tienes asignados.',
  ],
  PROFESOR_CURSO: [
    'Autorizar (o devolver con observaciones) las planificaciones ya revisadas por el guía o, en el ciclo inicial, directamente enviadas.',
    'Ver las evaluaciones y el historial de los estudiantes de tu curso.',
    'Definir la estructura evaluativa de tus asignaturas y escribir las notas externas (productos, informe final, TANEP) en «Notas finales»: las notas de pauta se calculan solas.',
  ],
  COORDINADOR: [
    'Registrar estudiantes y profesores, e inscribir a los estudiantes en una práctica.',
    'Cerrar inscripciones con su estado final y nota.',
    'Crear y editar las pautas de evaluación (si una pauta ya se usó, al editarla se crea una versión nueva).',
    'Ver todas las planificaciones, evaluaciones e historiales (los borradores de evaluación también).',
    'Descargar informes por asignatura y por estudiante en PDF o Excel.',
    'Monitorear en un panel las planificaciones pendientes, las evaluaciones hechas, los estudiantes sin evaluar y el avance de cada estudiante.',
  ],
  TUTOR: [
    'Evaluar las clases de los estudiantes de Práctica Profesional de los que eres tutor, con las pautas de tutor (Especialidad y Orientación, escala 2 a 7).',
    'Ver sus inscripciones, planificaciones, evaluaciones finalizadas e historial (solo lectura).',
  ],
  JEFE_CARRERA: [
    'Obtener la cartola de prácticas: un PDF con las prácticas cerradas entre dos fechas (evidencia para la acreditación).',
    'Descargar informes por asignatura y por estudiante en PDF o Excel.',
    'Ver las inscripciones, planificaciones, evaluaciones e historiales de todos los estudiantes, como el coordinador.',
    'Como jefe de carrera es solo lectura: no crea, edita, aprueba ni evalúa (si también eres profesor guía, eso lo haces con tus estudiantes asignados).',
  ],
};
