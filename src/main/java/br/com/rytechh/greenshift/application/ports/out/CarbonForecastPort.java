package br.com.rytechh.greenshift.application.ports.out;

import br.com.rytechh.greenshift.core.domain.CarbonForecast;

import java.util.List;

/**
 * Porta de saída: obtenção da previsão de intensidade de carbono das próximas 24h.
 * Implementada por um adapter de integração (API externa real ou simulada).
 */
public interface CarbonForecastPort {

    List<CarbonForecast> obterPrevisao24h(String regionCode);
}
