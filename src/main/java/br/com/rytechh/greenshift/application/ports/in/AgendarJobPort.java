package br.com.rytechh.greenshift.application.ports.in;

import br.com.rytechh.greenshift.core.domain.WorkloadJob;

import java.time.Instant;

/**
 * Porta de entrada: caso de uso de recebimento e agendamento otimizado de um job.
 */
public interface AgendarJobPort {

    WorkloadJob agendar(NovoJobCommand command);

    record NovoJobCommand(String comando, String descricao, int duracaoMinutos,
                           Instant deadline, double consumoEstimadoKw) {
    }
}
