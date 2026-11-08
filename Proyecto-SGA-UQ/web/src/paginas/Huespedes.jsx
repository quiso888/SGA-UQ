import React, { useEffect, useMemo, useState } from 'react'
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
import { fecha, moneda } from '../utils.js'

const TONOS_ESTADO = {
  PENDIENTE: 'amarillo',
  CONFIRMADA: 'verde',
  EN_CURSO: 'azul',
  FINALIZADA: 'neutro',
  CANCELADA: 'rojo',
  NO_SHOW: 'rojo',
}

function EstadoBadge({ estado }) {
  return (
    <Badge tono={TONOS_ESTADO[estado.clave] || 'neutro'} punto>
      {estado.descripcion}
    </Badge>
  )
}

export default function Huespedes({ version, notificar, irAReserva }) {
  const [datos, setDatos] = useState(null)
  const [apartamentos, setApartamentos] = useState([])
  const [error, setError] = useState(null)
  const [documento, setDocumento] = useState(null)
  const [busqueda, setBusqueda] = useState('')
  const [modalNuevo, setModalNuevo] = useState(false)
  const [modalEditar, setModalEditar] = useState(false)
  const [favorito, setFavorito] = useState('')

  useEffect(() => {
    let vigente = true
    setError(null)
    Promise.all([api.huespedes(), api.temporadas()])
      .then(([respuesta, temporadas]) => {
        if (!vigente) return
        setDatos(respuesta)
        setApartamentos(temporadas.apartamentos || [])
        setDocumento((actual) => {
          if (actual && respuesta.huespedes.some((h) => h.documento === actual)) {
            return actual
          }
          return respuesta.huespedes[0]?.documento || null
        })
      })
      .catch((problema) => {
        if (vigente) setError(problema.message)
      })
    return () => {
      vigente = false
    }
  }, [version])

  const huespedes = datos?.huespedes || []
  const huesped = huespedes.find((item) => item.documento === documento) || null

  const filtrados = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    if (!texto) return huespedes
    return huespedes.filter(
      (item) =>
        item.nombre.toLowerCase().includes(texto) ||
        item.documento.toLowerCase().includes(texto) ||
        (item.correo || '').toLowerCase().includes(texto),
    )
  }, [huespedes, busqueda])

  useEffect(() => {
    setFavorito('')
  }, [documento])

  if (error) {
    return (
      <Vacio titulo="No se pudieron cargar los huéspedes" icono="alerta">
        <p>{error}</p>
      </Vacio>
    )
  }

  if (!datos) {
    return <Cargando texto="Cargando fichas de huéspedes…" />
  }

  const alternarFavorito = async (codigo) => {
    if (!huesped) return
    try {
      const respuesta = await api.alternarFavorito(huesped.documento, codigo)
      notificar(respuesta.mensaje)
    } catch (problema) {
      notificar(problema.message, 'error')
    }
  }

  const agregarFavorito = async () => {
    if (!favorito) return
    await alternarFavorito(favorito)
    setFavorito('')
  }

  const noFavoritos = apartamentos.filter(
    (apartamento) => !huesped?.favoritos.includes(apartamento.codigo),
  )

  return (
    <div className="pagina huespedes">
      <CabeceraSeccion
        eyebrow="Relación con el huésped"
        titulo="Huéspedes"
        descripcion="Fichas, contacto, preferencias e historial completo de estancias."
        acciones={
          <button className="boton primario" onClick={() => setModalNuevo(true)}>
            <Icon name="plus" size={16} />
            Nuevo huésped
          </button>
        }
      />

      <div className="disposicion-huespedes">
        <section className="panel lista-huespedes">
          <div className="panel-cabecera">
            <label className="buscador compacto">
              <Icon name="search" size={16} />
              <input
                value={busqueda}
                onChange={(evento) => setBusqueda(evento.target.value)}
                placeholder="Buscar nombre, documento o correo…"
              />
            </label>
            <span className="contador-lista">{filtrados.length}</span>
          </div>

          <div className="lista-huespedes-items">
            {filtrados.length === 0 ? (
              <Vacio titulo="Sin huéspedes" icono="huespedes">
                <p>Registre el primero con el botón «Nuevo huésped».</p>
              </Vacio>
            ) : (
              filtrados.map((item) => (
                <button
                  key={item.documento}
                  className={`fila-huesped ${
                    documento === item.documento ? 'activa' : ''
                  }`}
                  onClick={() => setDocumento(item.documento)}
                >
                  <Avatar nombre={item.nombre} />
                  <span className="fila-huesped-texto">
                    <strong>{item.nombre}</strong>
                    <small>{item.documento}</small>
                  </span>
                  <span className="fila-huesped-numero">{item.totalReservas}</span>
                </button>
              ))
            )}
          </div>
          <footer className="panel-pie">
            <span>{huespedes.length} huéspedes en total</span>
            <span>{datos.totalSistema} fichas</span>
          </footer>
        </section>

        <section className="detalle-huesped">
          {!huesped ? (
            <div className="panel">
              <Vacio titulo="Seleccione un huésped" icono="huespedes" />
            </div>
          ) : (
            <div className="panel perfil-huesped">
              <header className="perfil-cabecera">
                <Avatar nombre={huesped.nombre} tamano="grande" />
                <div className="perfil-titulo">
                  <h2>{huesped.nombre}</h2>
                  <p>
                    Documento {huesped.documento} · {huesped.totalReservas} reserva(s)
                  </p>
                </div>
                <button
                  className="boton secundario compacto"
                  onClick={() => setModalEditar(true)}
                >
                  <Icon name="editar" size={15} />
                  Editar ficha
                </button>
              </header>

              <div className="rejilla-datos contacto">
                <Dato etiqueta="Teléfono">{huesped.telefono || '—'}</Dato>
                <Dato etiqueta="Correo">{huesped.correo || '—'}</Dato>
                <Dato etiqueta="Nacimiento">{fecha(huesped.fechaNacimiento)}</Dato>
                <Dato etiqueta="Preferencias">
                  {huesped.favoritos.length
                    ? `${huesped.favoritos.length} apartamento(s)`
                    : 'Sin preferencias'}
                </Dato>
              </div>

              <div className="rejilla-totales tres">
                <div>
                  <span>Noches acumuladas</span>
                  <strong>{huesped.totalNoches}</strong>
                </div>
                <div>
                  <span>Valor histórico</span>
                  <strong>{moneda(huesped.totalGastado)}</strong>
                </div>
                <div>
                  <span>Reservas</span>
                  <strong>{huesped.totalReservas}</strong>
                </div>
              </div>

              <section className="bloque-perfil">
                <header className="bloque-perfil-cabecera">
                  <h3>
                    <Icon name="estrella" size={17} />
                    Preferencias
                  </h3>
                </header>
                <div className="favoritos">
                  {huesped.favoritos.length === 0 ? (
                    <p className="nota-pie">
                      Sin apartamentos favoritos. Agregue una preferencia para futuras
                      asignaciones.
                    </p>
                  ) : (
                    huesped.favoritos.map((codigo) => {
                      const info = apartamentos.find((a) => a.codigo === codigo)
                      return (
                        <span className="favorito" key={codigo}>
                          <strong>{codigo}</strong>
                          <span>{info?.nombre || 'Apartamento'}</span>
                          <button
                            className="boton-icono"
                            onClick={() => alternarFavorito(codigo)}
                            aria-label={`Quitar ${codigo} de favoritos`}
                          >
                            <Icon name="close" size={14} />
                          </button>
                        </span>
                      )
                    })
                  )}
                </div>
                {noFavoritos.length > 0 && (
                  <div className="agregar-favorito">
                    <select
                      value={favorito}
                      onChange={(evento) => setFavorito(evento.target.value)}
                    >
                      <option value="">Agregar apartamento favorito…</option>
                      {noFavoritos.map((apartamento) => (
                        <option key={apartamento.codigo} value={apartamento.codigo}>
                          {apartamento.codigo} · {apartamento.nombre}
                        </option>
                      ))}
                    </select>
                    <button
                      className="boton secundario compacto"
                      onClick={agregarFavorito}
                      disabled={!favorito}
                    >
                      <Icon name="plus" size={15} />
                      Agregar
                    </button>
                  </div>
                )}
              </section>

              <section className="bloque-perfil">
                <header className="bloque-perfil-cabecera">
                  <h3>
                    <Icon name="reservas" size={17} />
                    Historial de estancias
                  </h3>
                </header>
                {huesped.historial.length === 0 ? (
                  <Vacio titulo="Sin estancias registradas" icono="reservas" />
                ) : (
                  <div className="tabla-envoltura">
                    <table className="tabla">
                      <thead>
                        <tr>
                          <th>Reserva</th>
                          <th>Apartamento</th>
                          <th>Fechas</th>
                          <th>Estado</th>
                          <th className="alinear-derecha">Total</th>
                        </tr>
                      </thead>
                      <tbody>
                        {huesped.historial.map((reserva) => (
                          <tr
                            key={reserva.codigo}
                            className={irAReserva ? 'fila-clic' : ''}
                            onClick={() => irAReserva?.(reserva.codigo)}
                          >
                            <td>
                              <strong>{reserva.codigo}</strong>
                              <small className="celda-secundaria">
                                {reserva.canal.descripcion}
                              </small>
                            </td>
                            <td>{reserva.apartamento}</td>
                            <td>
                              {fecha(reserva.entrada)} → {fecha(reserva.salida)}
                            </td>
                            <td>
                              <EstadoBadge estado={reserva.estado} />
                            </td>
                            <td className="alinear-derecha">{moneda(reserva.total)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            </div>
          )}
        </section>
      </div>

      <ModalHuesped
        abierto={modalNuevo}
        onCerrar={() => setModalNuevo(false)}
        notificar={notificar}
        onCreado={(nuevoDocumento) => setDocumento(nuevoDocumento)}
      />

      {huesped && (
        <ModalEditarHuesped
          abierto={modalEditar}
          huesped={huesped}
          onCerrar={() => setModalEditar(false)}
          notificar={notificar}
        />
      )}
    </div>
  )
}

function ModalHuesped({ abierto, onCerrar, notificar, onCreado }) {
  const [documento, setDocumento] = useState('')
  const [nombre, setNombre] = useState('')
  const [fechaNacimiento, setFechaNacimiento] = useState('')
  const [telefono, setTelefono] = useState('')
  const [correo, setCorreo] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (!abierto) return
    setDocumento('')
    setNombre('')
    setFechaNacimiento('')
    setTelefono('')
    setCorreo('')
    setEnviando(false)
  }, [abierto])

  const guardar = async () => {
    if (!documento.trim() || !nombre.trim()) {
      notificar('El documento y el nombre son obligatorios.', 'error')
      return
    }
    setEnviando(true)
    try {
      const respuesta = await api.crearHuesped({
        documento: documento.trim(),
        nombre: nombre.trim(),
        fechaNacimiento: fechaNacimiento || null,
        telefono,
        correo,
      })
      onCreado?.(respuesta.huesped.documento)
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
      abierto={abierto}
      titulo="Nuevo huésped"
      subtitulo="La ficha se conserva para futuras reservas e historial."
      onCerrar={onCerrar}
      ancho="pequeno"
      pie={
        <>
          <button className="boton fantasma" onClick={onCerrar}>
            Cancelar
          </button>
          <button className="boton primario" onClick={guardar} disabled={enviando}>
            {enviando ? 'Guardando…' : 'Registrar huésped'}
          </button>
        </>
      }
    >
      <div className="formulario-apilado">
        <Campo etiqueta="Documento">
          <input
            value={documento}
            onChange={(evento) => setDocumento(evento.target.value)}
            placeholder="1.020-304.567"
            autoFocus
          />
        </Campo>
        <Campo etiqueta="Nombre completo">
          <input
            value={nombre}
            onChange={(evento) => setNombre(evento.target.value)}
            placeholder="Ana María Rodríguez"
          />
        </Campo>
        <Campo etiqueta="Fecha de nacimiento">
          <input
            type="date"
            value={fechaNacimiento}
            onChange={(evento) => setFechaNacimiento(evento.target.value)}
          />
        </Campo>
        <div className="formulario-fila">
          <Campo etiqueta="Teléfono">
            <input
              value={telefono}
              onChange={(evento) => setTelefono(evento.target.value)}
            />
          </Campo>
          <Campo etiqueta="Correo">
            <input
              type="email"
              value={correo}
              onChange={(evento) => setCorreo(evento.target.value)}
            />
          </Campo>
        </div>
      </div>
    </Modal>
  )
}

function ModalEditarHuesped({ abierto, huesped, onCerrar, notificar }) {
  const [nombre, setNombre] = useState(huesped.nombre)
  const [fechaNacimiento, setFechaNacimiento] = useState(
    huesped.fechaNacimiento || '',
  )
  const [telefono, setTelefono] = useState(huesped.telefono || '')
  const [correo, setCorreo] = useState(huesped.correo || '')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (!abierto) return
    setNombre(huesped.nombre)
    setFechaNacimiento(huesped.fechaNacimiento || '')
    setTelefono(huesped.telefono || '')
    setCorreo(huesped.correo || '')
    setEnviando(false)
  }, [abierto, huesped])

  const guardar = async () => {
    if (!nombre.trim()) {
      notificar('El nombre es obligatorio.', 'error')
      return
    }
    setEnviando(true)
    try {
      const respuesta = await api.actualizarHuesped(huesped.documento, {
        nombre: nombre.trim(),
        fechaNacimiento: fechaNacimiento || null,
        telefono,
        correo,
      })
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
      abierto={abierto}
      titulo="Editar ficha"
      subtitulo={`${huesped.documento} · ${huesped.nombre}`}
      onCerrar={onCerrar}
      ancho="pequeno"
      pie={
        <>
          <button className="boton fantasma" onClick={onCerrar}>
            Cancelar
          </button>
          <button className="boton primario" onClick={guardar} disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar cambios'}
          </button>
        </>
      }
    >
      <div className="formulario-apilado">
        <Campo etiqueta="Nombre completo">
          <input
            value={nombre}
            onChange={(evento) => setNombre(evento.target.value)}
          />
        </Campo>
        <Campo etiqueta="Fecha de nacimiento">
          <input
            type="date"
            value={fechaNacimiento}
            onChange={(evento) => setFechaNacimiento(evento.target.value)}
          />
        </Campo>
        <div className="formulario-fila">
          <Campo etiqueta="Teléfono">
            <input
              value={telefono}
              onChange={(evento) => setTelefono(evento.target.value)}
            />
          </Campo>
          <Campo etiqueta="Correo">
            <input
              type="email"
              value={correo}
              onChange={(evento) => setCorreo(evento.target.value)}
            />
          </Campo>
        </div>
      </div>
    </Modal>
  )
}
