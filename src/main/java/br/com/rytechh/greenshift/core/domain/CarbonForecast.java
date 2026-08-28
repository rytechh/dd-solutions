package br.com.rytechh.greenshift.core.domain;

import java.time.Instant;

/**
 * Ponto de previsão de intensidade de carbono da rede elétrica para uma hora específica.
 *
 * @param timestamp   início da janela horária
 * @param co2Intensity gramas de CO2 por kWh estimados para a janela
 * @param regionCode  código da região/rede elétrica (ex: "BR-S", "BR-NE")
 */
public record CarbonForecast(Instant timestamp, double co2Intensity, String regionCode) {
}
