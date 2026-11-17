package br.com.torque.shared.demo;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.Usuario;
import br.com.torque.auth.UsuarioRepository;
import br.com.torque.catalogo.Peca;
import br.com.torque.catalogo.PecaRepository;
import br.com.torque.catalogo.Servico;
import br.com.torque.catalogo.ServicoRepository;
import br.com.torque.clientes.Cliente;
import br.com.torque.clientes.ClienteRepository;
import br.com.torque.clientes.Veiculo;
import br.com.torque.clientes.VeiculoRepository;
import br.com.torque.ordens.Orcamento;
import br.com.torque.ordens.OrdemServico;
import br.com.torque.ordens.OrdemServicoRepository;
import br.com.torque.ordens.StatusOS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados fictícios para o teste manual e a apresentação. Só existe no perfil {@code demo}
 * ({@code SPRING_PROFILES_ACTIVE=demo}) e só carrega com o banco sem clientes.
 *
 * <p>Cria 1 atendente, 2 mecânicos (e um administrador, se ainda não houver), catálogo, 10 clientes, 15 veículos
 * e OS em todos os status. As OS passam pelas regras do domínio (orçamento, aprovação, transições), então o
 * histórico de status é real; depois as datas são recuadas para o quadro e o painel terem variedade.
 * Todos os usuários demo usam a senha de {@code torque.demo.senha} (padrão {@code demo12345}).
 */
