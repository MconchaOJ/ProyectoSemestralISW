// HU-09 · Estructura evaluativa y notas finales de una práctica en un período.
//   #/notas?asignaturaId=5&periodo=2026-2
// La estructura tiene componentes "de pauta" (la nota sale de las evaluaciones finalizadas con esa pauta) y
// "externos" (la escribe el profesor del curso: productos, informe final, TANEP). Entre todos suman 100 %.
// La editan el profesor del curso de la asignatura y el coordinador (S-83); el jefe de carrera solo la lee.
// Usa GET/PUT /api/asignaturas/{id}/estructura-evaluativa, GET /api/notas y PUT/DELETE
// /api/inscripciones/{id}/notas-externas/{componenteId}.
import { api } from '../api.js';
import { asignaturas } from '../datos.js';
import {
  alerta, alertaError, badge, cabecera, campo, cargando, conCarga, confirmar, html, icono, modalFormulario, nota,
  toast, vacio,
} from '../ui.js';

const TIPO = { PAUTA: 'De pauta', EXTERNO: 'Externo' };

export async function render(cont, ctx) {
  ctx.titulo('Notas finales');
  const [catalogo, inscripciones] = await Promise.all([asignaturas(), api.get('/api/inscripciones')]);
  const periodos = (asignaturaId) => [...new Set(inscripciones.filter((i) => String(i.asignatura.id) === String(asignaturaId))
    .map((i) => i.periodo))].sort().reverse();

  cont.innerHTML = html`
    ${cabecera({
      hu: 'HU-09', titulo: 'Notas finales',
      subtitulo: 'La nota final de cada práctica según su estructura evaluativa: una parte sale de las evaluaciones con pauta del sistema (nota de 1,0 a 7,0 con 60 % de exigencia) y otra la escribe el profesor del curso (productos, informe final, TANEP). Mientras falten notas se muestra la nota parcial con el porcentaje ya evaluado.',
    })}
    <div class="tarjeta">
      <form id="form-notas" class="rejilla-form" novalidate>
        <div class="campo">
          <label for="f-asignatura">Práctica</label>
          <select id="f-asignatura" name="asignaturaId">
            ${catalogo.map((a) => html`<option value="${a.id}">${a.orden}. ${a.nombre}</option>`)}
          </select>
        </div>
        <div class="campo">
          <label for="f-periodo">Período</label>
          <select id="f-periodo" name="periodo"></select>
        </div>
      </form>
    </div>
    <div id="estructura"></div>
    <div id="notas"></div>`.toString();

  const form = cont.querySelector('#form-notas');
  const selAsig = form.elements.asignaturaId;
  const selPer = form.elements.periodo;
  const zonaEstructura = cont.querySelector('#estructura');
  const zonaNotas = cont.querySelector('#notas');

  const llenarPeriodos = (elegido) => {
    const lista = periodos(selAsig.value);
    selPer.innerHTML = lista.length
      ? html`${lista.map((p) => html`<option value="${p}" ${p === elegido ? 'selected' : ''}>${p}</option>`)}`.toString()
      : '<option value="">Sin inscripciones</option>';
    selPer.disabled = lista.length === 0;
  };

  const ver = async () => {
    history.replaceState(null, '', `#/notas?asignaturaId=${selAsig.value}${selPer.value ? `&periodo=${selPer.value}` : ''}`);
    zonaEstructura.innerHTML = cargando().toString();
    zonaNotas.innerHTML = '';
    try {
      if (!selPer.value) {
        pintarEstructura(zonaEstructura, await api.get(`/api/asignaturas/${selAsig.value}/estructura-evaluativa`), ver);
        zonaNotas.innerHTML = vacio('No tienes inscripciones en esta práctica', 'Elige otra práctica para ver sus notas.', 'chart').toString();
        return;
      }
      const datos = await api.get('/api/notas', { asignaturaId: selAsig.value, periodo: selPer.value });
      pintarEstructura(zonaEstructura, datos.estructura, ver);
      pintarNotas(zonaNotas, datos, ver);
    } catch (e) {
      if (e.status !== 401) zonaEstructura.innerHTML = alertaError(e).toString();
    }
  };

  selAsig.addEventListener('change', () => { llenarPeriodos(); ver(); });
  selPer.addEventListener('change', ver);

  const pedida = ctx.consulta.get('asignaturaId');
  selAsig.value = pedida && catalogo.some((a) => String(a.id) === pedida) ? pedida
    : String((catalogo.find((a) => periodos(a.id).length) ?? catalogo[0]).id);
  llenarPeriodos(ctx.consulta.get('periodo'));
  await ver();
}

