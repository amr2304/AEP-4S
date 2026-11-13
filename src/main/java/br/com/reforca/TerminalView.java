package br.com.reforca;

import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

class TerminalView {
    private final Scanner in = new Scanner(System.in);
    private final UsuarioService usuarios;
    private final DisciplinaService disciplinas;
    private final PlanoService planos;
    private final SessaoService sessoes;
    TerminalView(UsuarioService u, DisciplinaService d, PlanoService p, SessaoService s) {
        usuarios = u;
        disciplinas = d;
        planos = p;
        sessoes = s;
    }

    void iniciar() {
        boolean sair = false;
        while (!sair) {
            System.out.println("\n=== REFORÇA | Mentoria escolar voluntária ===\n\n1 - Cadastrar aluno\n2 - Cadastrar mentor\n3 - Cadastrar coordenador\n4 - Cadastrar disciplina\n5 - Listar cadastros\n6 - Planos de estudo\n7 - Agendar sessão\n8 - Consultar sessões\n9 - Alterar sessão\n10 - Cancelar sessão\n11 - Registrar resultado\n12 - Relatório\n0 - Sair");
            try {
                switch (ler("Opção: ")) {
                    case "1" -> aluno();
                    case "2" -> mentor();
                    case "3" -> coordenador();
                    case "4" -> disciplina();
                    case "5" -> cadastros();
                    case "6" -> menuPlanos();
                    case "7" -> agendar();
                    case "8" -> consultar();
                    case "9" -> alterar();
                    case "10" -> cancelar();
                    case "11" -> resultado();
                    case "12" -> relatorio();
                    case "0" -> sair = true;
                    default -> System.out.println("Opção inválida.");
                }
            }
            catch (Exception e) {
                System.out.println("Não foi possível concluir: " + e.getMessage());
            }
        }
    }

    private String ler(String q) {
        System.out.print(q);
        return in.nextLine().trim();
    }

    private String req(String q) {
        while (true) {
            String s = ler(q);
            if (!s.isBlank()) return s;
            System.out.println("Campo obrigatório.");
        }
    }

    private long num(String q) {
        return Long.parseLong(req(q));
    }

    private int inteiro(String q) {
        return Integer.parseInt(req(q));
    }

    private LocalDate data(String q) {
        while (true) {
            try {
                return LocalDate.parse(req(q));
            }
            catch (DateTimeParseException e) {
                System.out.println("Use AAAA-MM-DD.");
            }
        }
    }

    private LocalTime hora(String q) {
        while (true) {
            try {
                return LocalTime.parse(req(q));
            }
            catch (DateTimeParseException e) {
                System.out.println("Use HH:MM.");
            }
        }
    }

    private Long opcionalLong(String q) {
        String s = ler(q);
        return s.isBlank()?null:Long.valueOf(s);
    }

    private LocalDate opcionalData(String q) {
        String s = ler(q);
        return s.isBlank()?null:LocalDate.parse(s);
    }

    private void aluno() throws SQLException {
        Aluno a = new Aluno(0, req("Nome completo: "), req("E-mail: "), req("Telefone: "), data("Nascimento (AAAA-MM-DD): "), req("Ano/série: "), req("Escola de origem: "), req("Nome do responsável: "), req("Telefone do responsável: "));
        System.out.println("Aluno criado com ID #" + usuarios.aluno(a));
    }

    private void mentor() throws SQLException {
        List<Disciplina> ds = disciplinas.listar();
        if (ds.isEmpty()) throw new RegraDeNegocioException("Cadastre disciplinas antes dos mentores.");
        System.out.println("Disciplinas:");
        ds.forEach(x -> System.out.println("#" + x.id() + " | " + x.nome() + " | " + x.nivel()));
        List<Long> ids = Arrays.stream(req("IDs das disciplinas separados por vírgula: ").split(",")).map(String :: trim).map(Long :: parseLong).toList();
        List<Disponibilidade> disponibilidade = new ArrayList<> ();
        int n = inteiro("Quantas faixas semanais? ");
        for (int i = 0;i < n;i ++) {
            DayOfWeek dia = dia(req("Dia " + (i + 1) + " (SEGUNDA, TERCA, QUARTA, QUINTA, SEXTA, SABADO, DOMINGO): "));
            LocalTime ini = hora("Início: "), fim = hora("Fim: ");
            disponibilidade.add(new Disponibilidade(dia, ini, fim));
        }
        Mentor m = new Mentor(0, req("Nome completo: "), req("E-mail: "), req("Telefone: "), req("Formação acadêmica: "), ids, disponibilidade);
        System.out.println("Mentor criado com ID #" + usuarios.mentor(m));
    }

    private DayOfWeek dia(String x) {
        return switch (x.toUpperCase(Locale.ROOT)) {
            case "SEGUNDA" -> DayOfWeek.MONDAY;
            case "TERCA", "TERÇA" -> DayOfWeek.TUESDAY;
            case "QUARTA" -> DayOfWeek.WEDNESDAY;
            case "QUINTA" -> DayOfWeek.THURSDAY;
            case "SEXTA" -> DayOfWeek.FRIDAY;
            case "SABADO", "SÁBADO" -> DayOfWeek.SATURDAY;
            case "DOMINGO" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Dia da semana inválido.");
        }
        ;
    }

