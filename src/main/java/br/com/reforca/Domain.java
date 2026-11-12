package br.com.reforca;

import java.time.*;
import java.util.List;

abstract class Usuario {
    private final long id;
    private final String nome, email, telefone;
    Usuario(long id, String nome, String email, String telefone) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    long id() {
        return id;
    }

    String nome() {
        return nome;
    }

    String email() {
        return email;
    }

    String telefone() {
        return telefone;
    }

    abstract String tipo();
}

final class Aluno extends Usuario {
    final LocalDate nascimento;
    final String serie, escola, responsavel, telefoneResponsavel;
    Aluno(long id, String nome, String email, String telefone, LocalDate nascimento, String serie, String escola, String resp, String telResp) {
        super(id, nome, email, telefone);
        this.nascimento = nascimento;
        this.serie = serie;
        this.escola = escola;
        this.responsavel = resp;
        this.telefoneResponsavel = telResp;
    }

    @Override
    String tipo() {
        return "ALUNO";
    }

}

final class Mentor extends Usuario {
    final String formacao;
    final List<Long> disciplinas;
    final List<Disponibilidade> disponibilidades;
    Mentor(long id, String nome, String email, String telefone, String formacao, List<Long> disciplinas, List<Disponibilidade> disp) {
        super(id, nome, email, telefone);
        this.formacao = formacao;
        this.disciplinas = List.copyOf(disciplinas);
        this.disponibilidades = List.copyOf(disp);
    }

    @Override
    String tipo() {
        return "MENTOR";
    }

}

final class Coordenador extends Usuario {
    final String matricula;
    Coordenador(long id, String nome, String email, String telefone, String matricula) {
        super(id, nome, email, telefone);
        this.matricula = matricula;
    }

    @Override
    String tipo() {
        return "COORDENADOR";
    }

}

record Disponibilidade(DayOfWeek dia, LocalTime inicio, LocalTime fim) {
    Disponibilidade {
        if (dia == null || inicio == null || fim == null || !inicio.isBefore(fim)) throw new IllegalArgumentException("Faixa de disponibilidade inválida.");
    }

}

record Disciplina(long id, String nome, String nivel, String descricao) {
}

record PlanoDeEstudo(long id, long alunoId, String titulo, String objetivo, LocalDate inicio, LocalDate fim) {
}

record RegistroSessao(long sessaoId, boolean presente, int nota, String comentario) {
    RegistroSessao {
        if (nota < 1 || nota > 5) throw new IllegalArgumentException("A evolução deve ser de 1 a 5.");
    }

}

abstract class SessaoMentoria {
    final long id, alunoId, mentorId, disciplinaId;
    final Long planoId;
    final LocalDate data;
    final LocalTime inicio, fim;
    final String status;
    SessaoMentoria(long id, long aluno, long mentor, long disciplina, Long plano, LocalDate data, LocalTime inicio, LocalTime fim, String status) {
        this.id = id;
        alunoId = aluno;
        mentorId = mentor;
        disciplinaId = disciplina;
        planoId = plano;
        this.data = data;
        this.inicio = inicio;
        this.fim = fim;
        this.status = status;
    }

    abstract String modalidade();
    abstract String obterInstrucoesDeAcesso();
    abstract String local();
    abstract String acesso();
}

final class SessaoPresencial extends SessaoMentoria {
    final String sala, endereco;
    SessaoPresencial(long id, long aluno, long mentor, long disciplina, Long plano, LocalDate data, LocalTime inicio, LocalTime fim, String status, String sala, String endereco) {
        super(id, aluno, mentor, disciplina, plano, data, inicio, fim, status);
        this.sala = sala;
        this.endereco = endereco;
    }

    @Override
    String modalidade() {
        return "PRESENCIAL";
    }

    @Override
    String obterInstrucoesDeAcesso() {
        return "Sala: " + sala + " | Endereço: " + endereco;
    }

    @Override
    String local() {
        return sala;
    }

    @Override
    String acesso() {
        return endereco;
    }

}

final class SessaoOnline extends SessaoMentoria {
    final String plataforma, link;
    SessaoOnline(long id, long aluno, long mentor, long disciplina, Long plano, LocalDate data, LocalTime inicio, LocalTime fim, String status, String plataforma, String link) {
        super(id, aluno, mentor, disciplina, plano, data, inicio, fim, status);
        this.plataforma = plataforma;
        this.link = link;
    }

    @Override
    String modalidade() {
        return "ONLINE";
    }

    @Override
    String obterInstrucoesDeAcesso() {
        return "Plataforma: " + plataforma + " | Link: " + link;
    }

    @Override
    String local() {
        return plataforma;
    }

    @Override
    String acesso() {
        return link;
    }

}

record SessaoResumo(long id, LocalDate data, LocalTime inicio, LocalTime fim, String modalidade, String status, String aluno, String mentor, String disciplina, String instrucoes, String motivoCancelamento, String presenca, Integer nota, String comentario) {
}