// ============================ ESTRUCTURA ============================
function totales(componentes) {
  const total = componentes.reduce((s, c) => s + (Number(c.porcentaje) || 0), 0);
  const pautas = componentes.filter((c) => c.tipo === 'PAUTA').reduce((s, c) => s + (Number(c.porcentaje) || 0), 0);
  return { total, pautas, externos: total - pautas };
}

function pintarEstructura(zona, est, recargar) {
  const t = totales(est.componentes);
  zona.innerHTML = html`<div class="tarjeta">
    <div class="tarjeta-titulo"><h2>Estructura evaluativa · ${est.asignatura}</h2>
      ${est.puedeEditar ? html`<button type="button" class="btn btn-chico" data-editar-estructura>${icono('edit')} Editar estructura</button>` : ''}</div>
    ${est.componentes.length === 0
      ? vacio('Esta práctica no tiene estructura evaluativa',
        est.puedeEditar ? 'Agrega sus componentes con «Editar estructura». Mientras no tenga, la nota final se escribe a mano al cerrar.'
          : 'La define el profesor del curso o el coordinador. Mientras no tenga, la nota final se escribe a mano al cerrar.', 'chart')
      : html`<div class="tabla-contenedor"><table class="tabla">
          <thead><tr><th>#</th><th>Componente</th><th>Tipo</th><th class="num">Porcentaje</th></tr></thead>
          <tbody>${est.componentes.map((c) => html`<tr>
            <td>${c.orden}</td>
            <td><strong>${c.nombre}</strong>${c.tipo === 'PAUTA' ? html`<br><small>Pauta: ${c.pautaNombre}${c.pautaVersion > 1 ? ` (versión ${c.pautaVersion})` : ''}</small>` : ''}</td>
            <td>${TIPO[c.tipo]}</td>
            <td class="num">${c.porcentaje} %</td>
          </tr>`)}</tbody></table></div>
        <p class="pie-tabla">De pautas del sistema: <strong>${t.pautas} %</strong> · Externos (los escribe el profesor del curso): <strong>${t.externos} %</strong> · Total: <strong>${t.total} %</strong></p>`}
  </div>`.toString();
  zona.querySelector('[data-editar-estructura]')?.addEventListener('click', () => editarEstructura(zona, est, recargar));
}

