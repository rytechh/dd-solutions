package br.com.rytechh.greenshift.application.ports.in;

import br.com.rytechh.greenshift.core.domain.WorkloadJob;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de entrada: consulta de status de jobs para o cliente monitorar o ciclo de vida.
 */
public interface ConsultarJobPort {

    Optional<WorkloadJob> buscarPorId(UUID id);

    List<WorkloadJob> listarTodos();
}
