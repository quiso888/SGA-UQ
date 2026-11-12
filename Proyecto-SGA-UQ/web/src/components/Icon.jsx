import React from 'react'

const TRAZOS = {
  tablero: (
    <>
      <rect x="3" y="3" width="7" height="7" rx="1.5" />
      <rect x="14" y="3" width="7" height="7" rx="1.5" />
      <rect x="3" y="14" width="7" height="7" rx="1.5" />
      <rect x="14" y="14" width="7" height="7" rx="1.5" />
    </>
  ),
  reservas: (
    <>
      <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v18H6.5A2.5 2.5 0 0 0 4 23V5.5Z" />
      <path d="M4 18.5A2.5 2.5 0 0 1 6.5 16H20" />
      <path d="M8 7h8M8 11h5" />
    </>
  ),
  calendario: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M8 3v4M16 3v4M3 10h18" />
      <path d="M8 14h2M14 14h2M8 18h2" />
    </>
  ),
  huespedes: (
    <>
      <circle cx="9" cy="8" r="3.2" />
      <path d="M2.8 20a6.2 6.2 0 0 1 12.4 0" />
      <path d="M16 5.5a3 3 0 0 1 0 5.8M17.5 14.4A5.5 5.5 0 0 1 21.5 20" />
    </>
  ),
  preparacion: (
    <>
      <path d="m4 20 6.5-6.5" />
      <path d="m9 11 4 4" />
      <path d="M14.5 4.5 16 3l1.5 1.5L19 3l1.5 1.5L22 6l-1.5 1.5L22 9l-1.5 1.5L19 12l-1.5-1.5L16 12l-1.5-1.5L13 9l1.5-1.5L13 6l1.5-1.5Z" />
    </>
  ),
  mapa: (
    <>
      <path d="m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3V6Z" />
      <path d="M9 3v15M15 6v15" />
    </>
  ),
  estadisticas: (
    <>
      <path d="M4 20V10M10 20V4M16 20v-7M22 20H2" />
    </>
  ),
  canal: (
    <>
      <path d="M4 4h16v12H7l-3 3V4Z" />
      <path d="M8 9h8M8 12h5" />
    </>
  ),
  plus: <path d="M12 5v14M5 12h14" />,
  search: (
    <>
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="m15.5 15.5 4.5 4.5" />
    </>
  ),
  refresh: (
    <>
      <path d="M20 7v5h-5" />
      <path d="M19.2 15.5A8 8 0 1 1 20 12" />
    </>
  ),
  close: <path d="M6 6l12 12M18 6 6 18" />,
  check: <path d="m4.5 12.5 5 5 10-11" />,
  alerta: (
    <>
      <path d="M12 3 2.5 20h19L12 3Z" />
      <path d="M12 9v5M12 17.5v.5" />
    </>
  ),
  info: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 11v5M12 7.5v.5" />
    </>
  ),
  izquierda: <path d="m14.5 5-7 7 7 7" />,
  derecha: <path d="m9.5 5 7 7-7 7" />,
  abajo: <path d="m5 9.5 7 7 7-7" />,
  rotar: (
    <>
      <path d="M20 7v5h-5" />
      <path d="M19.2 15.5A8 8 0 1 1 20 12" />
    </>
  ),
  flecha: <path d="M5 12h14M13 6l6 6-6 6" />,
  cama: (
    <>
      <path d="M3 18v-8M3 15h18v3M21 18v-5a2 2 0 0 0-2-2H9v4" />
      <circle cx="6.5" cy="10.5" r="1.7" />
    </>
  ),
  reloj: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  dinero: (
    <>
      <rect x="2.5" y="6" width="19" height="12" rx="2" />
      <circle cx="12" cy="12" r="2.6" />
      <path d="M6 9h.01M18 15h.01" />
    </>
  ),
  pin: (
    <>
      <path d="M12 21s7-6.2 7-11.5A7 7 0 0 0 5 9.5C5 14.8 12 21 12 21Z" />
      <circle cx="12" cy="9.5" r="2.4" />
    </>
  ),
  ruta: (
    <>
      <circle cx="6" cy="18" r="2.5" />
      <circle cx="18" cy="6" r="2.5" />
      <path d="M8.5 18H15a3.5 3.5 0 0 0 0-7H9a3.5 3.5 0 0 1 0-7h6.5" />
    </>
  ),
  estrella: <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3 6.4 20.2l1.1-6.2L3 9.6l6.2-.9L12 3Z" />,
  editar: (
    <>
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5Z" />
    </>
  ),
  basura: (
    <>
      <path d="M3 6h18M8 6V4h8v2M6 6l1 15h10l1-15" />
      <path d="M10 11v6M14 11v6" />
    </>
  ),
  menus: <path d="M4 7h16M4 12h16M4 17h16" />,
  datos: (
    <>
      <ellipse cx="12" cy="5.5" rx="8" ry="3" />
      <path d="M4 5.5v6c0 1.7 3.6 3 8 3s8-1.3 8-3v-6" />
      <path d="M4 11.5v6c0 1.7 3.6 3 8 3s8-1.3 8-3v-6" />
    </>
  ),
  llave: (
    <>
      <circle cx="8" cy="15" r="4" />
      <path d="m11 12 8-8M16 4l3 3M14 6l3 3" />
    </>
  ),
  salida: (
    <>
      <path d="M14 4h5v16h-5" />
      <path d="M10 8l4 4-4 4M3 12h11" />
    </>
  ),
  entrar: (
    <>
      <path d="M10 4H4v16h6" />
      <path d="m15 8 4 4-4 4M19 12H8" />
    </>
  ),
  chispa: (
    <>
      <path d="M12 2v4M12 18v4M4.9 4.9l2.8 2.8M16.3 16.3l2.8 2.8M2 12h4M18 12h4M4.9 19.1l2.8-2.8M16.3 7.7l2.8-2.8" />
    </>
  ),
}

export function Icon({ name, size = 18, className = '', strokeWidth = 1.8 }) {
  return (
    <svg
      className={`icono ${className}`}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {TRAZOS[name] || TRAZOS.info}
    </svg>
  )
}
