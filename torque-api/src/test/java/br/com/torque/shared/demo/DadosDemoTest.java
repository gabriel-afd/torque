package br.com.torque.shared.demo;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.torque.shared.validacao.CpfCnpjUtil;
import br.com.torque.shared.validacao.PlacaUtil;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class DadosDemoTest {

    @Test
    void deveGerarCpfsFicticiosValidosEDistintos() {
        Set<String> cpfs = new HashSet<>();
        IntStream.range(0, 10).forEach(i -> {
            String cpf = DadosDemo.cpfFicticio(i);
            assertThat(CpfCnpjUtil.isValido(cpf)).as(cpf).isTrue();
            cpfs.add(cpf);
        });
        assertThat(cpfs).hasSize(10);
    }

    @Test
    void deveGerarPlacasValidasNosDoisFormatosESemRepetir() {
        Set<String> placas = new HashSet<>();
        IntStream.range(0, 15).forEach(i -> {
            String placa = DadosDemo.placa(i);
            assertThat(PlacaUtil.isValida(placa)).as(placa).isTrue();
            placas.add(placa);
        });
        assertThat(placas).hasSize(15);
        assertThat(DadosDemo.placa(0)).matches("[A-Z]{3}[0-9]{4}");
        assertThat(DadosDemo.placa(1)).matches("[A-Z]{3}[0-9][A-Z][0-9]{2}");
    }
}