function editarEstructura(zona, est, recargar) {
  // Copia de trabajo: se edita aquí y se envía completa al guardar
  let filas = est.componentes.map((c) => ({ ...c }));
  const quitadas = [];

  const leer = () => {
    zona.querySelectorAll('tr[data-fila]').forEach((tr) => {
      const f = filas[Number(tr.dataset.fila)];
      f.nombre = tr.querySelector('[name=nombre]').value;
      f.porcentaje = tr.querySelector('[name=porcentaje]').value;
      const sel = tr.querySelector('[name=pautaId]');
      if (sel) f.pautaId = sel.value ? Number(sel.value) : null;
    });
  };

  const pintarTotal = () => {
    leer();
    const t = totales(filas);
    const caja = zona.querySelector('[data-total]');
    caja.innerHTML = alerta(t.total === 100 || filas.length === 0 ? 'info' : 'aviso',
      html`De pautas: <strong>${t.pautas} %</strong> · Externos: <strong>${t.externos} %</strong> · Total: <strong>${t.total} %</strong>${t.total === 100 || filas.length === 0 ? '' : ' (debe sumar 100 %)'}`).toString();
  };

  const pintar = () => {
    zona.innerHTML = html`<div class="tarjeta">
      <div class="tarjeta-titulo"><h2>Editar estructura evaluativa · ${est.asignatura}</h2></div>
      <p class="ayuda">Los componentes «de pauta» toman la nota de las evaluaciones finalizadas con esa pauta (si hay varias, su promedio). Los «externos» los califica otro docente fuera del sistema y su nota la escribe el profesor del curso. Puedes cambiarla a mitad de semestre: las notas se recalculan.</p>
      <div data-alerta-estructura></div>
      <div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>#</th><th>Tipo</th><th>Componente</th><th class="num">%</th><th></th></tr></thead>
        <tbody>${filas.map((f, i) => html`<tr data-fila="${i}">
          <td>${i + 1}</td>
          <td>${TIPO[f.tipo]}</td>
          <td>
            ${f.tipo === 'PAUTA' ? html`<select name="pautaId" aria-label="Pauta del componente ${i + 1}" style="width:100%;margin-bottom:.3rem">
                <option value="">Elige la pauta…</option>
                ${est.pautasDisponibles.map((p) => html`<option value="${p.id}" ${p.id === f.pautaId ? 'selected' : ''}>${p.nombre}${p.version > 1 ? ` (v${p.version})` : ''}${p.activa ? '' : ' (inactiva)'}</option>`)}
              </select>` : ''}
            <input name="nombre" value="${f.nombre ?? ''}" maxlength="150" style="width:100%"
              aria-label="Nombre del componente ${i + 1}" placeholder="${f.tipo === 'PAUTA' ? 'Nombre (vacío = el de la pauta)' : 'Ej. Producto 3: Informe final'}">
          </td>
          <td class="num"><input name="porcentaje" type="number" min="1" max="100" step="1" value="${f.porcentaje ?? ''}" style="width:5rem" aria-label="Porcentaje del componente ${i + 1}"></td>
          <td class="acciones">
            <button type="button" class="btn btn-chico btn-fantasma" data-subir="${i}" ${i === 0 ? 'disabled' : ''} aria-label="Subir">↑</button>
            <button type="button" class="btn btn-chico btn-fantasma" data-bajar="${i}" ${i === filas.length - 1 ? 'disabled' : ''} aria-label="Bajar">↓</button>
            <button type="button" class="btn btn-chico btn-fantasma" data-quitar="${i}" aria-label="Quitar">${icono('x')}</button>
          </td>
        </tr>`)}</tbody></table></div>
      ${filas.length === 0 ? html`<p class="subtitulo">Sin componentes: si guardas así, la práctica queda sin estructura evaluativa.</p>` : ''}
      <div data-total></div>
      <div class="acciones-form">
        <button type="button" class="btn" data-agregar="PAUTA">${icono('plus')} Componente de pauta</button>
        <button type="button" class="btn" data-agregar="EXTERNO">${icono('plus')} Componente externo</button>
        <span style="flex:1"></span>
        <button type="button" class="btn" data-cancelar>Cancelar</button>
        <button type="button" class="btn btn-primario" data-guardar data-cargando="Guardando…">${icono('check')} Guardar estructura</button>
      </div>
    </div>`.toString();
    pintarTotal();

    zona.querySelectorAll('input, select').forEach((el) => el.addEventListener('input', pintarTotal));
    zona.querySelectorAll('[data-agregar]').forEach((b) => b.addEventListener('click', () => {
      leer();
      filas.push({ id: null, tipo: b.dataset.agregar, nombre: '', pautaId: null, porcentaje: '' });
      pintar();
    }));
    zona.querySelectorAll('[data-quitar]').forEach((b) => b.addEventListener('click', () => {
      leer();
      quitadas.push(...filas.splice(Number(b.dataset.quitar), 1).filter((f) => f.id));
      pintar();
    }));
    const mover = (i, d) => { leer(); [filas[i], filas[i + d]] = [filas[i + d], filas[i]]; pintar(); };
    zona.querySelectorAll('[data-subir]').forEach((b) => b.addEventListener('click', () => mover(Number(b.dataset.subir), -1)));
    zona.querySelectorAll('[data-bajar]').forEach((b) => b.addEventListener('click', () => mover(Number(b.dataset.bajar), 1)));
    zona.querySelector('[data-cancelar]').addEventListener('click', () => pintarEstructura(zona, est, recargar));
    zona.querySelector('[data-guardar]').addEventListener('click', async (ev) => {
      leer();
      const boton = ev.currentTarget;
      const conNotas = quitadas.filter((f) => f.notasRegistradas > 0);
      if (conNotas.length) {
        const n = conNotas.reduce((s, f) => s + f.notasRegistradas, 0);
        const ok = await confirmar({
          titulo: 'Quitar componentes con notas', peligro: true, textoOk: 'Quitar y borrar sus notas',
          mensaje: `${conNotas.map((f) => `«${f.nombre}»`).join(', ')} ya tiene${conNotas.length > 1 ? 'n' : ''} ${n} nota${n === 1 ? '' : 's'} registrada${n === 1 ? '' : 's'}: se borrarán. Las notas finales ya guardadas al cerrar no cambian.`,
        });
        if (!ok) return;
      }
      const caja = zona.querySelector('[data-alerta-estructura]');
      caja.innerHTML = '';
      try {
        await conCarga(boton, () => api.put(`/api/asignaturas/${est.asignaturaId}/estructura-evaluativa`, {
          componentes: filas.map((f) => ({
            id: f.id ?? null, tipo: f.tipo, nombre: f.nombre?.trim() || null,
            pautaId: f.tipo === 'PAUTA' ? f.pautaId : null,
            porcentaje: f.porcentaje === '' || f.porcentaje == null ? null : Number(f.porcentaje),
          })),
        }));
        toast('Estructura evaluativa guardada: las notas se recalcularon.');
        await recargar();
      } catch (e) {
        if (e.status !== 401) caja.innerHTML = alertaError(e).toString();
      }
    });
  };
  pintar();
}

