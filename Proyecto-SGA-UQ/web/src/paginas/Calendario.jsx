import React, { useEffect, useMemo, useState } from 'react'
import { api } from '../api.js'
import { Icon } from '../components/Icon.jsx'
import { Badge, CabeceraSeccion, Cargando, Vacio } from '../components/ui.jsx'
import {
  colorEstado,
  diaNumero,
  diaSemana,
  fecha,
  hoyISO,
  sumarDias,
} from '../utils.js'

const OPCIONES_DIAS = [14, 30, 45]

export default function Calendario({ version, notificar, irAReserva }) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [inicio, setInicio] = useState(hoyISO())
  const [dias, setDias] = useState(30)

  useEffect(() => {
    let vigente = true
    setError(null)
    api
      .calendario({ inicio, dias })
      .then((respuesta) => {
        if (vigente) setDatos(respuesta)
      })
      .catch((problema) => {
        if (vigente) setError(problema.message)
      })
    return () => {
      vigente = false
    }
  }, [version, inicio, dias])

  const resumen = useMemo(() => {
    if (!datos) return null
    const totalCeldas = datos.apartamentos.length * datos.dias
    let ocupadas = 0
    const reservas = new Set()
    datos.apartamentos.forEach((apartamento) => {
      apartamento.celdas.forEach((celda) => {
        if (celda.reserva) {
          ocupadas++
          reservas.add(celda.reserva.codigo)
        }
      })
    })
    return {
      totalCeldas,
      ocupadas,
      porcentaje: totalCeldas ? (ocupadas / totalCeldas) * 100 : 0,
      reservas: reservas.size,
    }
  }, [datos])

  if (error) {
    return (
      <Vacio titulo="No se pudo cargar el calendario" icono="alerta">
        <p>{error}</p>
      </Vacio>
    )
  }

  if (!datos) {
    return <Cargando texto="Construyendo la ocupación noche a noche…" />
  }

  return (
    <div className="pagina calendario">
      <CabeceraSeccion
        eyebrow="Disponibilidad"
        titulo="Calendario de ocupación"
        descripcion="Cada celda es una noche. Las reservas canceladas y no-show no bloquean inventario."
        acciones={
          <div className="grupo-botones">
            <button
              className="boton secundario compacto"
              onClick={() => setInicio(sumarDias(inicio, -dias))}
              title="Periodo anterior"
            >
              <Icon name="izquierda" size={15} />
            </button>
            <button
              className="boton secundario compacto"
              onClick={() => setInicio(hoyISO())}
            >
              Hoy
            </button>
            <button
              className="boton secundario compacto"
              onClick={() => setInicio(sumarDias(inicio, dias))}
              title="Periodo siguiente"
            >
              <Icon name="derecha" size={15} />
            </button>
          </div>
        }
      />

      <div className="controles-calendario">
        <label className="control-calendario">
          <span>Desde</span>
          <input
            type="date"
            value={inicio}
            onChange={(evento) => setInicio(evento.target.value)}
          />
        </label>
        <label className="control-calendario">
          <span>Días</span>
          <select
            value={dias}
            onChange={(evento) => setDias(Number(evento.target.value))}
          >
            {OPCIONES_DIAS.map((opcion) => (
              <option key={opcion} value={opcion}>
                {opcion} días
              </option>
            ))}
          </select>
        </label>
        <div className="resumen-calendario">
          <div>
            <strong>{resumen.porcentaje.toFixed(1)} %</strong>
            <span>ocupación del periodo</span>
          </div>
          <div>
            <strong>{resumen.ocupadas}</strong>
            <span>noches ocupadas</span>
          </div>
          <div>
            <strong>{resumen.reservas}</strong>
            <span>reservas activas</span>
          </div>
          <div>
            <strong>{datos.apartamentos.length}</strong>
            <span>apartamentos</span>
          </div>
        </div>
      </div>

      <div className="panel calendario-panel">
        <div className="calendario-desplazable">
          <table className="tabla-calendario">
            <thead>
              <tr>
                <th className="columna-apartamento sticky-izquierda">
                  Apartamento
                </th>
                {datos.fechas.map((dia) => (
                  <th
                    key={dia.fecha}
                    className={`columna-dia temporada-${dia.temporada.clave.toLowerCase()} ${
                      dia.fecha === hoyISO() ? 'hoy' : ''
                    }`}
                  >
                    <span>{diaSemana(dia.fecha)}</span>
                    <strong>{diaNumero(dia.fecha)}</strong>
                    <i
                      className="punto-temporada"
                      title={`Temporada ${dia.temporada.descripcion}`}
                    />
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {datos.apartamentos.map((apartamento) => (
                <FilaCalendario
                  key={apartamento.codigo}
                  apartamento={apartamento}
                  irAReserva={irAReserva}
                />
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="leyenda-calendario">
        <span className="leyenda-titulo">Temporadas</span>
        <span className="leyenda-item">
          <i className="temporada-baja" /> Baja
        </span>
        <span className="leyenda-item">
          <i className="temporada-media" /> Media
        </span>
        <span className="leyenda-item">
          <i className="temporada-alta" /> Alta
        </span>
        <span className="leyenda-titulo separado">Estados</span>
        <span className="leyenda-item">
          <i style={{ background: colorEstado('OCUPADO') }} /> En curso
        </span>
        <span className="leyenda-item">
          <i style={{ background: colorEstado('PENDIENTE_PREPARACION') }} /> Confirmada
        </span>
        <span className="leyenda-item">
          <i className="rayado" /> Fuera de servicio
        </span>
      </div>

      <p className="nota-pie">
        Haga clic sobre una noche ocupada para abrir la reserva y gestionar su ciclo.
        Las fechas de salida no se marcan como ocupadas: la noche se libera para una nueva
        llegada.
      </p>
    </div>
  )
}

function FilaCalendario({ apartamento, irAReserva }) {
  const celdas = apartamento.celdas
  const segmentos = useMemo(() => {
    const resultado = {}
    celdas.forEach((celda, indice) => {
      if (!celda.reserva) return
      const codigo = celda.reserva.codigo
      if (!resultado[codigo]) {
        resultado[codigo] = { inicio: indice, fin: indice, reserva: celda.reserva }
      } else {
        resultado[codigo].fin = indice
      }
    })
    return resultado
  }, [celdas])

  return (
    <tr>
      <th className="columna-apartamento sticky-izquierda">
        <button className="apartamento-calendario" onClick={() => {}}>
          <span
            className="punto-estado"
            style={{ background: apartamento.estado.color }}
          />
          <span>
            <strong>{apartamento.codigo}</strong>
            <small>{apartamento.nombre}</small>
          </span>
          <Badge tono="neutro">{apartamento.bloque}</Badge>
        </button>
      </th>
      {celdas.map((celda, indice) => {
        const reserva = celda.reserva
        const anterior =
          indice > 0 && celdas[indice - 1].reserva?.codigo === reserva?.codigo
        const siguiente =
          indice < celdas.length - 1 &&
          celdas[indice + 1].reserva?.codigo === reserva?.codigo
        const esInicio = Boolean(reserva) && !anterior
        const segmento = reserva ? segmentos[reserva.codigo] : null
        const longitud = segmento ? segmento.fin - segmento.inicio + 1 : 1
        const color = reserva
          ? reserva.estado.clave === 'EN_CURSO'
            ? colorEstado('OCUPADO')
            : reserva.estado.clave === 'PENDIENTE'
              ? '#c9a227'
              : colorEstado('PREPARADO')
          : null

        return (
          <td
            key={`${apartamento.codigo}-${celda.fecha}`}
            className={`celda-calendario ${reserva ? 'ocupada' : ''} ${
              esInicio ? 'inicio-reserva' : ''
            } ${reserva && !siguiente ? 'fin-reserva' : ''} ${
              celda.fueraServicio ? 'fuera-servicio' : ''
            } ${celda.fecha === hoyISO() ? 'hoy' : ''}`}
            title={
              reserva
                ? `${reserva.codigo} · ${reserva.titular}\n${fecha(reserva.entrada)} → ${fecha(reserva.salida)} · ${reserva.estado.descripcion}\n${reserva.canal.descripcion}`
                : celda.fueraServicio
                  ? 'Apartamento fuera de servicio'
                  : `Disponible · ${fecha(celda.fecha)}`
            }
            onClick={() => {
              if (reserva) irAReserva(reserva.codigo)
            }}
          >
            {reserva && (
              <span
                className="celda-color"
                style={{ background: `${color}22`, borderColor: color }}
              />
            )}
            {reserva && esInicio && (
              <span
                className="bloque-calendario"
                style={{
                  '--color-reserva': color,
                  width: `calc(${longitud * 100}% - 2px)`,
                }}
              >
                <strong>{reserva.codigo}</strong>
                <span>{reserva.titular.split(' ')[0]}</span>
              </span>
            )}
          </td>
        )
      })}
    </tr>
  )
}