@Component
@Profile("demo")
@Order(2)
public class DadosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDemo.class);

    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final ServicoRepository servicos;
    private final PecaRepository pecas;
    private final OrdemServicoRepository ordens;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final String senha;

    private Usuario admin;
    private Usuario atendente;
    private Usuario mecanico1;
    private Usuario mecanico2;
    private final List<Servico> catalogoServicos = new ArrayList<>();
    private final List<Peca> catalogoPecas = new ArrayList<>();

    public DadosDemo(UsuarioRepository usuarios, ClienteRepository clientes, VeiculoRepository veiculos,
                     ServicoRepository servicos, PecaRepository pecas, OrdemServicoRepository ordens,
                     PasswordEncoder passwordEncoder, JdbcTemplate jdbc,
                     @Value("${torque.demo.senha:demo12345}") String senha) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.servicos = servicos;
        this.pecas = pecas;
        this.ordens = ordens;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (clientes.count() > 0) {
            log.info("Perfil demo: banco já tem clientes, dados de demonstração não carregados.");
            return;
        }
        criarUsuarios();
        criarCatalogo();
        List<Veiculo> frota = criarClientesEVeiculos();
        int quantidade = criarOrdens(frota);
        log.info("Perfil demo: {} usuários, {} clientes, {} veículos e {} OS fictícios carregados.",
                usuarios.count(), clientes.count(), veiculos.count(), quantidade);
    }

    // Cadastros -----------------------------------------------------------------------------------

    private void criarUsuarios() {
        admin = usuarios.findAll().stream()
                .filter(u -> u.getPerfil() == Perfil.ADMINISTRADOR && u.isAtivo())
                .findFirst()
                .orElseGet(() -> usuario("Administrador Demo", "admin.demo@oficina.teste", Perfil.ADMINISTRADOR));
        atendente = usuario("Ana Atendente", "ana.demo@oficina.teste", Perfil.ATENDENTE);
        mecanico1 = usuario("Raimundo Mecânico", "raimundo.demo@oficina.teste", Perfil.MECANICO);
        mecanico2 = usuario("Tiago Mecânico", "tiago.demo@oficina.teste", Perfil.MECANICO);
    }

    private Usuario usuario(String nome, String email, Perfil perfil) {
        return usuarios.save(new Usuario(nome, email, passwordEncoder.encode(senha), perfil));
    }

    private void criarCatalogo() {
        String[][] listaServicos = {
                {"Troca de óleo e filtro", "60.00"}, {"Troca de pastilhas dianteiras", "120.00"},
                {"Sangria do sistema de freio", "80.00"}, {"Alinhamento e balanceamento", "100.00"},
                {"Revisão de suspensão", "150.00"}, {"Troca de correia dentada", "250.00"},
                {"Diagnóstico eletrônico", "90.00"}, {"Higienização do ar-condicionado", "130.00"},
        };
        for (String[] s : listaServicos) {
            catalogoServicos.add(servicos.save(new Servico(s[0], new BigDecimal(s[1]))));
        }
        String[][] listaPecas = {
                {"OLE-5W30", "Óleo 5W30 sintético (1 L)", "42.90"}, {"FLT-OLEO", "Filtro de óleo", "35.00"},
                {"PST-DIANT", "Jogo de pastilhas dianteiras", "145.00"}, {"FLU-DOT4", "Fluido de freio DOT 4 (500 ml)", "22.50"},
                {"AMT-TRAS", "Amortecedor traseiro", "320.00"}, {"KIT-CORR", "Kit correia dentada e tensor", "410.00"},
                {"FLT-AR", "Filtro de ar", "38.00"}, {"FLT-CAB", "Filtro de cabine", "45.00"},
        };
        for (String[] p : listaPecas) {
            catalogoPecas.add(pecas.save(new Peca(p[0], p[1], new BigDecimal(p[2]))));
        }
    }

    private List<Veiculo> criarClientesEVeiculos() {
        String[] nomes = {"Rita Fictícia", "Pedro Fictício", "Joana Fictícia", "Bruno Fictício", "Carla Fictícia",
                "Diego Fictício", "Elisa Fictícia", "Fábio Fictício", "Gabriela Fictícia", "Hugo Fictício"};
        String[][] carros = {
                {"Renault", "Sandero", "2017", "prata"}, {"VW", "Gol 1.0", "2014", "branco"},
                {"Fiat", "Uno", "2015", "vermelho"}, {"Hyundai", "HB20", "2019", "preto"},
                {"Chevrolet", "Onix", "2020", "cinza"}, {"Toyota", "Corolla", "2018", "prata"},
                {"Honda", "Fit", "2016", "azul"}, {"Ford", "Ka", "2019", "branco"},
                {"Fiat", "Strada", "2021", "branco"}, {"VW", "Polo", "2022", "cinza"},
                {"Jeep", "Renegade", "2019", "verde"}, {"Nissan", "March", "2015", "prata"},
                {"Renault", "Kwid", "2021", "laranja"}, {"Chevrolet", "Prisma", "2017", "preto"},
                {"Fiat", "Argo", "2020", "vermelho"},
        };
        List<Veiculo> frota = new ArrayList<>();
        for (int i = 0; i < nomes.length; i++) {
            String email = nomes[i].split(" ")[0].toLowerCase().replace("á", "a") + "@cliente.teste";
            Cliente cliente = clientes.save(new Cliente(nomes[i], cpfFicticio(i), "839990000" + String.format("%02d", i), email));
            // os 5 primeiros clientes têm 2 veículos: 15 no total
            int quantos = i < 5 ? 2 : 1;
            for (int j = 0; j < quantos; j++) {
                String[] c = carros[frota.size()];
                Veiculo v = cliente.novoVeiculo(placa(frota.size()), c[0], c[1], Integer.valueOf(c[2]), c[3]);
                frota.add(veiculos.save(v));
            }
        }
        return frota;
    }

    /** Placas fictícias, alternando formato antigo (ABC1234) e Mercosul (ABC1D23). */
    static String placa(int i) {
        String letras = "" + (char) ('D' + i / 10) + (char) ('E' + i % 10) + 'M';
        return i % 2 == 0 ? letras + String.format("%04d", 1000 + i * 37) : letras + (i % 10) + (char) ('A' + i) + (10 + i);
    }

    /** CPF fictício com dígitos verificadores válidos (base 100000000 + i×1111). */
    static String cpfFicticio(int i) {
        String base = String.format("%09d", 100_000_000 + i * 1111);
        int d1 = digito(base, 10);
        int d2 = digito(base + d1, 11);
        return base + d1 + d2;
    }

    private static int digito(String numeros, int pesoInicial) {
        int soma = 0;
        for (int k = 0; k < numeros.length(); k++) {
            soma += (numeros.charAt(k) - '0') * (pesoInicial - k);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    // Ordens de serviço ---------------------------------------------------------------------------

    private int criarOrdens(List<Veiculo> frota) {
        List<OrdemServico> criadas = new ArrayList<>();
        // [veículo, problema, km, status final, mecânico (1/2/0), dias atrás]
        criadas.add(os(frota.get(0), "Ruído ao frear e pedal baixo", 84320, StatusOS.AGUARDANDO_APROVACAO, 0, 2, 1, 2));
        criadas.add(os(frota.get(1), "Revisão 60 mil km", 58900, StatusOS.ABERTA, 0, 0));
        criadas.add(os(frota.get(1), "Troca de óleo e filtros", 52410, StatusOS.ENTREGUE, 2, 190, 0));
        criadas.add(os(frota.get(1), "Correia dentada e tensor", 47150, StatusOS.ENTREGUE, 1, 360, 5));
        criadas.add(os(frota.get(1), "Amortecedores traseiros", 44020, StatusOS.RECUSADA, 0, 470, 4));
        criadas.add(os(frota.get(2), "Barulho na suspensão dianteira", 85000, StatusOS.ABERTA, 0, 0, 4));
        criadas.add(os(frota.get(3), "Ar-condicionado sem gelar", 41200, StatusOS.AGUARDANDO_APROVACAO, 0, 0, 7));
        criadas.add(os(frota.get(4), "Revisão 30 mil km", 30100, StatusOS.APROVADA, 1, 1, 0, 1));
        criadas.add(os(frota.get(5), "Luz de injeção acesa", 77300, StatusOS.APROVADA, 2, 0, 6));
        criadas.add(os(frota.get(6), "Troca de pastilhas", 61000, StatusOS.EM_EXECUCAO, 1, 1, 1, 2));
        criadas.add(os(frota.get(7), "Alinhamento puxando para a direita", 39800, StatusOS.EM_EXECUCAO, 2, 0, 3));
        criadas.add(os(frota.get(8), "Amortecedor vazando", 55500, StatusOS.AGUARDANDO_PECA, 1, 3, 4));
        criadas.add(os(frota.get(9), "Revisão geral", 20500, StatusOS.CONCLUIDA, 2, 2, 0, 1, 6));
        criadas.add(os(frota.get(10), "Troca de óleo", 48700, StatusOS.ENTREGUE, 1, 5, 0, 1));
        criadas.add(os(frota.get(11), "Barulho no motor", 99100, StatusOS.CANCELADA, 0, 4, 6));
        criadas.add(os(frota.get(12), "Revisão e filtro de cabine", 15200, StatusOS.ENTREGUE, 2, 12, 7, 0));
        criadas.add(novaVersaoEmExecucao(frota.get(13)));
        return criadas.size();
    }

    /**
     * Abre a OS com orçamento (itens do catálogo pelos índices) e a leva até o status pedido pelas regras do
     * domínio; depois recua as datas em {@code diasAtras}.
     */
    private OrdemServico os(Veiculo veiculo, String problema, int km, StatusOS destino, int qualMecanico, int diasAtras,
                            int... itens) {
        OrdemServico os = OrdemServico.abrir(veiculo, problema, km, atendente);
        Orcamento orcamento = os.getOrcamentoVigente();
        for (int i = 0; i < itens.length; i++) {
            orcamento.adicionarServico(catalogoServicos.get(itens[i]));
            orcamento.adicionarPeca(catalogoPecas.get(itens[i]), i == 0 ? 1 : 2);
        }
        Usuario mecanico = qualMecanico == 1 ? mecanico1 : qualMecanico == 2 ? mecanico2 : null;
        avancar(os, destino, mecanico);
        OrdemServico salva = ordens.saveAndFlush(os);
        recuarDatas(salva.getId(), diasAtras);
        return salva;
    }

    private void avancar(OrdemServico os, StatusOS destino, Usuario mecanico) {
        if (destino == StatusOS.ABERTA) {
            return;
        }
        if (destino == StatusOS.CANCELADA) {
            os.alterarStatus(StatusOS.CANCELADA, atendente);
            return;
        }
        os.alterarStatus(StatusOS.AGUARDANDO_APROVACAO, atendente);
        if (destino == StatusOS.AGUARDANDO_APROVACAO) {
            return;
        }
        if (destino == StatusOS.RECUSADA) {
            os.recusarOrcamento(1, atendente);
            return;
        }
        os.aprovarOrcamento(1, atendente);
        os.atribuirMecanico(mecanico, admin);
        for (StatusOS passo : caminhoAte(destino)) {
            os.alterarStatus(passo, passo == StatusOS.ENTREGUE ? atendente : mecanico);
        }
    }

    private static List<StatusOS> caminhoAte(StatusOS destino) {
        return switch (destino) {
            case EM_EXECUCAO -> List.of(StatusOS.EM_EXECUCAO);
            case AGUARDANDO_PECA -> List.of(StatusOS.EM_EXECUCAO, StatusOS.AGUARDANDO_PECA);
            case CONCLUIDA -> List.of(StatusOS.EM_EXECUCAO, StatusOS.CONCLUIDA);
            case ENTREGUE -> List.of(StatusOS.EM_EXECUCAO, StatusOS.CONCLUIDA, StatusOS.ENTREGUE);
            default -> List.of();
        };
    }

    /** RN07 na demonstração: OS em execução cujo orçamento foi alterado (versão 2, de volta à aprovação). */
    private OrdemServico novaVersaoEmExecucao(Veiculo veiculo) {
        OrdemServico os = OrdemServico.abrir(veiculo, "Freio fazendo barulho", 67400, atendente);
        os.getOrcamentoVigente().adicionarServico(catalogoServicos.get(1));
        os.getOrcamentoVigente().adicionarPeca(catalogoPecas.get(2), 1);
        avancar(os, StatusOS.EM_EXECUCAO, mecanico1);
        Orcamento versao2 = os.orcamentoParaEdicao(atendente);
        versao2.adicionarServico(catalogoServicos.get(2));
        versao2.adicionarPeca(catalogoPecas.get(3), 2);
        OrdemServico salva = ordens.saveAndFlush(os);
        recuarDatas(salva.getId(), 1);
        return salva;
    }

    /** Recua abertura, entrega, decisões e histórico da OS em N dias (mantém a ordem relativa). */
    private void recuarDatas(Long id, int dias) {
        if (dias <= 0) {
            return;
        }
        String intervalo = "make_interval(days => " + dias + ")";
        jdbc.update("update ordem_servico set data_abertura = data_abertura - " + intervalo
                + ", data_entrega = data_entrega - " + intervalo + " where id = ?", id);
        jdbc.update("update orcamento set data_decisao = data_decisao - " + intervalo + " where ordem_servico_id = ?", id);
        jdbc.update("update historico_status set data_hora = data_hora - " + intervalo + " where ordem_servico_id = ?", id);
    }
}
