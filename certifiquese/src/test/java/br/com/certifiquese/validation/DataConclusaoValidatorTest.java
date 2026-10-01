package br.com.certifiquese.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.certifiquese.dto.CertificadoRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class DataConclusaoValidatorTest {

    private DataConclusaoValidator validator;
    private Validator beanValidator;

    @BeforeEach
    void setUp() {
        validator = new DataConclusaoValidator();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        beanValidator = factory.getValidator();
    }

    @Test
    @DisplayName("Deve aceitar valor nulo para que @NotNull seja o responsável")
    void deveAceitarNulo() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar datas medievais ou muito antigas como ano 0303")
    void deveRejeitarDataMuitoAntiga() {
        assertThat(validator.isValid(LocalDate.of(303, 3, 3), null)).isFalse();
        assertThat(validator.isValid(LocalDate.of(1900, 1, 1), null)).isFalse();
        assertThat(validator.isValid(LocalDate.of(1959, 12, 31), null)).isFalse();
    }

    @Test
    @DisplayName("Deve aceitar a data limite inferior exata de 01/01/1960")
    void deveAceitarLimiteMinimo() {
        assertThat(validator.isValid(LocalDate.of(1960, 1, 1), null)).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar datas contemporâneas válidas e a data de hoje")
    void deveAceitarDatasContemporaneasEHoje() {
        assertThat(validator.isValid(LocalDate.of(2024, 5, 20), null)).isTrue();
        assertThat(validator.isValid(LocalDate.now(), null)).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar datas futuras")
    void deveRejeitarDataFutura() {
        assertThat(validator.isValid(LocalDate.now().plusDays(1), null)).isFalse();
        assertThat(validator.isValid(LocalDate.now().plusYears(1), null)).isFalse();
    }

    @Test
    @DisplayName("Deve respeitar ano mínimo customizado se fornecido")
    void deveRespeitarAnoMinimoCustomizado() {
        DataConclusaoValidator customValidator = new DataConclusaoValidator(2000);
        assertThat(customValidator.isValid(LocalDate.of(1999, 12, 31), null)).isFalse();
        assertThat(customValidator.isValid(LocalDate.of(2000, 1, 1), null)).isTrue();
    }

    @Test
    @DisplayName("Deve inicializar ano mínimo via anotação existente em classe")
    void deveInicializarViaAnotacao() throws NoSuchFieldException {
        class ObjetoExemplo {
            @DataConclusaoValida(anoMinimo = 2010)
            LocalDate data;
        }

        DataConclusaoValida anotacao = ObjetoExemplo.class
                .getDeclaredField("data")
                .getAnnotation(DataConclusaoValida.class);

        DataConclusaoValidator v = new DataConclusaoValidator();
        v.initialize(anotacao);

        assertThat(v.isValid(LocalDate.of(2009, 12, 31), null)).isFalse();
        assertThat(v.isValid(LocalDate.of(2010, 1, 1), null)).isTrue();
    }

    @Test
    @DisplayName("Deve validar a restrição no CertificadoRequestDTO via Bean Validation")
    void deveValidarViaBeanValidationNoDto() {
        CertificadoRequestDTO dtoInvalidoAno303 = new CertificadoRequestDTO(
                "foto.png",
                "Curso Teste",
                "Empresa Teste",
                LocalDate.of(303, 3, 3),
                List.of("java"),
                40,
                "Descricao",
                "https://link.com",
                true
        );

        var violacoes = beanValidator.validate(dtoInvalidoAno303);
        assertThat(violacoes).isNotEmpty();
        assertThat(violacoes)
                .anyMatch(v -> v.getPropertyPath().toString().equals("dataConclusao")
                        && v.getMessage().contains("01/01/1960"));
    }
}
