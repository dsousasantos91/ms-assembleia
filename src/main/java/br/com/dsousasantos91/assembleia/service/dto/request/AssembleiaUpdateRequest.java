package br.com.dsousasantos91.assembleia.service.dto.request;

import br.com.dsousasantos91.assembleia.domain.enumeration.TipoAssembleiaEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssembleiaUpdateRequest {
    @NotNull(message = "{0} é obrigatório")
    @Enumerated(EnumType.STRING)
    private TipoAssembleiaEnum tipoAssembleia;

    @NotNull(message = "{0} é obrigatório")
    @JsonFormat(pattern="dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataHoraInicioApuracao;

    @NotNull(message = "{0} é obrigatório")
    @JsonFormat(pattern="dd/MM/yyyy HH:mm:ss")
    private LocalDateTime dataHoraFimApuracao;

    @Size(min = 3, max = 50, message = "É obrigatório digitar um mínimo de {1} para {0}")
    @NotBlank(message = "{0} é obrigatório")
    private String cooperativa;

    @Valid
    @NotNull(message = "{0} é obrigatório")
    private EnderecoRequest local;

    @Size(min = 3, max = 50, message = "É obrigatório digitar um mínimo de {1} para {0}")
    @NotBlank(message = "{0} é obrigatório")
    private String presidente;

    @Size(min = 3, message = "É obrigatório digitar um mínimo de {1} para {0}")
    @NotBlank(message = "{0} é obrigatório")
    private String secretario;
}
