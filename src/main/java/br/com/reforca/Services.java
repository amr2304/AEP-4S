package br.com.reforca;

import java.sql.*;
import java.time.*;
import java.util.*;

class RegraDeNegocioException extends RuntimeException {
    RegraDeNegocioException(String m) {
        super(m);
    }

}

class UsuarioService {
    private final UsuarioDAO dao;
    UsuarioService(UsuarioDAO d) {
        dao = d;
    }

    long aluno(Aluno a) throws SQLException {
        required(a.nome(), "Nome");
        required(a.email(), "E-mail");
        required(a.telefone(), "Telefone");
        required(a.serie, "Ano/série");
        required(a.escola, "Escola");
        required(a.responsavel, "Responsável");
        required(a.telefoneResponsavel, "Telefone do responsável");
        if (a.nascimento == null || a.nascimento.isAfter(LocalDate.now())) throw new RegraDeNegocioException("Data de nascimento inválida.");
        return dao.aluno(a);
    }

    long mentor(Mentor m) throws SQLException {
        required(m.nome(), "Nome");
        required(m.email(), "E-mail");
        required(m.telefone(), "Telefone");
        required(m.formacao, "Formação");
        if (m.disciplinas.isEmpty() || m.disponibilidades.isEmpty()) throw new RegraDeNegocioException("Mentor precisa de ao menos uma disciplina e disponibilidade.");
        return dao.mentor(m);
    }

    long coordenador(Coordenador x) throws SQLException {
        required(x.nome(), "Nome");
        required(x.email(), "E-mail");
        required(x.telefone(), "Telefone");
        required(x.matricula, "Matrícula");
        return dao.coordenador(x);
    }

    List<String> listar() throws SQLException {
        return dao.listar();
    }

    static void required(String s, String nome) {
        if (s == null || s.isBlank()) throw new RegraDeNegocioException(nome + " é obrigatório.");
    }

}

class DisciplinaService {
    private final DisciplinaDAO dao;
    DisciplinaService(DisciplinaDAO d) {
        dao = d;
    }

    long criar(Disciplina d) throws SQLException {
        UsuarioService.required(d.nome(), "Nome da disciplina");
        UsuarioService.required(d.nivel(), "Nível de ensino");
        UsuarioService.required(d.descricao(), "Descrição");
        return dao.criar(d);
    }

    List<Disciplina> listar() throws SQLException {
        return dao.listar();
    }

}

class PlanoService {
    private final PlanoDAO dao;
    PlanoService(PlanoDAO d) {
        dao = d;
    }

    long criar(PlanoDeEstudo p) throws SQLException {
        UsuarioService.required(p.titulo(), "Título");
        UsuarioService.required(p.objetivo(), "Objetivo");
        if (p.inicio() == null || p.fim() == null || p.inicio().isAfter(p.fim())) throw new RegraDeNegocioException("Período do plano inválido.");
        return dao.criar(p);
    }

    void atualizar(long id, String titulo, String objetivo, LocalDate ini, LocalDate fim) throws SQLException {
        UsuarioService.required(titulo, "Título");
        UsuarioService.required(objetivo, "Objetivo");
        if (ini.isAfter(fim)) throw new RegraDeNegocioException("Período do plano inválido.");
        dao.atualizar(id, titulo, objetivo, ini, fim);
    }

    List<String> listar() throws SQLException {
        return dao.listar();
    }

    void excluir(long id) throws SQLException {
        dao.excluir(id);
    }

}

class SessaoService {
    private final SessaoDAO dao;
    private final UsuarioDAO usuarios;
    private final PlanoDAO planos;
    SessaoService(SessaoDAO s, UsuarioDAO u, PlanoDAO p) {
        dao = s;
        usuarios = u;
        planos = p;
    }

    private void validar(SessaoMentoria s) throws SQLException {
        if (s.data == null || s.inicio == null || s.fim == null || !s.inicio.isBefore(s.fim)) throw new RegraDeNegocioException("Data ou intervalo de horário inválido.");
        if (!usuarios.habilitado(s.mentorId, s.disciplinaId)) throw new RegraDeNegocioException("O mentor não está habilitado para essa disciplina.");
        if (!usuarios.disponivel(s.mentorId, s.data.getDayOfWeek(), s.inicio, s.fim)) throw new RegraDeNegocioException("Horário fora da disponibilidade semanal do mentor.");
        if (s.planoId != null && !planos.pertence(s.planoId, s.alunoId, s.data)) throw new RegraDeNegocioException("O plano não pertence ao aluno ou a sessão está fora do período do plano.");
        UsuarioService.required(s.local(), s.modalidade().equals("ONLINE")?"Plataforma":"Sala");
        UsuarioService.required(s.acesso(), s.modalidade().equals("ONLINE")?"Link":"Endereço");
    }

    long criar(SessaoMentoria s) throws SQLException {
        validar(s);
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                usuarios.bloquear(c, s.mentorId);
                if (dao.conflito(c, s.mentorId, s.data, s.inicio, s.fim, null)) throw new RegraDeNegocioException("Indisponibilidade: o mentor já tem sessão em horário conflitante.");
                long id = dao.criar(c, s);
                c.commit();
                return id;
            }
            catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException x) throw x;
                if (e instanceof RuntimeException x) throw x;
                throw new SQLException(e);
            }
        }
    }

    void atualizar(SessaoMentoria s) throws SQLException {
        validar(s);
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                usuarios.bloquear(c, s.mentorId);
                if (dao.conflito(c, s.mentorId, s.data, s.inicio, s.fim, s.id)) throw new RegraDeNegocioException("Indisponibilidade: o mentor já tem sessão em horário conflitante.");
                dao.atualizar(c, s);
                c.commit();
            }
            catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException x) throw x;
                if (e instanceof RuntimeException x) throw x;
                throw new SQLException(e);
            }
        }
    }

    Optional<SessaoMentoria> buscar(long id) throws SQLException {
        return dao.buscar(id);
    }

    List<SessaoResumo> listar(LocalDate de, LocalDate ate, Long mentor, Long disciplina) throws SQLException {
        return dao.listar(de, ate, mentor, disciplina);
    }

    void cancelar(long id, String motivo) throws SQLException {
        UsuarioService.required(motivo, "Motivo do cancelamento");
        dao.cancelar(id, motivo.trim());
    }

    void registrar(RegistroSessao r) throws SQLException {
        UsuarioService.required(r.comentario(), "Comentário");
        dao.registrar(r);
    }

    double totalHoras(LocalDate de, LocalDate ate, Long mentor, Long disciplina) throws SQLException {
        return dao.totalHoras(de, ate, mentor, disciplina);
    }

}
