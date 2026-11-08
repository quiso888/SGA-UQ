const MESES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
]

const MESES_CORTOS = [
  'ene', 'feb', 'mar', 'abr', 'may', 'jun',
  'jul', 'ago', 'sep', 'oct', 'nov', 'dic',
]

const DIAS = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb']

export function fecha(iso) {
  if (!iso) return '—'
  const [anio, mes, dia] = iso.split('-').map(Number)
  if (!anio || !mes || !dia) return iso
  return `${dia} ${MESES_CORTOS[mes - 1]} ${anio}`
}

export function fechaLarga(iso) {
  if (!iso) return '—'
  const [anio, mes, dia] = iso.split('-').map(Number)
  if (!anio || !mes || !dia) return iso
  return `${dia} de ${MESES[mes - 1]} de ${anio}`
}

export function fechaHora(texto) {
  if (!texto) return '—'
  if (texto.length <= 10) return fecha(texto)
  const [dia, resto] = [texto.slice(0, 10), texto.slice(11, 16)]
  return `${fecha(dia)} · ${resto}`
}

export function diaSemana(iso) {
  if (!iso) return ''
  const [anio, mes, dia] = iso.split('-').map(Number)
  return DIAS[new Date(anio, mes - 1, dia).getDay()]
}

export function diaNumero(iso) {
  if (!iso) return ''
  return iso.slice(8, 10).replace(/^0/, '')
}

export function hoyISO() {
  const ahora = new Date()
  const anio = ahora.getFullYear()
  const mes = String(ahora.getMonth() + 1).padStart(2, '0')
  const dia = String(ahora.getDate()).padStart(2, '0')
  return `${anio}-${mes}-${dia}`
}

export function sumarDias(iso, dias) {
  const [anio, mes, dia] = iso.split('-').map(Number)
  const fechaBase = new Date(anio, mes - 1, dia)
  fechaBase.setDate(fechaBase.getDate() + dias)
  const y = fechaBase.getFullYear()
  const m = String(fechaBase.getMonth() + 1).padStart(2, '0')
  const d = String(fechaBase.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function diferenciaDias(desde, hasta) {
  if (!desde || !hasta) return 0
  const [a1, m1, d1] = desde.split('-').map(Number)
  const [a2, m2, d2] = hasta.split('-').map(Number)
  return Math.round((new Date(a2, m2 - 1, d2) - new Date(a1, m1 - 1, d1)) / 86400000)
}

export function moneda(valor) {
  const numero = Number(valor) || 0
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: 'COP',
    maximumFractionDigits: 0,
  }).format(numero)
}

export function monedaCorta(valor) {
  const numero = Number(valor) || 0
  if (Math.abs(numero) >= 1_000_000) {
    return `$${(numero / 1_000_000).toFixed(numero % 1_000_000 === 0 ? 0 : 1)} M`
  }
  if (Math.abs(numero) >= 1_000) {
    return `$${Math.round(numero / 1_000)} mil`
  }
  return moneda(numero)
}

export function numero(valor) {
  return new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 }).format(valor || 0)
}

export function porcentaje(valor) {
  const numero = Number(valor) || 0
  return `${numero.toFixed(1).replace('.', ',')} %`
}

export function capitalizar(texto = '') {
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

export function iniciales(nombre = '') {
  const partes = nombre.trim().split(/\s+/).filter(Boolean)
  if (!partes.length) return '?'
  if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase()
  return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase()
}

export function minutos(segundos) {
  if (!segundos || segundos < 0) return '—'
  const total = Math.round(segundos / 60)
  if (total < 60) return `${total} min`
  const horas = Math.floor(total / 60)
  const resto = total % 60
  return `${horas} h ${resto} min`
}

export function claseEstado(clave = '') {
  return `estado-${clave.toLowerCase().replaceAll('_', '-')}`
}

export function claseTemporada(clave = '') {
  return `temporada-${clave.toLowerCase()}`
}

export function colorEstado(clave = '') {
  return {
    PREPARADO: '#2e7d32',
    OCUPADO: '#1565c0',
    PENDIENTE_PREPARACION: '#f9a825',
    EN_PREPARACION: '#fbc02d',
    FUERA_DE_SERVICIO: '#c62828',
  }[clave] || '#6b746d'
}

export function colorTemporada(clave = '') {
  return {
    BAJA: '#6b8f71',
    MEDIA: '#c9a227',
    ALTA: '#c0674f',
  }[clave] || '#6b746d'
}

export function rango(desde, hasta, paso = 1) {
  const valores = []
  for (let i = desde; i <= hasta; i += paso) valores.push(i)
  return valores
}