    private void coordenador() throws SQLException {
        Coordenador c = new Coordenador(0, req("Nome completo: "), req("E-mail: "), req("Telefone: "), req("Matrícula funcional: "));
        System.out.println("Coordenador criado com ID #" + usuarios.coordenador(c));
    }

    private void disciplina() throws SQLException {
        long id = disciplinas.criar(new Disciplina(0, req("Nome: "), req("Nível de ensino: "), req("Descrição: ")));
        System.out.println("Disciplina criada com ID #" + id);
    }

    private void cadastros() throws SQLException {
        System.out.println("\nUSUÁRIOS");
        usuarios.listar().forEach(System.out :: println);
        System.out.println("\nDISCIPLINAS");
        disciplinas.listar().forEach(System.out :: println);
    }

    private void menuPlanos() throws SQLException {
        System.out.println("1 Criar | 2 Listar | 3 Alterar | 4 Excluir");
        switch (ler("Opção: ")) {
            case "1" ->  {
                long id = planos.criar(new PlanoDeEstudo(0, num("ID do aluno: "), req("Título: "), req("Objetivo: "), data("Data inicial: "), data("Data final: ")));
                System.out.println("Plano criado com ID #" + id);
            }
            case "2" -> planos.listar().forEach(System.out :: println);
            case "3" -> planos.atualizar(num("ID do plano: "), req("Novo título: "), req("Novo objetivo: "), data("Data inicial: "), data("Data final: "));
            case "4" -> planos.excluir(num("ID do plano: "));
            default -> System.out.println("Opção inválida.");
        }
    }

    private SessaoMentoria lerSessao(long id, String status) {
        long aluno = num("ID do aluno: "), mentor = num("ID do mentor: "), disc = num("ID da disciplina: ");
        String pl = ler("ID do plano (Enter se sem plano): ");
        Long plano = pl.isBlank()?null:Long.valueOf(pl);
        LocalDate dt = data("Data (AAAA-MM-DD): ");
        LocalTime ini = hora("Início (HH:MM): "), fim = hora("Fim (HH:MM): ");
        String mod = req("Modalidade: 1 presencial | 2 online: ");
        if (mod.equals("1")) return new SessaoPresencial(id, aluno, mentor, disc, plano, dt, ini, fim, status, req("Sala: "), req("Endereço: "));
        if (mod.equals("2")) return new SessaoOnline(id, aluno, mentor, disc, plano, dt, ini, fim, status, req("Plataforma: "), req("Link: "));
        throw new IllegalArgumentException("Escolha 1 ou 2 para a modalidade.");
    }

    private void agendar() throws SQLException {
        System.out.println("Sessão agendada com ID #" + sessoes.criar(lerSessao(0, "AGENDADA")));
    }

    private void imprimir(List<SessaoResumo> xs) {
        if (xs.isEmpty()) System.out.println("Nenhuma sessão encontrada.");
        for (SessaoResumo s : xs) {
            System.out.printf("#%d | %s %s-%s | %s %s | Aluno %s | Mentor %s | Disciplina %s | %s%n", s.id(), s.data(), s.inicio(), s.fim(), s.modalidade(), s.status(), s.aluno(), s.mentor(), s.disciplina(), s.instrucoes());
            if (s.motivoCancelamento() != null) System.out.println("  Motivo do cancelamento: " + s.motivoCancelamento());
            if (s.nota() != null) System.out.println("  Presença: " + s.presenca() + " | Evolução: " + s.nota() + "/5 | Comentário: " + s.comentario());
        }
    }

    private Filtro filtro() throws SQLException {
        System.out.println("Filtro: campos vazios não restringem.");
        return new Filtro(opcionalData("Data inicial (AAAA-MM-DD): "), opcionalData("Data final (AAAA-MM-DD): "), opcionalLong("ID do mentor: "), opcionalLong("ID da disciplina: "));
    }

    private void consultar() throws SQLException {
        Filtro f = filtro();
        imprimir(sessoes.listar(f.de, f.ate, f.mentor, f.disciplina));
    }

    private void alterar() throws SQLException {
        long id = num("ID da sessão: ");
        SessaoMentoria atual = sessoes.buscar(id).orElseThrow(() -> new RegraDeNegocioException("Sessão não encontrada."));
        if (!atual.status.equals("AGENDADA")) throw new RegraDeNegocioException("Somente sessão agendada pode ser alterada.");
        sessoes.atualizar(lerSessao(id, "AGENDADA"));
        System.out.println("Sessão atualizada.");
    }

    private void cancelar() throws SQLException {
        sessoes.cancelar(num("ID da sessão: "), req("Motivo do cancelamento: "));
        System.out.println("Sessão cancelada e mantida no histórico.");
    }

    private void resultado() throws SQLException {
        long id = num("ID da sessão: ");
        boolean presente = req("Aluno presente? (S/N): ").equalsIgnoreCase("S");
        sessoes.registrar(new RegistroSessao(id, presente, inteiro("Evolução de 1 a 5: "), req("Comentário do mentor: ")));
        System.out.println("Resultado registrado; sessão marcada como REALIZADA.");
    }

    private void relatorio() throws SQLException {
        Filtro f = filtro();
        imprimir(sessoes.listar(f.de, f.ate, f.mentor, f.disciplina));
        System.out.printf("Total de horas realizadas: %.2f%n", sessoes.totalHoras(f.de, f.ate, f.mentor, f.disciplina));
    }

    private record Filtro(LocalDate de, LocalDate ate, Long mentor, Long disciplina) {
    }

}
