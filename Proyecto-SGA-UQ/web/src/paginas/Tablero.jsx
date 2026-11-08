import React, { useEffect, useMemo, useState } from 'react'
import { api } from '../api.js'
import { Icon } from '../components/Icon.jsx'
import {
  Badge,
  CabeceraSeccion,
  Cargando,
  Campo,
  Modal,
  Vacio,
} from '../components/ui.jsx'
import {
  claseEstado,
  colorEstado,
  fecha,
  fechaLarga,
  moneda,
  hoyISO,
} from '../utils.js'

const FILTROS = [
  { clave: 'TODOS', etiqueta: 'Todos' },
  { clave: 'PREPARADO', etiqueta: 'Preparados' },
  { clave: 'OCUPADO', etiqueta: 'Ocupados' },
  { clave: 'PENDIENTE_PREPARACION', etiqueta: 'Por preparar' },
  { clave: 'FUERA_DE_SERVICIO', etiqueta: 'Fuera de servicio' },
]

const ESTADOS_EDITABLES = [
  { clave: 'PREPARADO', etiqueta: 'Preparado', descripcion: 'Listo para recibir huéspedes.' },
  { clave: 'PENDIENTE_PREPARACION', etiqueta: 'Pendiente de preparación', descripcion: 'Requiere limpieza antes de un nuevo check-in.' },
  { clave: 'EN_PREPARACION', etiqueta: 'En preparación', descripcion: 'El personal está limpiando la unidad.' },
  { clave: 'FUERA_DE_SERVICIO', etiqueta: 'Fuera de servicio', descripcion: 'Retirado temporalmente de la operación.' },
]

export default function Tablero({ version, notificar, irAReserva, irAPreparacion }) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [filtro, setFiltro] = useState('TODOS')
  const [busqueda, setBusqueda] = useState('')
  const [editor, setEditor] = useState(null)

  useEffect(() => {
    let vigente = true
    setError(null)
    api
      .tablero()
      .then((respuesta) => {
        if (vigente) setDatos(respuesta)
      })
      .catch((problema) => {
        if (vigente) setError(problema.message)
      })
    return () => {
      vigente = false
    }
  }, [version])

  const apartamentos = datos?.apartamentos || []
  const conteo = datos?.conteoEstados || {}

  const filtrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return apartamentos.filter((apartamento) => {
      const coincideFiltro =
        filtro === 'TODOS' ||
        apartamento.estado.clave === filtro ||
        (filtro === 'PENDIENTE_PREPARACION' &&
          apartamento.estado.clave === 'EN_PREPARACION')
      const coincideTexto =
        !texto ||
        apartamento.codigo.toLowerCase().includes(texto) ||
        apartamento.nombre.toLowerCase().includes(texto) ||
        apartamento.bloque.toLowerCase().includes(texto)
      return coincideFiltro && coincideTexto
    })
  }, [apartamentos, filtro, busqueda])

  const porPreparar =
    (conteo.PENDIENTE_PREPARACION || 0) + (conteo.EN_PREPARACION || 0)

  if (error) {
    return (
      <Vacio titulo="No se pudo cargar el tablero" icono="alerta">
        <p>{error}</p>
        <button className="boton secundario" onClick={() => window.location.reload()}>
          Reintentar
        </button>
      </Vacio>
    )
  }

  if (!datos) {
    return <Cargando texto="Consultando el estado de los apartamentos…" />
  }

  return (
    <div className="pagina tablero">
      <CabeceraSeccion
        eyebrow="Operación en vivo"
        titulo="Tablero de apartamentos"
        descripcion="Aparta-Suites UQ · estado operativo de cada unidad, ocupación de hoy y próximas llegadas."
        acciones={
          <button className="boton secundario" onClick={irAPreparacion}>
            <Icon name="preparacion" size={16} />
            Ir a preparación
          </button>
        }
      />

      <section className="franja-hoy">
        <div className="franja-texto">
          <span className="franja-etiqueta">
            <Icon name="calendario" size={15} />
            {fechaLarga(datos.hoy.fecha)}
          </span>
          <h2>
            {conteo.OCUPADO || 0} de {apartamentos.length} unidades ocupadas
            {porPreparar > 0 && ` · ${porPreparar} por preparar`}
          </h2>
          <p>
            Temporada {datos.hoy.temporada.descripcion.toLowerCase()} ·{' '}
            {datos.turno
              ? `recepción a cargo de ${datos.turno.nombre}`
              : 'sin recepcionista de turno'}
          </p>
        </div>
        <div className="franja-indicadores">
          <Indicador etiqueta="Preparados" valor={conteo.PREPARADO || 0} tono="verde" />
          <Indicador etiqueta="Ocupados" valor={conteo.OCUPADO || 0} tono="azul" />
          <Indicador etiqueta="Por preparar" valor={porPreparar} tono="amarillo" />
          <Indicador
            etiqueta="Fuera de servicio"
            valor={conteo.FUERA_DE_SERVICIO || 0}
            tono="rojo"
          />
        </div>
      </section>

      <div className="barra-herramientas">
        <div className="filtros-chips">
          {FILTROS.map((opcion) => {
            const cantidad =
              opcion.clave === 'TODOS'
                ? apartamentos.length
                : opcion.clave === 'PENDIENTE_PREPARACION'
                  ? porPreparar
                  : conteo[opcion.clave] || 0
            return (
              <button
                key={opcion.clave}
                className={`chip ${filtro === opcion.clave ? 'activo' : ''}`}
                onClick={() => setFiltro(opcion.clave)}
              >
                {opcion.etiqueta}
                <span>{cantidad}</span>
              </button>
            )
          })}
        </div>
        <label className="buscador">
          <Icon name="search" size={16} />
          <input
            value={busqueda}
            onChange={(evento) => setBusqueda(evento.target.value)}
            placeholder="Buscar código, suite o bloque…"
            aria-label="Buscar apartamento"
          />
        </label>
      </div>

      {filtrados.length === 0 ? (
        <Vacio titulo="Ningún apartamento coincide" icono="cama">
          <p>Pruebe con otro estado o limpíe la búsqueda.</p>
        </Vacio>
      ) : (
        <div className="rejilla-apartamentos">
          {filtrados.map((apartamento) => (
            <TarjetaApartamento
              key={apartamento.codigo}
              apartamento={apartamento}
              temporada={datos.hoy.temporada.clave}
              alAbrirReserva={irAReserva}
              alEditar={() => setEditor(apartamento)}
            />
          ))}
        </div>
      )}

      <footer className="leyenda-tablero">
        <span className="leyenda-titulo">Código de colores</span>
        {(datos.leyenda || []).map((estado) => (
          <span className="leyenda-item" key={estado.clave}>
            <i style={{ background: estado.color }} />
            {estado.descripcion}
          </span>
        ))}
        <span className="leyenda-item">
          <i style={{ background: colorEstado('EN_PREPARACION') }} />
          En preparación
        </span>
      </footer>

      <ModalEstado
        apartamento={editor}
        onCerrar={() => setEditor(null)}
        notificar={notificar}
      />
    </div>
  )
}

