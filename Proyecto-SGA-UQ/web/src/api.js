const BASE = '/api'

function consulta(parametros = {}) {
  const partes = []
  Object.keys(parametros).forEach((clave) => {
    const valor = parametros[clave]
    if (valor !== undefined && valor !== null && valor !== '') {
      partes.push(`${encodeURIComponent(clave)}=${encodeURIComponent(valor)}`)
    }
  })
  return partes.length ? `?${partes.join('&')}` : ''
}

async function pedir(ruta, opciones = {}) {
  const configuracion = {
    method: opciones.method || 'GET',
    headers: {},
  }
  if (opciones.body !== undefined) {
    configuracion.headers['Content-Type'] = 'application/json; charset=utf-8'
    configuracion.body = JSON.stringify(opciones.body)
  }
  const respuesta = await fetch(`${BASE}${ruta}`, configuracion)
  const texto = await respuesta.text()
  let datos = null
  if (texto) {
    try {
      datos = JSON.parse(texto)
    } catch {
      datos = { error: texto }
    }
  }
  if (!respuesta.ok) {
    throw new Error(datos?.error || `Error ${respuesta.status}`)
  }
  return datos
}

export const api = {
  resumen: () => pedir('/resumen'),
  tablero: () => pedir('/tablero'),
  reservas: () => pedir('/reservas'),
  reserva: (codigo) => pedir(`/reservas/${encodeURIComponent(codigo)}`),
  disponibilidad: (parametros) => pedir(`/disponibilidad${consulta(parametros)}`),
  cotizacion: (parametros) => pedir(`/cotizacion${consulta(parametros)}`),
  huespedes: (q = '') => pedir(`/huespedes${consulta({ q })}`),
  calendario: (parametros) => pedir(`/calendario${consulta(parametros)}`),
  preparacion: () => pedir('/preparacion'),
  mapa: () => pedir('/mapa'),
  estadisticas: () => pedir('/estadisticas'),
  canalExterno: () => pedir('/canal-externo'),
  temporadas: () => pedir('/temporadas'),

  crearReserva: (datos) => pedir('/reservas', { method: 'POST', body: datos }),
  accionReserva: (codigo, accion, datos = {}) =>
    pedir(`/reservas/${encodeURIComponent(codigo)}/${accion}`, {
      method: 'POST',
      body: datos,
    }),
  procesarVencimientos: () =>
    pedir('/reservas/vencimientos', { method: 'POST', body: {} }),

  crearHuesped: (datos) => pedir('/huespedes', { method: 'POST', body: datos }),
  actualizarHuesped: (documento, datos) =>
    pedir(`/huespedes/${encodeURIComponent(documento)}`, { method: 'PUT', body: datos }),
  alternarFavorito: (documento, apartamento) =>
    pedir(`/huespedes/${encodeURIComponent(documento)}/favoritos`, {
      method: 'POST',
      body: { apartamento },
    }),

  cambiarEstadoApartamento: (codigo, estado, motivo = '') =>
    pedir(`/apartamentos/${encodeURIComponent(codigo)}/estado`, {
      method: 'PATCH',
      body: { estado, motivo },
    }),

  accionPreparacion: (codigo, accion) =>
    pedir(`/preparacion/${encodeURIComponent(codigo)}/${accion}`, {
      method: 'POST',
      body: {},
    }),

  rutaMapa: (origen, destino) =>
    pedir('/mapa/ruta', { method: 'POST', body: { origen, destino } }),
  rutaLimpieza: (puntoPartida) =>
    pedir('/mapa/limpieza', { method: 'POST', body: { puntoPartida } }),
  pasillo: (a, b, abrir) =>
    pedir('/mapa/pasillo', { method: 'POST', body: { a, b, abrir } }),

  rotarTurno: () => pedir('/turno/rotar', { method: 'POST', body: {} }),
  crearSolicitud: (datos) =>
    pedir('/canal-externo', { method: 'POST', body: datos }),
  procesarCanal: () =>
    pedir('/canal-externo/procesar', { method: 'POST', body: {} }),
  asignarGrupo: (datos) => pedir('/grupos', { method: 'POST', body: datos }),
  crearTemporada: (datos) =>
    pedir('/temporadas', { method: 'POST', body: datos }),
  cargarDatos: (accion, datos = {}) =>
    pedir(`/datos/${accion}`, { method: 'POST', body: datos }),
}
