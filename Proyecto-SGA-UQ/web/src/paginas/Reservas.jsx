import React, { useEffect, useMemo, useRef, useState } from 'react'
import { api } from '../api.js'
import { Icon } from '../components/Icon.jsx'
import {
  Avatar,
  Badge,
  CabeceraSeccion,
  Cargando,
  Campo,
  Dato,
  Modal,
  Vacio,
} from '../components/ui.jsx'
import {
  diferenciaDias,
  fecha,
  fechaHora,
  hoyISO,
  moneda,
  sumarDias,
} from '../utils.js'

const TONOS_ESTADO = {
  PENDIENTE: 'amarillo',
  CONFIRMADA: 'verde',
  EN_CURSO: 'azul',
  FINALIZADA: 'neutro',
  CANCELADA: 'rojo',
  NO_SHOW: 'rojo',
}

const FILTROS = [
  { clave: 'TODAS', etiqueta: 'Todas' },
  { clave: 'PENDIENTE', etiqueta: 'Pendientes' },
  { clave: 'CONFIRMADA', etiqueta: 'Confirmadas' },
  { clave: 'EN_CURSO', etiqueta: 'En curso' },
  { clave: 'FINALIZADA', etiqueta: 'Finalizadas' },
  { clave: 'CANCELADA', etiqueta: 'Canceladas' },
]

function normalizar(texto = '') {
  return texto.toLowerCase().replace(/[^a-z0-9]/g, '')
}

function EstadoReservaBadge({ estado, punto = true }) {
  return (
    <Badge tono={TONOS_ESTADO[estado.clave] || 'neutro'} punto={punto}>
      {estado.descripcion}
    </Badge>
  )
}