function Indicador({ etiqueta, valor, tono }) {
  return (
    <div className={`mini-indicador tono-${tono}`}>
      <strong>{valor}</strong>
      <span>{etiqueta}</span>
    </div>
  )
}

function TarjetaApartamento({ apartamento, temporada, alAbrirReserva, alEditar }) {
  const proxima = apartamento.proximaReserva
  const color = apartamento.estado.color || colorEstado(apartamento.estado.clave)
  const tarifa = apartamento.tarifas?.[temporada] ?? apartamento.tarifas?.BAJA

  return (
    <article
      className={`tarjeta-apartamento ${claseEstado(apartamento.estado.clave)}`}
      style={{ '--color-estado': color }}
    >
      <div className="tarjeta-apartamento-color" />
      <header className="tarjeta-apartamento-cabecera">
        <div>
          <span className="codigo-apartamento">{apartamento.codigo}</span>
          <h3>{apartamento.nombre}</h3>
        </div>
        <span
          className="estado-pildora"
          style={{ background: `${color}18`, color }}
        >
          <i style={{ background: color }} />
          {apartamento.estado.descripcion}
        </span>
      </header>

      <div className="tarjeta-apartamento-meta">
        <span>
          <Icon name="pin" size={14} /> Bloque {apartamento.bloque} · piso {apartamento.piso}
        </span>
        <span>
          <Icon name="cama" size={14} /> {apartamento.dormitorios} dorm. · {apartamento.capacidad} personas
        </span>
        <span>
          <Icon name="dinero" size={14} /> {moneda(tarifa)} por persona/noche
        </span>
      </div>

      {apartamento.caracteristicas?.length > 0 && (
        <div className="etiquetas-servicios">
          {apartamento.caracteristicas.slice(0, 4).map((caracteristica) => (
            <span key={caracteristica}>{caracteristica}</span>
          ))}
        </div>
      )}

      <div className="proxima-reserva">
        {proxima ? (
          <button className="proxima-boton" onClick={() => alAbrirReserva(proxima.codigo)}>
            <span className="proxima-cabecera">
              <Badge tono={proxima.estado.clave === 'EN_CURSO' ? 'azul' : 'verde'} punto>
                {proxima.estado.descripcion}
              </Badge>
              <strong>{proxima.codigo}</strong>
            </span>
            <span className="proxima-titular">{proxima.titular}</span>
            <span className="proxima-fechas">
              {fecha(proxima.entrada)} → {fecha(proxima.salida)} · {proxima.noches} noches
            </span>
          </button>
        ) : (
          <div className="proxima-vacia">
            <Icon name="calendario" size={16} />
            Sin reservas próximas
          </div>
        )}
      </div>

      {apartamento.estado.clave === 'FUERA_DE_SERVICIO' && apartamento.motivo && (
        <p className="motivo-fuera">
          <Icon name="alerta" size={14} /> {apartamento.motivo}
        </p>
      )}

      <footer className="tarjeta-apartamento-pie">
        <span>{apartamento.reservasRegistradas} reserva(s) registradas</span>
        <button className="enlace" onClick={alEditar}>
          <Icon name="editar" size={14} />
          Estado
        </button>
      </footer>
    </article>
  )
}

