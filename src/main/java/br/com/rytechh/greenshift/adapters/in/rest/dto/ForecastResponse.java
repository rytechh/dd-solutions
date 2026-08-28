package br.com.rytechh.greenshift.adapters.in.rest.dto;

import br.com.rytechh.greenshift.core.domain.CarbonForecast;

import java.time.Instant;
import java.util.List;

/**
 * Curva de intensidade de carbono das próximas 24h — alimenta o gráfico do
 * dashboard e evidencia visualmente os "vales verdes" usados no agendamento.
 */
public record ForecastResponse(
        String regionCode,
        double menorIntensidade,
        double maiorIntensidade,
        Instant melhorJanela,
        List<Ponto> curva
) {
    public record Ponto(Instant timestamp, double co2IntensityGco2Kwh) {
    }

    public static ForecastResponse de(String regionCode, List<CarbonForecast> previsao) {
        CarbonForecast minimo = previsao.stream()
                .min(java.util.Comparator.comparingDouble(CarbonForecast::co2Intensity))
                .orElseThrow();
        double maximo = previsao.stream()
                .mapToDouble(CarbonForecast::co2Intensity)
                .max()
                .orElse(0);

        List<Ponto> curva = previsao.stream()
                .map(p -> new Ponto(p.timestamp(), p.co2Intensity()))
                .toList();

        return new ForecastResponse(regionCode, minimo.co2Intensity(), maximo, minimo.timestamp(), curva);
    }
}