export default function Reservas({
  version,
  notificar,
  reservaEnfocada,
  onEnfocar,
  abrirNueva,
  onNuevaAbierta,
}) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [codigo, setCodigo] = useState(null)
  const [busqueda, setBusqueda] = useState('')
  const [filtro, setFiltro] = useState('TODAS')
  const [modalNueva, setModalNueva] = useState(false)
  const [modalGrupo, setModalGrupo] = useState(false)
  const [accion, setAccion] = useState(null)
  const [huespedes, setHuespedes] = useState([])

  useEffect(() => {
    let vigente = true
    setError(null)
    api
      .reservas()
      .then((respuesta) => {
        if (!vigente) return
        setDatos(respuesta)
        setCodigo((actual) => {
          if (reservaEnfocada) return reservaEnfocada
          if (actual && respuesta.reservas.some((r) => r.codigo === actual)) {
            return actual
          }
          return respuesta.reservas[0]?.codigo || null
        })
      })
      .catch((problema) => {
        if (vigente) setError(problema.message)
      })
    api
      .huespedes()
      .then((respuesta) => {
        if (vigente) setHuespedes(respuesta.huespedes)
      })
      .catch(() => {})
    return () => {
      vigente = false
    }
  }, [version, reservaEnfocada])

  useEffect(() => {
    if (abrirNueva) {
      setModalNueva(true)
      onNuevaAbierta?.()
    }
  }, [abrirNueva, onNuevaAbierta])

  const reservas = datos?.reservas || []
  const reserva = reservas.find((item) => item.codigo === codigo) || null

  const filtradas = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return reservas
      .filter((item) => filtro === 'TODAS' || item.estado.clave === filtro)
      .filter(
        (item) =>
          !texto ||
          item.codigo.toLowerCase().includes(texto) ||
          item.titular.nombre.toLowerCase().includes(texto) ||
          item.titular.documento.toLowerCase().includes(texto) ||
          item.apartamento.codigo.toLowerCase().includes(texto),
      )
      .sort((a, b) => {
        const orden = { EN_CURSO: 0, CONFIRMADA: 1, PENDIENTE: 2 }
        const diferencia =
          (orden[a.estado.clave] ?? 3) - (orden[b.estado.clave] ?? 3)
        if (diferencia !== 0) return diferencia
        return b.entrada.localeCompare(a.entrada)
      })
  }, [reservas, filtro, busqueda])

  if (error) {
    return (
      <Vacio titulo="No se pudieron cargar las reservas" icono="alerta">
        <p>{error}</p>
        <button className="boton secundario" onClick={() => window.location.reload()}>
          Reintentar
        </button>
      </Vacio>
    )
  }

  if (!datos) {
    return <Cargando texto="Cargando reservas y folios…" />
  }

  const ejecutarAccion = async (datosAccion) => {
    if (!reserva) return
    try {
      const respuesta = await api.accionReserva(reserva.codigo, accion.tipo, datosAccion)
      notificar(respuesta.mensaje)
      setAccion(null)
      onEnfocar?.(reserva.codigo)
    } catch (problema) {
      notificar(problema.message, 'error')
    }
  }

  const procesarVencimientos = async () => {
    try {
      const respuesta = await api.procesarVencimientos()
      notificar(respuesta.mensaje)
    } catch (problema) {
      notificar(problema.message, 'error')
    }
  }

  return (
    <div className="pagina reservas">
      <CabeceraSeccion
        eyebrow="Ciclo de la reserva"
        titulo="Reservas y folios"
        descripcion="Desde la solicitud hasta el check-out: anticipos, ocupantes, movimientos y contra-movimientos."
        acciones={
          <>
            <button className="boton fantasma" onClick={() => setModalGrupo(true)}>
              <Icon name="huespedes" size={16} />
              Asignar grupo
            </button>
            <button className="boton fantasma" onClick={procesarVencimientos}>
              <Icon name="reloj" size={16} />
              Procesar vencimientos
            </button>
            <button className="boton primario" onClick={() => setModalNueva(true)}>
              <Icon name="plus" size={16} />
              Nueva reserva
            </button>
          </>
        }
      />

      <div className="indicadores-reservas">
        {(datos.estados || []).map((estado) => (
          <button
            key={estado.clave}
            className={`indicador-reserva ${filtro === estado.clave ? 'activo' : ''}`}
            onClick={() =>
              setFiltro((actual) => (actual === estado.clave ? 'TODAS' : estado.clave))
            }
          >
            <strong>{estado.cantidad}</strong>
            <span>{estado.descripcion}</span>
          </button>
        ))}
      </div>

      <div className="disposicion-reservas">
        <section className="panel lista-reservas">
          <div className="panel-cabecera">
            <label className="buscador compacto">
              <Icon name="search" size={16} />
              <input
                value={busqueda}
                onChange={(evento) => setBusqueda(evento.target.value)}
                placeholder="Buscar por folio, huésped o suite…"
              />
            </label>
            <select
              className="selector-compacto"
              value={filtro}
              onChange={(evento) => setFiltro(evento.target.value)}
              aria-label="Filtrar reservas"
            >
              {FILTROS.map((opcion) => (
                <option key={opcion.clave} value={opcion.clave}>
                  {opcion.etiqueta}
                </option>
              ))}
            </select>
          </div>

          <div className="lista-reservas-items">
            {filtradas.length === 0 ? (
              <Vacio titulo="Sin reservas para este filtro" icono="reservas" />
            ) : (
              filtradas.map((item) => (
                <button
                  key={item.codigo}
                  className={`fila-reserva ${codigo === item.codigo ? 'activa' : ''}`}
                  onClick={() => {
                    setCodigo(item.codigo)
                    onEnfocar?.(item.codigo)
                  }}
                >
                  <span className="fila-reserva-principal">
                    <span>
                      <strong>{item.titular.nombre}</strong>
                      <small>{item.codigo} · {item.apartamento.codigo}</small>
                    </span>
                    <EstadoReservaBadge estado={item.estado} punto={false} />
                  </span>
                  <span className="fila-reserva-detalle">
                    <span>
                      {fecha(item.entrada)} → {fecha(item.salida)}
                    </span>
                    <strong>{moneda(item.total)}</strong>
                  </span>
                </button>
              ))
            )}
          </div>
          <footer className="panel-pie">
            <span>{filtradas.length} de {reservas.length} reservas</span>
            <span>{datos.totales?.huespedes || 0} huéspedes registrados</span>
          </footer>
        </section>

        <section className="detalle-reserva">
          {!reserva ? (
            <div className="panel">
              <Vacio titulo="Seleccione una reserva" icono="reservas">
                <p>Elija una reserva de la lista para ver su folio y acciones disponibles.</p>
              </Vacio>
            </div>
          ) : (
            <DetalleReserva
              reserva={reserva}
              onAccion={setAccion}
              onEnfocar={onEnfocar}
            />
          )}
        </section>
      </div>

      <ModalNuevaReserva
        abierto={modalNueva}
        onCerrar={() => setModalNueva(false)}
        huespedes={huespedes}
        notificar={notificar}
        onCreada={(mensaje, codigoNuevo) => {
          setModalNueva(false)
          setCodigo(codigoNuevo)
          onEnfocar?.(codigoNuevo)
          notificar(mensaje)
        }}
      />

      <ModalGrupo
        abierto={modalGrupo}
        onCerrar={() => setModalGrupo(false)}
        notificar={notificar}
      />

      <ModalAccion
        accion={accion}
        reserva={reserva}
        onCerrar={() => setAccion(null)}
        onEjecutar={ejecutarAccion}
        notificar={notificar}
      />
    </div>
  )
}