function ModalEstado({ apartamento, onCerrar, notificar }) {
  const [estado, setEstado] = useState('')
  const [motivo, setMotivo] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (apartamento) {
      setEstado(
        apartamento.estado.clave === 'OCUPADO'
          ? 'PREPARADO'
          : apartamento.estado.clave,
      )
      setMotivo(apartamento.motivo || '')
    }
  }, [apartamento])

  if (!apartamento) return null

  const ocupado = apartamento.estado.clave === 'OCUPADO'

  const guardar = async () => {
    if (!estado) return
    if (estado === 'FUERA_DE_SERVICIO' && !motivo.trim()) {
      notificar('Indique el motivo para retirar el apartamento de servicio.', 'error')
      return
    }
    setEnviando(true)
    try {
      const respuesta = await api.cambiarEstadoApartamento(
        apartamento.codigo,
        estado,
        motivo,
      )
      notificar(respuesta.mensaje)
      onCerrar()
    } catch (error) {
      notificar(error.message, 'error')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal
      abierto={Boolean(apartamento)}
      titulo={`${apartamento.codigo} · ${apartamento.nombre}`}
      subtitulo="Cambio de estado operativo"
      onCerrar={onCerrar}
      ancho="pequeno"
      pie={
        <>
          <button className="boton fantasma" onClick={onCerrar}>Cancelar</button>
          <button className="boton primario" onClick={guardar} disabled={enviando || ocupado}>
            {enviando ? 'Guardando…' : 'Guardar estado'}
          </button>
        </>
      }
    >
      <div className="estado-actual">
        <span>Estado actual</span>
        <Badge tono="neutro" punto>{apartamento.estado.descripcion}</Badge>
      </div>

      {ocupado ? (
        <div className="aviso info">
          <Icon name="info" size={17} />
          <p>
            El apartamento está ocupado. El cambio a disponible se hace al registrar el
            check-out de la reserva en curso.
          </p>
        </div>
      ) : (
        <>
          <Campo etiqueta="Nuevo estado">
            <select value={estado} onChange={(evento) => setEstado(evento.target.value)}>
              {ESTADOS_EDITABLES.map((opcion) => (
                <option key={opcion.clave} value={opcion.clave}>
                  {opcion.etiqueta}
                </option>
              ))}
            </select>
          </Campo>
          <p className="ayuda-estado">
            {ESTADOS_EDITABLES.find((opcion) => opcion.clave === estado)?.descripcion}
          </p>

          {estado === 'FUERA_DE_SERVICIO' && (
            <Campo etiqueta="Motivo" ayuda="Obligatorio para retirar la unidad de servicio.">
              <textarea
                rows={3}
                value={motivo}
                onChange={(evento) => setMotivo(evento.target.value)}
                placeholder="Mantenimiento, avería, reserva de propietario…"
              />
            </Campo>
          )}
        </>
      )}
    </Modal>
  )
}
