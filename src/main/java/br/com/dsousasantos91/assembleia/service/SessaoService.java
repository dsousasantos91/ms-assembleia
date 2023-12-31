package br.com.dsousasantos91.assembleia.service;

import br.com.dsousasantos91.assembleia.domain.Assembleia;
import br.com.dsousasantos91.assembleia.domain.Associado;
import br.com.dsousasantos91.assembleia.domain.Pauta;
import br.com.dsousasantos91.assembleia.domain.Sessao;
import br.com.dsousasantos91.assembleia.exception.GenericBadRequestException;
import br.com.dsousasantos91.assembleia.exception.GenericNotFoundException;
import br.com.dsousasantos91.assembleia.mapper.AssociadoMapper;
import br.com.dsousasantos91.assembleia.mapper.SessaoMapper;
import br.com.dsousasantos91.assembleia.repository.AssembleiaRepository;
import br.com.dsousasantos91.assembleia.repository.AssociadoRepository;
import br.com.dsousasantos91.assembleia.repository.PautaRepository;
import br.com.dsousasantos91.assembleia.repository.SessaoRepository;
import br.com.dsousasantos91.assembleia.scheduler.NotificadorScheduler;
import br.com.dsousasantos91.assembleia.service.dto.request.AssociadoRequest;
import br.com.dsousasantos91.assembleia.service.dto.request.SessaoEmLoteRequest;
import br.com.dsousasantos91.assembleia.service.dto.request.SessaoRequest;
import br.com.dsousasantos91.assembleia.service.dto.response.ContagemVotosResponse;
import br.com.dsousasantos91.assembleia.service.dto.response.SessaoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final PautaRepository pautaRepository;
    private final AssembleiaRepository assembleiaRepository;
    private final AssociadoRepository associadoRepository;
    private final SessaoMapper sessaoMapper;
    private final AssociadoMapper associadoMapper;
    private final NotificadorScheduler notificadorScheduler;
    private final ValidarCPFService validarCPFService;

    public SessaoResponse abrir(SessaoRequest request) {
        verificarExistenciaDeSessaoAberta(singletonList(request.getPautaId()));
        log.info("Abrindo sessão para pauta ID [{}].", request.getPautaId());
        Pauta pauta = pautaRepository.findById(request.getPautaId())
                .orElseThrow(() -> new GenericNotFoundException(String.format("Pauta com ID: %d não encontrada.", request.getPautaId())));
        Sessao sessao = Sessao.builder()
                .pauta(pauta)
                .associados(emptyList())
                .sessaoPrivada(request.getSessaoPrivada())
                .dataHoraInicio(LocalDateTime.now())
                .dataHoraFim(request.getDataHoraFim())
                .build();
        if (Boolean.TRUE.equals(request.getSessaoPrivada()) && nuloOuVazio(request.getAssociados())) {
            throw new GenericBadRequestException("Associados não enviados");
        }
        if (Boolean.TRUE.equals(request.getSessaoPrivada()) && !nuloOuVazio(request.getAssociados())) {
            sessao.setAssociados(parseAssociados(request.getAssociados()));
        }
        Sessao sessaoAberta = sessaoRepository.save(sessao);
        notificadorScheduler.agendarNotificacao(sessaoAberta);
        log.info("Sessão para pauta [{}] aberta com sucesso.", sessaoAberta.getPauta().getTitulo());
        return sessaoMapper.toResponse(sessaoAberta);
    }

    public List<SessaoResponse> abrirEmLote(SessaoEmLoteRequest request) {
        log.info("Abrindo sessões em lote");
        List<Pauta> pautas = new ArrayList<>();
        List<Sessao> sessoes;
        log.info("Buscando assembleia ID [{}].", request.getAssembleiaId());
        Optional<Assembleia> assembleia = this.assembleiaRepository.findById(request.getAssembleiaId());
        if (nuloOuVazio(request.getIdsPautas()) && !assembleia.isPresent()) {
            throw new GenericBadRequestException("assembleiaId não enviado OU idsPautas está vazio.");
        }
        if (nuloOuVazio(request.getIdsPautas()) && assembleia.isPresent()) {
            pautas = assembleia.get().getPautas();
            List<Long> pautasIds = pautas.stream().map(Pauta::getId).collect(toList());
            request.setIdsPautas(pautasIds);
        }
        verificarExistenciaDeSessaoAberta(request.getIdsPautas());
        sessoes = montarSessoes(request, pautas);
        if (Boolean.TRUE.equals(request.getSessaoPrivada()) && nuloOuVazio(request.getAssociados())) {
            throw new GenericBadRequestException("Sessão privada mas não há associado para relacioar. Envie associados na requisição.");
        }
        if (Boolean.TRUE.equals(request.getSessaoPrivada()) && !nuloOuVazio(request.getAssociados())) {
            List<Associado> associados = parseAssociados(request.getAssociados());
            sessoes.forEach(sessao -> sessao.setAssociados(associados));
        }
        List<Sessao> sessoesEmLote = sessaoRepository.saveAll(sessoes);
        sessoesEmLote.forEach(sessao -> {
            notificadorScheduler.agendarNotificacao(sessao);
            log.info("Sessão para pauta [{}] aberta com sucesso.", sessao.getPauta().getTitulo());
        });
        return sessaoMapper.toResponses(sessoesEmLote);
    }

    public Page<SessaoResponse> pesquisar(Pageable pageable) {
        log.info("Pesquisar sessões");
        return this.sessaoRepository.findAll(pageable).map(sessaoMapper::toResponse);
    }

    public SessaoResponse buscarPorId(Long id) {
        log.info("Buscar sessão ID [{}]", id);
        Sessao sessao = this.sessaoRepository.findById(id).orElseThrow(GenericNotFoundException::new);
        log.info("Sessão ID [{}] encontrada.", sessao.getId());
        return this.sessaoMapper.toResponse(sessao);
    }

    public SessaoResponse prorrogar(SessaoRequest request) {
        log.info("Prorrogando sessão para pauta ID [{}]", request.getPautaId());
        return this.sessaoRepository.findSessoesAbertasPorPautaIdIn(singletonList(request.getPautaId()), LocalDateTime.now())
                .stream().findFirst()
                .map(sessaoEncontrada -> {
                    sessaoEncontrada.setDataHoraFim(request.getDataHoraFim());
                    notificadorScheduler.agendarNotificacao(sessaoEncontrada);
                    log.info("Sessão ID [{}] prorrogada até [{}]", sessaoEncontrada.getId(), sessaoEncontrada.getDataHoraFim());
                    return this.sessaoMapper.toResponse(sessaoEncontrada);
                })
                .orElseGet(() -> this.abrir(request));
    }

    public SessaoResponse encerrar(Long sessaoId) {
        log.info("Encerrando sessão ID [{}]", sessaoId);
        Sessao sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new GenericNotFoundException(String.format("Sessao com ID: %d não encontrada.", sessaoId)));
        if (sessao.getDataHoraFim().isBefore(LocalDateTime.now()))
            throw new GenericBadRequestException(String.format("Sessao com ID: %d encerrada anteriormente.", sessaoId));
        sessao.setDataHoraFim(LocalDateTime.now());
        Sessao sessaoEncerrada = sessaoRepository.save(sessao);
        notificadorScheduler.agendarNotificacao(sessaoEncerrada);
        log.info("Sessão ID [{}] encerrada com sucesso.", sessaoId);
        return sessaoMapper.toResponse(sessaoEncerrada);
    }

    public void confirmarEnvioDeResultado(ContagemVotosResponse contagem) {
        log.info("Buscando sessão da pauta [{}].", contagem.getPauta().getTitulo());
        Optional<Sessao> sessao = sessaoRepository.findById(contagem.getSessaoId());
        if (!sessao.isPresent()) throw new RuntimeException("Sessão ID " + contagem.getSessaoId() + " não encontrada.");
        log.info("Sessão [{}] encontrada.", sessao.get().getId());
        sessao.get().setResultadoEnviado(Boolean.TRUE);
        sessaoRepository.save(sessao.get());
        log.info("Confirmação de envio do resultado da sessão [{}] recebida com sucesso.", sessao.get().getId());
    }

    public void apagar(Long id) {
        log.info("Apagando sessão ID [{}]", id);
        this.sessaoRepository.deleteById(id);
        log.info("Sessão ID [{}] apagada com sucesso", id);
    }

    private List<Sessao> montarSessoes(SessaoEmLoteRequest request, List<Pauta> pautas) {
        log.info("Instanciando sessões");
        return request.getIdsPautas().stream().map(pautaId -> {
            Optional<Pauta> pauta;
            Sessao sessao = Sessao.builder()
                    .associados(emptyList())
                    .sessaoPrivada(request.getSessaoPrivada())
                    .dataHoraInicio(LocalDateTime.now())
                    .dataHoraFim(request.getDataHoraFim())
                    .build();
            if (pautas.isEmpty()) {
                pauta = pautaRepository.findById(pautaId);
            }
            else {
                pauta = pautas.stream().filter(pauta_ -> pautaId.equals(pauta_.getId())).findFirst();
            }
            pauta.ifPresent(sessao::setPauta);
            log.info("Sessão ID [{}] instanciada com sucesso para a pauta [{}]", sessao.getId(), sessao.getPauta().getTitulo());
            return sessao;
        }).collect(toList());
    }

    private List<Associado> parseAssociados(List<AssociadoRequest> associados) {
        log.info("Relacionando associados a sessão");
        return associados.stream()
                .map(associado -> {
                    validarCPFService.validar(associado.getCpf());
                    return associadoRepository.findByCpf(associado.getCpf())
                            .orElseGet(() -> associadoMapper.toEntity(associado));
                })
                .collect(toList());
    }

    private void verificarExistenciaDeSessaoAberta(List<Long> pautasIds) {
        List<Sessao> sessaoExiste = sessaoRepository.findSessoesAbertasPorPautaIdIn(pautasIds, LocalDateTime.now());
        if (sessaoExiste.isEmpty()) return;
        String titulos = sessaoExiste.stream().map(sessao -> sessao.getPauta().getTitulo()).collect(joining(", "));
        throw new GenericBadRequestException(String.format("Existe uma sessão aberta para as pautas: %s.", titulos));
    }

    private <T> Boolean nuloOuVazio(List<T> list) {
        return isNull(list) || list.isEmpty();
    }
}
