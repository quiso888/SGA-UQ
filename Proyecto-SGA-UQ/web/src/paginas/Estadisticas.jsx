import React, { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Icon } from '../components/Icon.jsx'
import {
  Badge,
  CabeceraSeccion,
  Cargando,
  Campo,
  GraficoBarras,
  Modal,
  Vacio,
} from '../components/ui.jsx'
import { fecha, hoyISO, moneda, monedaCorta, numero, porcentaje, sumarDias } from '../utils.js'

const COLORES = ['#2f6b54', '#c9a227', '#c0674f', '#4f7cac', '#7b6d8d', '#5b8c7b']

export default function Estadisticas({ version, notificar }) {
  const [datos, setDatos] = useState(null)
  const [temporadas, setTemporadas] = useState(null)
  const [error, setError] = useState(null)
  const [modalTemporada, setModalTemporada] = useState(false)

  useEffect(() => {
    let vigente = true
    setError(null)
    Promise.all([api.estadisticas(), api.temporadas()])
      .then(([estadisticas, calendario]) => {
        if (!vigente) return
        setDatos(estadisticas)
        setTemporadas(calendario)
      })
      .catch((problema) => {
        if (vigente) setError(problema.message)
      })
    return () => {
      vigente = false
    }
  }, [version])

  if (error) {
    return (
      <Vacio titulo="No se pudieron cargar las estadísticas" icono="alerta">
        <p>{error}</p>
      </Vacio>
    )
  }

  if (!datos) {
    return <Cargando texto="Calculando indicadores del alojamiento…" />
  }

  const indicadores = datos.indicadores
  const series = datos.series

  return (
    <div className="pagina estadisticas">
      <CabeceraSeccion
        eyebrow="Inteligencia operativa"
        titulo="Estadísticas del alojamiento"
        descripcion="Ocupación, ingresos, reservas, cancelaciones, conflictos de canal y tiempos de preparación."
        acciones={
          <button className="boton secundario" onClick={() => setModalTemporada(true)}>
            <Icon name="calendario" size={16} />
            Registrar temporada
          </button>
        }
      />

      <div className="rejilla-kpis">
        <Kpi
          icono="dinero"
          etiqueta="Ingresos totales"
          valor={moneda(indicadores.ingresoTotal)}
          detalle={`Promedio por reserva ${moneda(indicadores.ingresoPromedio)}`}
          tono="verde"
        />
        <Kpi
          icono="cama"
          etiqueta="Ocupación de hoy"
          valor={porcentaje(indicadores.ocupacionHoy)}
          detalle={`${indicadores.apartamentosPorPreparar} unidad(es) por preparar`}
          tono="azul"
        />
        <Kpi
          icono="reservas"
          etiqueta="Reservas registradas"
          valor={numero(indicadores.totalReservas)}
          detalle="Incluye todos los estados"
          tono="dorado"
        />
        <Kpi
          icono="close"
          etiqueta="Tasa de cancelación"
          valor={porcentaje(indicadores.tasaCancelacion)}
          detalle={`No-show: ${porcentaje(indicadores.tasaNoShow)}`}
          tono="rojo"
        />
        <Kpi
          icono="canal"
          etiqueta="Conflictos de canal"
          valor={numero(indicadores.conflictosCanal)}
          detalle="Solicitudes externas rechazadas"
          tono="terracota"
        />
        <Kpi
          icono="reloj"
          etiqueta="Preparación promedio"
          valor={`${numero(indicadores.preparacionPromedio)} min`}
          detalle="Tiempo medio de housekeeping"
          tono="neutro"
        />
      </div>

      <div className="rejilla-graficos">
        <GraficoBarras
          titulo="Reservas por estado"
          descripcion="Distribución del ciclo de vida de las reservas."
          datos={series.reservasEstado}
          formato={(valor) => numero(valor)}
          color={COLORES[0]}
        />
        <GraficoBarras
          titulo="Reservas por canal"
          descripcion="Participación de cada origen de la reserva."
          datos={series.reservasCanal}
          formato={(valor) => numero(valor)}
          color={COLORES[1]}
        />
        <GraficoBarras
          titulo="Ingresos por canal"
          descripcion="Valor estimado de las reservas no canceladas."
          datos={series.ingresosCanal}
          formato={(valor) => monedaCorta(valor)}
          color={COLORES[2]}
        />
        <GraficoBarras
          titulo="Ingresos por temporada"
          descripcion="Contribución de cada temporada tarifaria."
          datos={series.ingresosTemporada}
          formato={(valor) => monedaCorta(valor)}
          color={COLORES[3]}
        />
        <GraficoBarras
          titulo="Apartamentos por estado"
          descripcion="Situación operativa actual del inventario."
          datos={series.apartamentosEstado}
          formato={(valor) => numero(valor)}
          color={COLORES[4]}
        />
        <GraficoBarras
          titulo="Noches reservadas por unidad"
          descripcion="Demanda acumulada por apartamento."
          datos={series.nochesPorApartamento}
          formato={(valor) => `${numero(valor)} noches`}
          color={COLORES[5]}
        />
      </div>

      <section className="panel temporadas-panel">
        <header className="panel-cabecera simple">
          <div>
            <h2>Temporadas y tarifas</h2>
            <p>
              Cada apartamento cobra por persona y por noche. La temporada se aplica noche
              a noche al liquidar una reserva.
            </p>
          </div>
          <Badge tono={`temporada-${temporadas?.temporadaHoy?.clave?.toLowerCase() || 'baja'}`}>
            Hoy: {temporadas?.temporadaHoy?.descripcion || '—'}
          </Badge>
        </header>

        <div className="disposicion-temporadas">
          <div>
            <h3>Periodos configurados</h3>
            {!temporadas?.periodos?.length ? (
              <Vacio titulo="Solo temporada baja" icono="calendario">
                <p>Sin periodos especiales: todas las noches usan la tarifa baja.</p>
              </Vacio>
            ) : (
              <div className="lista-periodos">
                {temporadas.periodos.map((periodo, indice) => (
                  <div key={`${periodo.inicio}-${indice}`} className="periodo">
                    <Badge
                      tono={`temporada-${periodo.temporada.clave.toLowerCase()}`}
                    >
                      {periodo.temporada.descripcion}
                    </Badge>
                    <span>
                      {fecha(periodo.inicio)} → {fecha(periodo.fin)}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div>
            <h3>Tarifas por persona / noche</h3>
            <div className="tabla-envoltura tabla-tarifas">
              <table className="tabla">
                <thead>
                  <tr>
                    <th>Apartamento</th>
                    <th className="alinear-derecha">Baja</th>
                    <th className="alinear-derecha">Media</th>
                    <th className="alinear-derecha">Alta</th>
                  </tr>
                </thead>
                <tbody>
                  {(temporadas?.apartamentos || []).map((apartamento) => (
                    <tr key={apartamento.codigo}>
                      <td>
                        <strong>{apartamento.codigo}</strong>
                        <small className="celda-secundaria">
                          {apartamento.nombre}
                        </small>
                      </td>
                      <td className="alinear-derecha">
                        {moneda(apartamento.tarifas.BAJA)}
                      </td>
                      <td className="alinear-derecha">
                        {moneda(apartamento.tarifas.MEDIA)}
                      </td>
                      <td className="alinear-derecha">
                        {moneda(apartamento.tarifas.ALTA)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

      <ModalTemporada
        abierto={modalTemporada}
        onCerrar={() => setModalTemporada(false)}
        notificar={notificar}
      />
    </div>
  )
}

function Kpi({ icono, etiqueta, valor, detalle, tono }) {
  return (
    <article className={`kpi kpi-${tono}`}>
      <span className="kpi-icono">
        <Icon name={icono} size={20} />
      </span>
      <span className="kpi-etiqueta">{etiqueta}</span>
      <strong>{valor}</strong>
      <small>{detalle}</small>
    </article>
  )
}

function ModalTemporada({ abierto, onCerrar, notificar }) {
  const [temporada, setTemporada] = useState('MEDIA')
  const [inicio, setInicio] = useState(sumarDias(hoyISO(), 7))
  const [fin, setFin] = useState(sumarDias(hoyISO(), 14))
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (!abierto) return
    setTemporada('MEDIA')
    setInicio(sumarDias(hoyISO(), 7))
    setFin(sumarDias(hoyISO(), 14))
    setEnviando(false)
  }, [abierto])

  const guardar = async () => {
    if (fin < inicio) {
      notificar('La fecha final debe ser igual o posterior a la inicial.', 'error')
      return
    }
    setEnviando(true)
    try {
      const respuesta = await api.crearTemporada({ temporada, inicio, fin })
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
      titulo="Registrar temporada"
      subtitulo="Define un rango de fechas con tarifa media o alta. Las noches fuera de rango usan tarifa baja."
      onCerrar={onCerrar}
      ancho="pequeno"
      pie={
        <>
          <button className="boton fantasma" onClick={onCerrar}>
            Cancelar
          </button>
          <button className="boton primario" onClick={guardar} disabled={enviando}>
            {enviando ? 'Guardando…' : 'Registrar'}
          </button>
        </>
      }
    >
      <div className="formulario-apilado">
        <Campo etiqueta="Temporada">
          <select
            value={temporada}
            onChange={(evento) => setTemporada(evento.target.value)}
          >
            <option value="BAJA">Baja</option>
            <option value="MEDIA">Media</option>
            <option value="ALTA">Alta</option>
          </select>
        </Campo>
        <div className="formulario-fila">
          <Campo etiqueta="Inicio">
            <input
              type="date"
              value={inicio}
              onChange={(evento) => setInicio(evento.target.value)}
            />
          </Campo>
          <Campo etiqueta="Fin">
            <input
              type="date"
              value={fin}
              min={inicio}
              onChange={(evento) => setFin(evento.target.value)}
            />
          </Campo>
        </div>
      </div>
      <p className="nota-pie">
        Los periodos registrados tienen prioridad sobre la temporada por defecto al
        liquidar cada noche.
      </p>
    </Modal>
  )
}
