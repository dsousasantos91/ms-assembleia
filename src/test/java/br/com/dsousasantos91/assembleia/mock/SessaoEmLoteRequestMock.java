package br.com.dsousasantos91.assembleia.mock;

import br.com.dsousasantos91.assembleia.service.dto.request.AssociadoRequest;
import br.com.dsousasantos91.assembleia.service.dto.request.SessaoEmLoteRequest;
import br.com.dsousasantos91.assembleia.service.dto.request.TempoSessaoRequest;

import java.util.Arrays;
import java.util.List;

public class SessaoEmLoteRequestMock {
    private SessaoEmLoteRequest element;
    private SessaoEmLoteRequestMock(){}

    public static SessaoEmLoteRequestMock mocked() {
        SessaoEmLoteRequestMock mock = new SessaoEmLoteRequestMock();
        initializeDefaultData(mock);
        return mock;
    }

    public static void initializeDefaultData(SessaoEmLoteRequestMock mock) {
        mock.element = SessaoEmLoteRequest.builder()
                .assembleiaId(1L)
                .idsPautas(Arrays.asList(1L, 2L, 3L))
                .associados(AssociadoRequestMock.mocked().mockList())
                .sessaoPrivada(Boolean.TRUE)
                .tempoSessao(TempoSessaoRequestMock.mocked().mock())
                .build();
    }

    public SessaoEmLoteRequestMock withAssembleiaId(Long param) {
        element.setAssembleiaId(param);
        return this;
    }

    public SessaoEmLoteRequestMock withListIdsPautas(Long... params) {
        element.setIdsPautas(Arrays.asList(params));
        return this;
    }

    public SessaoEmLoteRequestMock withListIdsPautasList(List<Long> listIds) {
        element.setIdsPautas(listIds);
        return this;
    }

    public SessaoEmLoteRequestMock withListAssociados(AssociadoRequest... params) {
        element.setAssociados(Arrays.asList(params));
        return this;
    }

    public SessaoEmLoteRequestMock withListAssociadosList(List<AssociadoRequest> associados) {
        element.setAssociados(associados);
        return this;
    }

    public SessaoEmLoteRequestMock withSessaoPrivada(Boolean param) {
        element.setSessaoPrivada(param);
        return this;
    }

    public SessaoEmLoteRequestMock withTempoSessao(TempoSessaoRequest param) {
        element.setTempoSessao(param);
        return this;
    }

    public SessaoEmLoteRequest mock() {
        return element;
    }
}
