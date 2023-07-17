package br.com.dsousasantos91.assembleia.controller;

import br.com.dsousasantos91.assembleia.event.RecursoCriadoEvent;
import br.com.dsousasantos91.assembleia.service.VotacaoService;
import br.com.dsousasantos91.assembleia.service.dto.request.VotacaoRequest;
import br.com.dsousasantos91.assembleia.service.dto.response.ContagemVotosResponse;
import br.com.dsousasantos91.assembleia.service.dto.response.VotacaoResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Api(value = "API REST - Entidade Votacao")
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/v1/votacao")
public class VotacaoController {

	private final VotacaoService votacaoService;
	private final ApplicationEventPublisher publish;

	@ApiOperation(value = "Votação de Pauta.")
	@PostMapping(path = "/votar", produces = "application/json")
	public ResponseEntity<VotacaoResponse> abrir(@Valid @RequestBody VotacaoRequest request, HttpServletResponse servletResponse) {
		VotacaoResponse response = this.votacaoService.votar(request);
		publish.publishEvent(new RecursoCriadoEvent(this, servletResponse, response.getId()));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@ApiOperation(value = "Listagem de pautas.")
	@GetMapping
	public ResponseEntity<Page<VotacaoResponse>> pesquisar(
			@SortDefault.SortDefaults({ @SortDefault(sort = "id") }) Pageable pageable) {
		Page<VotacaoResponse> response = votacaoService.pesquisar(pageable);
		return ResponseEntity.ok(response);
	}

	@ApiOperation(value = "Contabilizar Votos.")
	@GetMapping(path = "/contabilizar/sessao/{sessaoId}")
	public ResponseEntity<ContagemVotosResponse> contabilizar(@PathVariable Long sessaoId) {
		ContagemVotosResponse response = votacaoService.contabilizar(sessaoId);
		return ResponseEntity.ok(response);
	}
}
