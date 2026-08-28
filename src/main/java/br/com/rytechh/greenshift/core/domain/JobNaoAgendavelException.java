package br.com.rytechh.greenshift.core.domain;

/**
 * Lançada quando não existe nenhuma janela de tempo, dentro do deadline informado,
 * capaz de acomodar a duração do job.
 */
public class JobNaoAgendavelException extends RuntimeException {

    public JobNaoAgendavelException(String message) {
        super(message);
    }
}