// ============================ NOTAS ============================
function celdaFinal(f) {
  if (f.estado !== 'EN_CURSO' && f.notaFinalGuardada != null) {
    return html`<strong>${nota(f.notaFinalGuardada)}</strong><br><small>guardada al cerrar</small>`;
  }
  if (f.completa) return html`<strong>${nota(f.notaPropuesta)}</strong><br><small>final (100 % evaluado)</small>`;
  if (f.notaParcial != null) return html`${nota(f.notaParcial)}<br><small>parcial: ${f.porcentajeEvaluado} % evaluado</small>`;
  return html`—<br><small>sin notas aún</small>`;
}

function pintarNotas(zona, datos, recargar) {
  const comps = datos.estructura.componentes;
  if (datos.inscripciones.length === 0) {
    zona.innerHTML = vacio('No hay inscripciones en este período', 'Elige otro período.', 'users').toString();
    return;
  }
  zona.innerHTML = html`<div class="tarjeta">
    <div class="tarjeta-titulo"><h2>Notas del período ${datos.periodo}</h2></div>
    ${comps.length === 0 ? alerta('info', 'Sin estructura evaluativa no hay nota calculada: la nota final se escribe a mano al cerrar la inscripción.') : ''}
    <div class="tabla-contenedor"><table class="tabla">
      <thead><tr><th>Estudiante</th>
        ${comps.map((c) => html`<th class="num" title="${c.nombre}">${c.orden}. ${c.porcentaje} %<br><small>${TIPO[c.tipo]}</small></th>`)}
        <th class="num">Nota</th></tr></thead>
      <tbody>${datos.inscripciones.map((f) => html`<tr>
        <td><strong>${f.estudiante.nombreCompleto}</strong><br>${badge(f.estado)}</td>
        ${f.componentes.map((c) => html`<td class="num">
          ${c.nota != null ? nota(c.nota) : '—'}
          ${c.tipo === 'PAUTA' && c.evaluaciones > 1 ? html`<br><small>promedio de ${c.evaluaciones}</small>` : ''}
          ${c.tipo === 'EXTERNO' && f.puedeEditarNotas
            ? html`<br><button type="button" class="btn btn-chico btn-fantasma" data-nota="${f.inscripcionId}:${c.componenteId}" aria-label="Escribir la nota de ${c.nombre} para ${f.estudiante.nombreCompleto}">${icono('edit')}</button>` : ''}
        </td>`)}
        <td class="num">${celdaFinal(f)}</td>
      </tr>`)}</tbody></table></div>
    <p class="pie-tabla">${datos.inscripciones.length} inscripci${datos.inscripciones.length === 1 ? 'ón' : 'ones'}. Pasa el cursor sobre cada columna para ver el nombre del componente. Las notas de pauta salen de las evaluaciones finalizadas; las externas, del lápiz de cada celda.</p>
  </div>`.toString();

  zona.querySelectorAll('[data-nota]').forEach((b) => b.addEventListener('click', () => {
    const [inscripcionId, componenteId] = b.dataset.nota.split(':').map(Number);
    const fila = datos.inscripciones.find((f) => f.inscripcionId === inscripcionId);
    const comp = fila.componentes.find((c) => c.componenteId === componenteId);
    modalFormulario({
      titulo: 'Nota de un componente externo', chico: true, textoOk: 'Guardar nota',
      introduccion: html`<p><strong>${fila.estudiante.nombreCompleto}</strong><br>${comp.nombre} · ${comp.porcentaje} %</p>
        ${comp.registradaPor ? html`<p class="ayuda">Última nota escrita por ${comp.registradaPor.nombreCompleto}.</p>` : ''}`,
      campos: [campo({ nombre: 'nota', etiqueta: 'Nota', tipo: 'number', completo: true, valor: comp.nota ?? '',
        ayuda: 'De 1,0 a 7,0 con un decimal (usa punto: 5.8). Déjala vacía para borrarla.', atributos: { min: 1, max: 7, step: 0.1 } })],
      alEnviar: (d) => (d.nota == null
        ? api.del(`/api/inscripciones/${inscripcionId}/notas-externas/${componenteId}`)
        : api.put(`/api/inscripciones/${inscripcionId}/notas-externas/${componenteId}`, { nota: d.nota })),
      alListo: async (r) => {
        toast(r.completa ? `Nota guardada. Nota final: ${nota(r.notaPropuesta)}.`
          : r.notaParcial != null ? `Nota guardada. Parcial: ${nota(r.notaParcial)} (${r.porcentajeEvaluado} % evaluado).` : 'Nota borrada.');
        await recargar();
      },
    });
  }));
}
