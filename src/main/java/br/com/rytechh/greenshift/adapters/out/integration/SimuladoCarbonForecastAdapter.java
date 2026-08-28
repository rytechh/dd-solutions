package br.com.rytechh.greenshift.adapters.out.integration;

import br.com.rytechh.greenshift.application.ports.out.CarbonForecastPort;
import br.com.rytechh.greenshift.core.domain.CarbonForecast;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de integração: simula a previsão de intensidade de carbono (gCO2/kWh)
 * das próximas 24h com uma curva realista de matriz elétrica brasileira —
 * vale solar ao meio-dia, vale eólico de madrugada, pico na "ponta" (18h-21h).
 *
 * MVP: evita dependência de chave de API externa (ex: Electricity Maps, WattTime)
 * durante a demo do hackathon. Para plugar uma API real, basta criar outra classe
 * implementando {@link CarbonForecastPort} (ex: ElectricityMapsAdapter) e trocar o
 * bean instanciado em {@code config.BeanConfig} — o CORE não muda uma linha.
 */
public class SimuladoCarbonForecastAdapter implements CarbonForecastPort {

    private static final double INTENSIDADE_BASE = 420.0;   // gCO2/kWh — matriz térmica de referência
    private static final double AMPLITUDE_SOLAR = 180.0;    // redução no pico solar (meio-dia)
    private static final double AMPLITUDE_EOLICA = 90.0;    // redução no vale eólico (madrugada)
    private static final double AMPLITUDE_PONTA = 110.0;    // aumento no horário de ponta (noite)

    @Override
    public List<CarbonForecast> obterPrevisao24h(String regionCode) {
        List<CarbonForecast> previsao = new ArrayList<>();
        ZonedDateTime agora = ZonedDateTime.now(ZoneOffset.UTC).withMinute(0).withSecond(0).withNano(0);

        for (int hora = 0; hora < 24; hora++) {
            ZonedDateTime instanteSlot = agora.plusHours(hora);
            int horaDoDia = instanteSlot.getHour();

            double intensidade = INTENSIDADE_BASE
                    - AMPLITUDE_SOLAR * gaussiana(horaDoDia, 13, 3)
                    - AMPLITUDE_EOLICA * gaussiana(horaDoDia, 4, 3)
                    + AMPLITUDE_PONTA * gaussiana(horaDoDia, 19, 2);

            Instant timestamp = instanteSlot.toInstant();
            previsao.add(new CarbonForecast(timestamp, arredondar(intensidade), regionCode));
        }
        return previsao;
    }

    private double gaussiana(int hora, int centro, double largura) {
        double x = (hora - centro) / largura;
        return Math.exp(-0.5 * x * x);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
