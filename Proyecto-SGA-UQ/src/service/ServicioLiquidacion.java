package service;

import model.*;

import java.time.LocalDate;

/**
 * Servicio de liquidación: calcula el valor de una estancia <b>noche por noche</b>.
 *
 * <p>El algoritmo recorre la estancia con su iterador (que ya devuelve exactamente
 * las noches a cobrar) y, para cada noche, aplica la tarifa de la temporada a la que
 * pertenece y el número de personas que pagan esa noche.</p>
 *
 * <p>La resolución de la temporada de cada noche es <b>recursiva</b>, porque una
 * estancia puede empezar en una temporada y terminar en otra.</p>
 */
public class ServicioLiquidacion {

    private final model.CalendarioTemporadas calendario;

    public ServicioLiquidacion(model.CalendarioTemporadas calendario) {
        this.calendario = calendario;
    }

    /**
     * Liquida una estancia y devuelve el desglose noche por noche.
     *
     * @param apartamento apartamento donde se aloja el grupo
     * @param estancia    estancia a liquidar
     * @param ocupantes   ocupantes registrados (los menores de seis años no pagan)
     */
    public Liquidacion liquidar(Apartamento apartamento, Estancia estancia,
                                Iterable<Ocupante> ocupantes) {
        Liquidacion liquidacion = new Liquidacion("EST-" + System.currentTimeMillis());
        double total = liquidarRecursivo(apartamento, estancia, ocupantes, liquidacion, 0);
        liquidacion.setTotal(total);
        return liquidacion;
    }

    /**
     * Acumulación recursiva del total de la estancia, recorriendo las noches.
     *
     * @param indice posición de la noche que se está procesando
     */
    private double liquidarRecursivo(Apartamento apartamento, Estancia estancia,
                                     Iterable<Ocupante> ocupantes,
                                     Liquidacion liquidacion, int indice) {
        if (indice >= estancia.numeroNoches()) {
            return 0;
        }
        LocalDate noche = sumaDias(estancia.getFechaEntrada(), indice);
        Temporada temporada = calendario.temporadaDe(noche);
        double tarifa = apartamento.precioPorPersona(temporada);
        int pagadores = contarPagadores(ocupantes, noche);
        double valor = tarifa * pagadores;
        liquidacion.agregarNoche(new Liquidacion.DetalleNoche(noche, temporada, pagadores, tarifa, valor));
        return valor + liquidarRecursivo(apartamento, estancia, ocupantes, liquidacion, indice + 1);
    }

    /**
     * Suma de fechas de forma recursiva, sin depender de java.time.plusDays en un
     * bucle (la aritmética de fechas queda encapsulada y es fácil de verificar).
     */
    private LocalDate sumaDias(LocalDate base, int dias) {
        if (dias <= 0) {
            return base;
        }
        return sumaDias(base.plusDays(1), dias - 1);
    }

    /** Cuenta los ocupantes que pagan en una noche concreta. */
    private int contarPagadores(Iterable<Ocupante> ocupantes, LocalDate noche) {
        int pagadores = 0;
        for (Ocupante ocupante : ocupantes) {
            if (ocupante.pagaEn(noche)) {
                pagadores++;
            }
        }
        return pagadores;
    }

    /**
     * Calcula el total de una estancia sin construir el desglose.
     */
    public double totalEstancia(Apartamento apartamento, Estancia estancia,
                                 Iterable<Ocupante> ocupantes) {
        return liquidar(apartamento, estancia, ocupantes).getTotal();
    }
}
