import React, { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { Icon } from './Icon.jsx'

export function Modal({
  abierto,
  titulo,
  subtitulo,
  children,
  pie,
  onCerrar,
  ancho = 'mediano',
}) {
  useEffect(() => {
    if (!abierto) return undefined
    const manejar = (evento) => {
      if (evento.key === 'Escape') onCerrar?.()
    }
    document.addEventListener('keydown', manejar)
    const anterior = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', manejar)
      document.body.style.overflow = anterior
    }
  }, [abierto, onCerrar])

  if (!abierto) return null

  return createPortal(
    <div
      className="modal-fondo"
      onMouseDown={(evento) => {
        if (evento.target === evento.currentTarget) onCerrar?.()
      }}
    >
      <section
        className={`modal modal-${ancho}`}
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
      >
        <header className="modal-cabecera">
          <div>
            <h2>{titulo}</h2>
            {subtitulo && <p>{subtitulo}</p>}
          </div>
          <button className="boton-icono" onClick={onCerrar} aria-label="Cerrar">
            <Icon name="close" size={20} />
          </button>
        </header>
        <div className="modal-cuerpo">{children}</div>
        {pie && <footer className="modal-pie">{pie}</footer>}
      </section>
    </div>,
    document.body,
  )
}

export function Toast({ mensaje, tipo = 'exito', onCerrar }) {
  useEffect(() => {
    if (!mensaje) return undefined
    const temporizador = setTimeout(() => onCerrar?.(), 5200)
    return () => clearTimeout(temporizador)
  }, [mensaje, onCerrar])

  if (!mensaje) return null

  return createPortal(
    <div className={`toast toast-${tipo}`} role="status">
      <div className="toast-icono">
        <Icon name={tipo === 'error' ? 'alerta' : 'check'} size={18} />
      </div>
      <p>{mensaje}</p>
      <button className="boton-icono" onClick={onCerrar} aria-label="Cerrar aviso">
        <Icon name="close" size={16} />
      </button>
    </div>,
    document.body,
  )
}

export function Cargando({ texto = 'Cargando información…' }) {
  return (
    <div className="cargando">
      <span className="girador" />
      <p>{texto}</p>
    </div>
  )
}

export function Vacio({ titulo = 'Sin información', children, icono = 'info' }) {
  return (
    <div className="vacio">
      <span className="vacio-icono">
        <Icon name={icono} size={26} />
      </span>
      <h3>{titulo}</h3>
      {children && <div className="vacio-texto">{children}</div>}
    </div>
  )
}

export function Badge({ children, tono = 'neutro', punto = false }) {
  return (
    <span className={`badge badge-${tono}`}>
      {punto && <span className="badge-punto" />}
      {children}
    </span>
  )
}

export function Avatar({ nombre, tamano = 'normal' }) {
  const iniciales = (nombre || '?')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map((parte, indice, partes) =>
      indice === 0 || indice === partes.length - 1 ? parte[0] : '',
    )
    .join('')
    .slice(0, 2)
    .toUpperCase()
  return <span className={`avatar avatar-${tamano}`}>{iniciales || '?'}</span>
}

export function Campo({ etiqueta, ayuda, children, className = '' }) {
  return (
    <label className={`campo ${className}`}>
      {etiqueta && <span className="campo-etiqueta">{etiqueta}</span>}
      {children}
      {ayuda && <span className="campo-ayuda">{ayuda}</span>}
    </label>
  )
}

export function CabeceraSeccion({ titulo, descripcion, acciones, eyebrow }) {
  return (
    <header className="cabecera-seccion">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{titulo}</h1>
        {descripcion && <p className="descripcion-seccion">{descripcion}</p>}
      </div>
      {acciones && <div className="acciones-seccion">{acciones}</div>}
    </header>
  )
}

export function Dato({ etiqueta, valor, children }) {
  return (
    <div className="dato">
      <span className="dato-etiqueta">{etiqueta}</span>
      <span className="dato-valor">{children ?? valor ?? '—'}</span>
    </div>
  )
}

export function Barra({ valor, color = 'var(--verde-2)', altura = 8 }) {
  const ancho = Math.max(0, Math.min(100, Number(valor) || 0))
  return (
    <span className="barra" style={{ height: altura }}>
      <span style={{ width: `${ancho}%`, background: color }} />
    </span>
  )
}

export function GraficoBarras({
  titulo,
  descripcion,
  datos = [],
  formato = (valor) => valor,
  color = 'var(--verde-2)',
  horizontal = true,
}) {
  const maximo = Math.max(1, ...datos.map((dato) => Number(dato.valor) || 0))
  const hayDatos = datos.some((dato) => Number(dato.valor) > 0)
  return (
    <article className="tarjeta grafico">
      <header>
        <h3>{titulo}</h3>
        {descripcion && <p>{descripcion}</p>}
      </header>
      {!hayDatos ? (
        <p className="grafico-vacio">Todavía no hay datos para este indicador.</p>
      ) : (
        <div className={horizontal ? 'barras-horizontales' : 'barras-verticales'}>
          {datos.map((dato) => (
            <div className="barra-fila" key={dato.etiqueta}>
              <span className="barra-etiqueta" title={dato.etiqueta}>
                {dato.etiqueta}
              </span>
              <div className="barra-pista">
                <span
                  style={{
                    width: `${(Number(dato.valor) / maximo) * 100}%`,
                    background: dato.color || color,
                  }}
                />
              </div>
              <span className="barra-valor">
                {formato(Number(dato.valor) || 0, dato)}
              </span>
            </div>
          ))}
        </div>
      )}
    </article>
  )
}
