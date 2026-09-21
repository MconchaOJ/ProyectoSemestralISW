// Cliente de la API REST y manejo de la sesión (JWT en sessionStorage).
// El front-end vive en el mismo origen que la API: no hay CORS.

const K_TOKEN = 'pem.token';
const K_USUARIO = 'pem.usuario';
const K_EXPIRA = 'pem.expira';

// sessionStorage puede fallar (modo privado, bloqueado): se cae a memoria para no romper la página.
const memoria = {};
function leer(clave) {
  try { return sessionStorage.getItem(clave); } catch { return memoria[clave] ?? null; }
}
function escribir(clave, valor) {
  try { sessionStorage.setItem(clave, valor); } catch { memoria[clave] = valor; }
}
function borrar(clave) {
  try { sessionStorage.removeItem(clave); } catch { /* nada */ }
  delete memoria[clave];
}

export const sesion = {
  guardar({ token, usuario, expiraEnSegundos }) {
    escribir(K_TOKEN, token);
    escribir(K_USUARIO, JSON.stringify(usuario));
    escribir(K_EXPIRA, String(Date.now() + expiraEnSegundos * 1000));
  },
  token() { return leer(K_TOKEN); },
  usuario() {
    try { return JSON.parse(leer(K_USUARIO)); } catch { return null; }
  },
  expira() { return Number(leer(K_EXPIRA)) || 0; },
  /** Hay token, usuario y todavía no vence (el servidor igual lo vuelve a validar en cada petición). */
  vigente() {
    return Boolean(this.token() && this.usuario() && this.expira() > Date.now());
  },
  limpiar() {
    borrar(K_TOKEN); borrar(K_USUARIO); borrar(K_EXPIRA);
  },
};

/** Error de la API con el cuerpo uniforme del backend: { status, mensaje, errores: [{campo, mensaje}] }. */
export class ApiError extends Error {
  constructor(status, mensaje, errores = []) {
    super(mensaje);
    this.status = status;
    this.errores = errores;
  }
}

let saliendo = false;

/** 401 (token vencido o inválido): se limpia la sesión y se vuelve al login. */
function sesionExpirada() {
  if (saliendo) return;
  saliendo = true;
  sesion.limpiar();
  location.replace('index.html?expirada=1');
}

async function pedir(metodo, ruta, { cuerpo, params, publico = false } = {}) {
  let url = ruta;
  if (params) {
    const q = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && String(v).trim() !== '') q.set(k, String(v).trim());
    });
    const texto = q.toString();
    if (texto) url += '?' + texto;
  }

  const cabeceras = { Accept: 'application/json' };
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';
  if (!publico && sesion.token()) cabeceras.Authorization = 'Bearer ' + sesion.token();

  let respuesta;
  try {
    respuesta = await fetch(url, {
      method: metodo,
      headers: cabeceras,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    });
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor. Revisa que la aplicación esté encendida.');
  }

  let datos = null;
  const texto = await respuesta.text();
  if (texto) {
    try { datos = JSON.parse(texto); } catch { datos = null; }
  }

  if (!respuesta.ok) {
    if (respuesta.status === 401 && !publico) sesionExpirada();
    throw new ApiError(respuesta.status, datos?.mensaje || respuesta.statusText || 'Error inesperado', datos?.errores || []);
  }
  return datos;
}

export const api = {
  get: (ruta, params) => pedir('GET', ruta, { params }),
  post: (ruta, cuerpo) => pedir('POST', ruta, { cuerpo: cuerpo ?? {} }),
  put: (ruta, cuerpo) => pedir('PUT', ruta, { cuerpo: cuerpo ?? {} }),
  login: (email, password) => pedir('POST', '/api/auth/login', { cuerpo: { email, password }, publico: true }),
};