function DetalleReserva({ reserva, onAccion, onEnfocar }) {
  const [pestana, setPestana] = useState('resumen')

  useEffect(() => {
    setPestana('resumen')
  }, [reserva.codigo])

  const anticipoPendiente = Math.max(
    0,
    (reserva.anticipoRequerido || 0) - (reserva.anticipoPagado || 0),
  )
  const puedeCancelarConDevolucion =
    reserva.acciones.cancelar &&
    reserva.anticipoPagado > 0 &&
    new Date(`${reserva.entrada}T00:00:00`) > new Date()

  return (
    <div className="panel detalle-reserva-panel">
      <header className="detalle-cabecera">
        <div className="detalle-titulo">
          <span className="detalle-folio">{reserva.codigo}</span>
          <h2>{reserva.titular.nombre}</h2>
          <p>
            {reserva.apartamento.codigo} · {reserva.apartamento.nombre} ·{' '}
            {reserva.canal.descripcion}
          </p>
        </div>
        <div className="detalle-estado">
          <EstadoReservaBadge estado={reserva.estado} />
          <span className="detalle-total">{moneda(reserva.total)}</span>
          <span className="detalle-total-etiqueta">total estimado</span>
        </div>
      </header>

      <div className="acciones-reserva">
        {reserva.acciones.anticipo && (
          <button
            className="boton primario compacto"
            onClick={() =>
              onAccion({
                tipo: 'anticipo',
                titulo: 'Registrar anticipo',
                descripcion: `Anticipo requerido: ${moneda(reserva.anticipoRequerido)}. Pendiente: ${moneda(anticipoPendiente)}.`,
                monto: anticipoPendiente || reserva.anticipoRequerido,
              })
            }
          >
            <Icon name="dinero" size={15} />
            Anticipo
          </button>
        )}
        {reserva.acciones.checkIn && (
          <button
            className="boton primario compacto"
            onClick={() =>
              onAccion({
                tipo: 'checkin',
                titulo: 'Registrar check-in',
                descripcion: `La llegada de ${reserva.titular.nombre} quedará registrada y el apartamento pasará a ocupado.`,
              })
            }
          >
            <Icon name="entrar" size={15} />
            Check-in
          </button>
        )}
        {reserva.acciones.checkOut && (
          <button
            className="boton primario compacto"
            onClick={() =>
              onAccion({
                tipo: 'checkout',
                titulo: 'Registrar check-out',
                descripcion: 'El apartamento quedará pendiente de preparación en la cola de limpieza.',
              })
            }
          >
            <Icon name="salida" size={15} />
            Check-out
          </button>
        )}
        {reserva.acciones.movimientos && (
          <button
            className="boton secundario compacto"
            onClick={() =>
              onAccion({
                tipo: 'movimientos',
                titulo: 'Movimiento de folio',
                descripcion: 'Registre un cargo, un pago o un abono sobre esta reserva.',
              })
            }
          >
            <Icon name="dinero" size={15} />
            Movimiento
          </button>
        )}
        {reserva.acciones.deshacer && (
          <button
            className="boton secundario compacto"
            onClick={() =>
              onAccion({
                tipo: 'deshacer',
                titulo: 'Deshacer última operación',
                descripcion: 'Se registrará el contra-movimiento de la última operación del folio.',
              })
            }
          >
            <Icon name="rotar" size={15} />
            Deshacer
          </button>
        )}
        {reserva.acciones.noShow && (
          <button
            className="boton fantasma compacto peligro"
            onClick={() =>
              onAccion({
                tipo: 'noshow',
                titulo: 'Registrar no-show',
                descripcion: 'El huésped no se presentó. La reserva se cierra y el apartamento se libera.',
                motivo: '',
              })
            }
          >
            No-show
          </button>
        )}
        {reserva.acciones.cancelar && (
          <button
            className="boton fantasma compacto peligro"
            onClick={() =>
              onAccion({
                tipo: 'cancelar',
                titulo: 'Cancelar reserva',
                descripcion: puedeCancelarConDevolucion
                  ? `La cancelación es anterior a la entrada: devolución estimada de ${moneda(reserva.anticipoPagado)}.`
                  : 'La cancelación es el mismo día o posterior a la entrada: no aplica devolución del anticipo.',
                motivo: '',
              })
            }
          >
            Cancelar
          </button>
        )}
      </div>

      <div className="pestanas-detalle">
        {[
          ['resumen', 'Resumen'],
          ['ocupantes', `Ocupantes (${reserva.ocupantes.length})`],
          ['folio', 'Folio'],
          ['liquidacion', 'Liquidación'],
        ].map(([clave, etiqueta]) => (
          <button
            key={clave}
            className={pestana === clave ? 'activa' : ''}
            onClick={() => setPestana(clave)}
          >
            {etiqueta}
          </button>
        ))}
      </div>

      <div className="detalle-contenido">
        {pestana === 'resumen' && (
          <>
            <div className="rejilla-datos">
              <Dato etiqueta="Documento">{reserva.titular.documento}</Dato>
              <Dato etiqueta="Teléfono">{reserva.titular.telefono || '—'}</Dato>
              <Dato etiqueta="Correo">{reserva.titular.correo || '—'}</Dato>
              <Dato etiqueta="Canal">{reserva.canal.descripcion}</Dato>
              <Dato etiqueta="Entrada">{fecha(reserva.entrada)} · {reserva.hora}</Dato>
              <Dato etiqueta="Salida">{fecha(reserva.salida)}</Dato>
              <Dato etiqueta="Noches">{reserva.noches}</Dato>
              <Dato etiqueta="Personas">{reserva.personas}</Dato>
              <Dato etiqueta="Creada">{fechaHora(reserva.fechaCreacion)}</Dato>
              <Dato etiqueta="Vence">
                {reserva.estado.clave === 'PENDIENTE'
                  ? fechaHora(reserva.fechaVencimiento)
                  : '—'}
              </Dato>
            </div>

            {reserva.motivoCierre && (
              <div className="aviso info">
                <Icon name="info" size={17} />
                <p>
                  <strong>Motivo de cierre:</strong> {reserva.motivoCierre}
                </p>
              </div>
            )}

            <div className="rejilla-totales">
              <div>
                <span>Total estancia</span>
                <strong>{moneda(reserva.total)}</strong>
              </div>
              <div>
                <span>Anticipo pagado</span>
                <strong>{moneda(reserva.anticipoPagado)}</strong>
              </div>
              <div>
                <span>Saldo del folio</span>
                <strong className={reserva.folio.saldo > 0 ? 'texto-peligro' : 'texto-exito'}>
                  {moneda(reserva.folio.saldo)}
                </strong>
              </div>
              <div>
                <span>Anticipo requerido</span>
                <strong>{moneda(reserva.anticipoRequerido)}</strong>
              </div>
            </div>

            <button className="enlace" onClick={() => onEnfocar(reserva.codigo)}>
              Folio {reserva.folio.codigo} · {reserva.folio.movimientos.length} movimiento(s)
              <Icon name="flecha" size={14} />
            </button>
          </>
        )}

        {pestana === 'ocupantes' && (
          <div className="tabla-envoltura">
            <table className="tabla">
              <thead>
                <tr>
                  <th>Huésped</th>
                  <th>Nacimiento</th>
                  <th>Edad</th>
                  <th>Tarifa</th>
                  <th>Titular</th>
                </tr>
              </thead>
              <tbody>
                {reserva.ocupantes.map((ocupante, indice) => (
                  <tr key={`${ocupante.nombre}-${indice}`}>
                    <td>{ocupante.nombre}</td>
                    <td>{fecha(ocupante.fechaNacimiento)}</td>
                    <td>{ocupante.edad} años</td>
                    <td>
                      <Badge tono={ocupante.pagaHoy ? 'verde' : 'neutro'}>
                        {ocupante.pagaHoy ? 'Paga' : 'No paga'}
                      </Badge>
                    </td>
                    <td>{ocupante.titular ? 'Sí' : 'No'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {pestana === 'folio' && (
          <>
            <div className="folio-resumen">
              <div>
                <span>Cargos</span>
                <strong>{moneda(reserva.folio.totalCargos)}</strong>
              </div>
              <div>
                <span>Pagos</span>
                <strong>{moneda(reserva.folio.totalPagos)}</strong>
              </div>
              <div>
                <span>Saldo</span>
                <strong>{moneda(reserva.folio.saldo)}</strong>
              </div>
              <div>
                <span>Estado</span>
                <Badge tono={reserva.folio.saldado ? 'verde' : 'amarillo'}>
                  {reserva.folio.saldado ? 'Saldado' : 'Con saldo'}
                </Badge>
              </div>
            </div>
            <div className="tabla-envoltura">
              <table className="tabla">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Tipo</th>
                    <th>Concepto</th>
                    <th className="alinear-derecha">Efecto</th>
                  </tr>
                </thead>
                <tbody>
                  {reserva.folio.movimientos.map((movimiento, indice) => (
                    <tr key={`${movimiento.fecha}-${indice}`}>
                      <td>{fechaHora(movimiento.fecha)}</td>
                      <td>
                        <Badge
                          tono={
                            movimiento.tipo === 'PAGO' || movimiento.tipo === 'ABONO'
                              ? 'verde'
                              : 'amarillo'
                          }
                        >
                          {movimiento.tipoDescripcion}
                        </Badge>
                      </td>
                      <td>{movimiento.concepto}</td>
                      <td
                        className={`alinear-derecha ${
                          movimiento.efecto < 0 ? 'texto-exito' : ''
                        }`}
                      >
                        {moneda(movimiento.efecto)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        {pestana === 'liquidacion' && (
          <>
            <div className="tabla-envoltura">
              <table className="tabla">
                <thead>
                  <tr>
                    <th>Noche</th>
                    <th>Temporada</th>
                    <th>Personas que pagan</th>
                    <th>Tarifa por persona</th>
                    <th className="alinear-derecha">Valor</th>
                  </tr>
                </thead>
                <tbody>
                  {reserva.liquidacion.noches.map((noche) => (
                    <tr key={noche.fecha}>
                      <td>{fecha(noche.fecha)}</td>
                      <td>
                        <Badge tono={`temporada-${noche.temporada.clave.toLowerCase()}`}>
                          {noche.temporada.descripcion}
                        </Badge>
                      </td>
                      <td>{noche.personas}</td>
                      <td>{moneda(noche.tarifa)}</td>
                      <td className="alinear-derecha">{moneda(noche.valor)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan={4}>Total liquidado</td>
                    <td className="alinear-derecha">{moneda(reserva.liquidacion.total)}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
            <p className="nota-pie">
              La liquidación aplica la temporada vigente a cada noche. Los menores de 3 años
              no pagan; los mayores de 12 pagan tarifa completa.
            </p>
          </>
        )}
      </div>
    </div>
  )
}

function ModalAccion({ accion, reserva, onCerrar, onEjecutar, notificar }) {
  const [monto, setMonto] = useState('')
  const [motivo, setMotivo] = useState('')
  const [tipo, setTipo] = useState('CARGO')
  const [concepto, setConcepto] = useState('')
  const [valor, setValor] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (!accion) return
    setMonto(
      accion.tipo === 'anticipo'
        ? String(Math.round(accion.monto || 0))
        : '',
    )
    setMotivo(accion.motivo || '')
    setTipo('CARGO')
    setConcepto('')
    setValor('')
    setEnviando(false)
  }, [accion])

  if (!accion || !reserva) return null

  const confirmar = async () => {
    let datos = {}
    if (accion.tipo === 'anticipo') {
      const numeroMonto = Number(monto)
      if (!numeroMonto || numeroMonto <= 0) {
        notificar('Ingrese un monto de anticipo mayor que cero.', 'error')
        return
      }
      datos = { monto: numeroMonto }
    } else if (accion.tipo === 'cancelar' || accion.tipo === 'noshow') {
      if (!motivo.trim()) {
        notificar('Indique el motivo para continuar.', 'error')
        return
      }
      datos = { motivo }
    } else if (accion.tipo === 'movimientos') {
      const numeroValor = Number(valor)
      if (!concepto.trim() || !numeroValor || numeroValor <= 0) {
        notificar('Complete el concepto y un valor mayor que cero.', 'error')
        return
      }
      datos = { tipo, concepto, valor: numeroValor }
    }

    setEnviando(true)
    try {
      await onEjecutar(datos)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal
      abierto={Boolean(accion)}
      titulo={accion.titulo}
      subtitulo={`${reserva.codigo} · ${reserva.titular.nombre}`}
      onCerrar={onCerrar}
      ancho="pequeno"
      pie={
        <>
          <button className="boton fantasma" onClick={onCerrar}>
            Cancelar
          </button>
          <button
            className={`boton ${
              accion.tipo === 'cancelar' || accion.tipo === 'noshow'
                ? 'peligro-solido'
                : 'primario'
            }`}
            disabled={enviando}
            onClick={confirmar}
          >
            {enviando ? 'Procesando…' : 'Confirmar'}
          </button>
        </>
      }
    >
      <div className="aviso info">
        <Icon name="info" size={17} />
        <p>{accion.descripcion}</p>
      </div>

      {accion.tipo === 'anticipo' && (
        <Campo
          etiqueta="Monto del anticipo"
          ayuda={`Anticipo requerido: ${moneda(reserva.anticipoRequerido)} · ya pagado: ${moneda(reserva.anticipoPagado)}`}
        >
          <input
            type="number"
            min="1"
            step="1000"
            value={monto}
            onChange={(evento) => setMonto(evento.target.value)}
            autoFocus
          />
        </Campo>
      )}

      {(accion.tipo === 'cancelar' || accion.tipo === 'noshow') && (
        <Campo etiqueta="Motivo">
          <textarea
            rows={3}
            value={motivo}
            onChange={(evento) => setMotivo(evento.target.value)}
            placeholder={
              accion.tipo === 'cancelar'
                ? 'Solicitud del huésped, cambio de planes…'
                : 'El huésped no se presentó antes del límite…'
            }
            autoFocus
          />
        </Campo>
      )}

      {accion.tipo === 'movimientos' && (
        <div className="formulario-apilado">
          <Campo etiqueta="Tipo de movimiento">
            <select value={tipo} onChange={(evento) => setTipo(evento.target.value)}>
              <option value="CARGO">Cargo</option>
              <option value="PAGO">Pago</option>
              <option value="ABONO">Abono</option>
            </select>
          </Campo>
          <Campo etiqueta="Concepto">
            <input
              value={concepto}
              onChange={(evento) => setConcepto(evento.target.value)}
              placeholder="Consumo minibar, pago en efectivo…"
            />
          </Campo>
          <Campo etiqueta="Valor">
            <input
              type="number"
              min="1"
              step="1000"
              value={valor}
              onChange={(evento) => setValor(evento.target.value)}
            />
          </Campo>
        </div>
      )}

      {accion.tipo === 'deshacer' && (
        <p className="nota-pie">
          Esta acción no elimina el movimiento original: crea un contra-movimiento para
          mantener la trazabilidad del folio.
        </p>
      )}
    </Modal>
  )
}

function ModalGrupo({ abierto, onCerrar, notificar }) {
  const [entrada, setEntrada] = useState(hoyISO())
  const [salida, setSalida] = useState(sumarDias(hoyISO(), 2))
  const [personas, setPersonas] = useState(8)
  const [resultado, setResultado] = useState(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (abierto) {
      setResultado(null)
      setEntrada(hoyISO())
      setSalida(sumarDias(hoyISO(), 2))
    }
  }, [abierto])

  const buscar = async () => {
    if (diferenciaDias(entrada, salida) <= 0) {
      notificar('La fecha de salida debe ser posterior a la entrada.', 'error')
      return
    }
    setEnviando(true)
    try {
      const respuesta = await api.asignarGrupo({
        entrada,
        salida,
        personas: Number(personas),
      })
      setResultado(respuesta)
    } catch (error) {
      notificar(error.message, 'error')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal
      abierto={abierto}
      titulo="Asignar grupo"
      subtitulo="Combinación mínima de apartamentos mediante búsqueda con retroceso (backtracking)."
      onCerrar={onCerrar}
      ancho="mediano"
      pie={
        <>
          <button className="boton fantasma" onClick={onCerrar}>
            Cerrar
          </button>
          <button className="boton primario" onClick={buscar} disabled={enviando}>
            {enviando ? 'Buscando…' : 'Buscar combinación'}
          </button>
        </>
      }
    >
      <div className="formulario-fila">
        <Campo etiqueta="Entrada">
          <input
            type="date"
            value={entrada}
            min={hoyISO()}
            onChange={(evento) => setEntrada(evento.target.value)}
          />
        </Campo>
        <Campo etiqueta="Salida">
          <input
            type="date"
            value={salida}
            min={sumarDias(entrada, 1)}
            onChange={(evento) => setSalida(evento.target.value)}
          />
        </Campo>
        <Campo etiqueta="Personas">
          <input
            type="number"
            min="1"
            max="60"
            value={personas}
            onChange={(evento) => setPersonas(evento.target.value)}
          />
        </Campo>
      </div>

      {resultado && (
        <div className="resultado-grupo">
          <div className="aviso info">
            <Icon name="info" size={17} />
            <p>{resultado.mensaje}</p>
          </div>
          <div className="lista-asignados">
            {(resultado.apartamentos || []).map((apartamento) => (
              <div key={apartamento.codigo} className="asignado">
                <strong>{apartamento.codigo}</strong>
                <span>{apartamento.nombre}</span>
                <Badge tono="neutro">{apartamento.capacidad} personas</Badge>
              </div>
            ))}
          </div>
          <p className="nota-pie">
            Capacidad combinada: {resultado.capacidadTotal} personas para{' '}
            {resultado.personas} solicitadas.
          </p>
        </div>
      )}
    </Modal>
  )
}

function ModalNuevaReserva({ abierto, onCerrar, huespedes, notificar, onCreada }) {
  const [entrada, setEntrada] = useState(hoyISO())
  const [salida, setSalida] = useState(sumarDias(hoyISO(), 2))
  const [personas, setPersonas] = useState(2)
  const [canal, setCanal] = useState('DIRECTA')
  const [hora, setHora] = useState('15:00')
  const [documento, setDocumento] = useState('')
  const [nombre, setNombre] = useState('')
  const [nacimiento, setNacimiento] = useState('')
  const [telefono, setTelefono] = useState('')
  const [correo, setCorreo] = useState('')
  const [disponibles, setDisponibles] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [apartamento, setApartamento] = useState(null)
  const [cotizacion, setCotizacion] = useState(null)
  const [acompanantes, setAcompanantes] = useState([])
  const [enviando, setEnviando] = useState(false)
  const consultaRef = useRef(0)

  useEffect(() => {
    if (!abierto) return
    setEntrada(hoyISO())
    setSalida(sumarDias(hoyISO(), 2))
    setPersonas(2)
    setCanal('DIRECTA')
    setHora('15:00')
    setDocumento('')
    setNombre('')
    setNacimiento('')
    setTelefono('')
    setCorreo('')
    setDisponibles(null)
    setApartamento(null)
    setCotizacion(null)
    setAcompanantes([{ nombre: '', fechaNacimiento: '' }])
    setEnviando(false)
  }, [abierto])

  useEffect(() => {
    consultaRef.current += 1
    setDisponibles(null)
    setApartamento(null)
    setCotizacion(null)
    setBuscando(false)
  }, [entrada, salida, personas])

  useEffect(() => {
    const cantidad = Math.max(0, Number(personas) - 1)
    setAcompanantes((actuales) => {
      const copia = actuales.slice(0, cantidad)
      while (copia.length < cantidad) {
        copia.push({ nombre: '', fechaNacimiento: '' })
      }
      return copia
    })
  }, [personas])

  const encontrado = huespedes.find(
    (huesped) => normalizar(huesped.documento) === normalizar(documento),
  )

  useEffect(() => {
    if (encontrado) {
      setNombre(encontrado.nombre)
      setNacimiento(encontrado.fechaNacimiento || '')
      setTelefono(encontrado.telefono || '')
      setCorreo(encontrado.correo || '')
    }
  }, [encontrado?.documento])

  useEffect(() => {
    if (!apartamento) {
      setCotizacion(null)
      return undefined
    }
    let vigente = true
    api
      .cotizacion({
        apartamento: apartamento.codigo,
        entrada,
        salida,
        personas,
      })
      .then((respuesta) => {
        if (vigente) setCotizacion(respuesta)
      })
      .catch(() => {
        if (vigente) setCotizacion(null)
      })
    return () => {
      vigente = false
    }
  }, [apartamento, entrada, salida, personas])

  const buscar = async () => {
    if (diferenciaDias(entrada, salida) <= 0) {
      notificar('La fecha de salida debe ser posterior a la entrada.', 'error')
      return
    }
    const consulta = ++consultaRef.current
    setBuscando(true)
    setApartamento(null)
    setCotizacion(null)
    try {
      const respuesta = await api.disponibilidad({
        entrada,
        salida,
        personas,
        orden: 'precio',
      })
      if (consulta === consultaRef.current) {
        setDisponibles(respuesta)
      }
    } catch (error) {
      if (consulta === consultaRef.current) {
        notificar(error.message, 'error')
      }
    } finally {
      if (consulta === consultaRef.current) {
        setBuscando(false)
      }
    }
  }

  const crear = async () => {
    if (!apartamento) {
      notificar('Seleccione un apartamento disponible.', 'error')
      return
    }
    if (!documento.trim() || !nombre.trim()) {
      notificar('Complete el documento y el nombre del titular.', 'error')
      return
    }
    if (!nacimiento) {
      notificar('Indique la fecha de nacimiento del titular.', 'error')
      return
    }
    const sinNacimiento = acompanantes.find(
      (persona) => persona.nombre.trim() && !persona.fechaNacimiento,
    )
    if (sinNacimiento) {
      notificar(
        `Indique la fecha de nacimiento de ${sinNacimiento.nombre.trim()}.`,
        'error',
      )
      return
    }
    const ocupantes = [
      {
        nombre: encontrado ? encontrado.nombre : nombre.trim(),
        fechaNacimiento: nacimiento || null,
        titular: true,
      },
      ...acompanantes
        .filter((persona) => persona.nombre.trim())
        .map((persona) => ({
          nombre: persona.nombre.trim(),
          fechaNacimiento: persona.fechaNacimiento || null,
          titular: false,
        })),
    ]
    setEnviando(true)
    try {
      const respuesta = await api.crearReserva({
        titularDocumento: documento.trim(),
        titularNombre: nombre.trim(),
        titularFechaNacimiento: nacimiento || null,
        titularTelefono: telefono || null,
        titularCorreo: correo || null,
        apartamento: apartamento.codigo,
        entrada,
        salida,
        canal,
        hora,
        ocupantes,
      })
      onCreada(respuesta.mensaje, respuesta.reserva.codigo)
    } catch (error) {
      notificar(error.message, 'error')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal
      abierto={abierto}
      titulo="Nueva reserva"
      subtitulo="Disponibilidad, cotización noche a noche y registro de ocupantes."
      onCerrar={onCerrar}
      ancho="grande"
      pie={
        <>
          <button className="boton fantasma" onClick={onCerrar}>
            Cancelar
          </button>
          <button
            className="boton primario"
            onClick={crear}
            disabled={enviando || !apartamento}
          >
            {enviando ? 'Registrando…' : 'Crear reserva'}
          </button>
        </>
      }
    >
      <div className="bloques-formulario">
        <section className="bloque-formulario">
          <h3>
            <span>1</span> Estancia
          </h3>
          <div className="formulario-fila">
            <Campo etiqueta="Entrada">
              <input
                type="date"
                value={entrada}
                min={hoyISO()}
                onChange={(evento) => setEntrada(evento.target.value)}
              />
            </Campo>
            <Campo etiqueta="Salida">
              <input
                type="date"
                value={salida}
                min={sumarDias(entrada, 1)}
                onChange={(evento) => setSalida(evento.target.value)}
              />
            </Campo>
            <Campo etiqueta="Personas">
              <input
                type="number"
                min="1"
                max="20"
                value={personas}
                onChange={(evento) => setPersonas(Number(evento.target.value) || 1)}
              />
            </Campo>
          </div>
          <div className="formulario-fila">
            <Campo etiqueta="Canal">
              <select value={canal} onChange={(evento) => setCanal(evento.target.value)}>
                <option value="DIRECTA">Recepción directa</option>
                <option value="PORTAL">Portal web</option>
              </select>
            </Campo>
            <Campo etiqueta="Hora estimada de llegada">
              <input
                type="time"
                value={hora}
                onChange={(evento) => setHora(evento.target.value)}
              />
            </Campo>
            <div className="campo accion-campo">
              <button className="boton secundario" onClick={buscar} disabled={buscando}>
                <Icon name="search" size={16} />
                {buscando ? 'Buscando…' : 'Consultar disponibilidad'}
              </button>
            </div>
          </div>

          {canal === 'PORTAL' && (
            <p className="nota-pie">
              La reserva del portal queda <strong>pendiente</strong> hasta registrar el
              anticipo del 30 %.
            </p>
          )}
        </section>

        {disponibles && (
          <section className="bloque-formulario">
            <h3>
              <span>2</span> Apartamento disponible
            </h3>
            {disponibles.apartamentos.length === 0 ? (
              <div className="aviso advertencia">
                <Icon name="alerta" size={17} />
                <p>
                  No hay apartamentos con capacidad para {personas} personas en esas
                  fechas. Pruebe otras fechas o ajuste la cantidad de huéspedes.
                </p>
              </div>
            ) : (
              <div className="selector-apartamentos">
                {disponibles.apartamentos.map((opcion) => (
                  <button
                    key={opcion.codigo}
                    className={`opcion-apartamento ${
                      apartamento?.codigo === opcion.codigo ? 'activa' : ''
                    }`}
                    onClick={() => setApartamento(opcion)}
                  >
                    <span className="opcion-apartamento-codigo">{opcion.codigo}</span>
                    <strong>{opcion.nombre}</strong>
                    <small>
                      {opcion.capacidad} personas · {opcion.dormitorios} dorm.
                    </small>
                    <span className="opcion-apartamento-precio">
                      {moneda(opcion.totalEstimado)}
                    </span>
                    <small>estimado {disponibles.noches} noches</small>
                  </button>
                ))}
              </div>
            )}

            {cotizacion && (
              <div className="cotizacion">
                <div className="cotizacion-cabecera">
                  <strong>Cotización de {apartamento.codigo}</strong>
                  <span>{moneda(cotizacion.total)}</span>
                </div>
                <div className="cotizacion-noches">
                  {cotizacion.detalle.map((noche) => (
                    <div key={noche.fecha}>
                      <span>{fecha(noche.fecha)}</span>
                      <Badge
                        tono={`temporada-${noche.temporada.clave.toLowerCase()}`}
                      >
                        {noche.temporada.descripcion}
                      </Badge>
                      <span>{noche.personas} pax</span>
                      <strong>{moneda(noche.valor)}</strong>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </section>
        )}

        <section className="bloque-formulario">
          <h3>
            <span>3</span> Huésped titular
          </h3>
          <div className="formulario-fila">
            <Campo etiqueta="Documento" ayuda="Si ya existe, se reutiliza su ficha.">
              <input
                value={documento}
                onChange={(evento) => setDocumento(evento.target.value)}
                placeholder="1.020-304.567"
                list="lista-huespedes"
              />
              <datalist id="lista-huespedes">
                {huespedes.map((huesped) => (
                  <option key={huesped.documento} value={huesped.documento}>
                    {huesped.nombre}
                  </option>
                ))}
              </datalist>
            </Campo>
            <Campo etiqueta="Nombre completo">
              <input
                value={nombre}
                onChange={(evento) => setNombre(evento.target.value)}
                placeholder="Nombre y apellidos"
              />
            </Campo>
            <Campo etiqueta="Fecha de nacimiento">
              <input
                type="date"
                value={nacimiento}
                onChange={(evento) => setNacimiento(evento.target.value)}
              />
            </Campo>
          </div>
          <div className="formulario-fila">
            <Campo etiqueta="Teléfono">
              <input
                value={telefono}
                onChange={(evento) => setTelefono(evento.target.value)}
                placeholder="300 000 0000"
              />
            </Campo>
            <Campo etiqueta="Correo">
              <input
                type="email"
                value={correo}
                onChange={(evento) => setCorreo(evento.target.value)}
                placeholder="huesped@correo.com"
              />
            </Campo>
            <div className="campo estado-huesped">
              {encontrado ? (
                <div className="aviso exito compacto">
                  <Icon name="check" size={16} />
                  <p>Huésped registrado: {encontrado.nombre}</p>
                </div>
              ) : (
                <p className="nota-pie">Se creará una ficha nueva al guardar.</p>
              )}
            </div>
          </div>
        </section>

        {Number(personas) > 0 && (
          <section className="bloque-formulario">
            <h3>
              <span>4</span> Ocupantes adicionales
            </h3>
            <p className="nota-pie">
              El titular ocupa la primera plaza. Registre los acompañantes para calcular
              correctamente la tarifa por edades.
            </p>
            {acompanantes.length === 0 ? (
              <p className="nota-pie">No hay acompañantes adicionales.</p>
            ) : (
              <div className="acompanantes">
                {acompanantes.map((persona, indice) => (
                  <div className="acompanante" key={indice}>
                    <span className="acompanante-numero">{indice + 2}</span>
                    <input
                      value={persona.nombre}
                      onChange={(evento) =>
                        setAcompanantes((actuales) =>
                          actuales.map((item, posicion) =>
                            posicion === indice
                              ? { ...item, nombre: evento.target.value }
                              : item,
                          ),
                        )
                      }
                      placeholder={`Nombre del acompañante ${indice + 1}`}
                    />
                    <input
                      type="date"
                      value={persona.fechaNacimiento}
                      onChange={(evento) =>
                        setAcompanantes((actuales) =>
                          actuales.map((item, posicion) =>
                            posicion === indice
                              ? { ...item, fechaNacimiento: evento.target.value }
                              : item,
                          ),
                        )
                      }
                      aria-label="Fecha de nacimiento"
                    />
                    <button
                      className="boton-icono"
                      onClick={() =>
                        setAcompanantes((actuales) =>
                          actuales.filter((_, posicion) => posicion !== indice),
                        )
                      }
                      aria-label="Quitar acompañante"
                    >
                      <Icon name="close" size={16} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}
      </div>
    </Modal>
  )
}
