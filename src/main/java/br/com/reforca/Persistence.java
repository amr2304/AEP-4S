package br.com.reforca;

import java.sql.*;
import java.time.*;
import java.util.*;

final class Database {
    private Database() {
    }

    static Connection open() throws SQLException {
        String url = System.getenv().getOrDefault("MYSQL_URL", "jdbc:mysql://localhost:3306/reforca?serverTimezone=America/Sao_Paulo&useSSL=false&allowPublicKeyRetrieval=true");
        return DriverManager.getConnection(url, System.getenv().getOrDefault("MYSQL_USER", "root"), System.getenv().getOrDefault("MYSQL_PASSWORD", ""));
    }

}

class UsuarioDAO {
    long aluno(Aluno a) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                long id = base(c, a);
                try (PreparedStatement p = c.prepareStatement("INSERT INTO alunos(usuario_id,data_nascimento,ano_serie,escola_origem,nome_responsavel,telefone_responsavel) VALUES(?,?,?,?,?,?)")) {
                    p.setLong(1, id);
                    p.setDate(2, java.sql.Date.valueOf(a.nascimento));
                    p.setString(3, a.serie);
                    p.setString(4, a.escola);
                    p.setString(5, a.responsavel);
                    p.setString(6, a.telefoneResponsavel);
                    p.executeUpdate();
                }
                c.commit();
                return id;
            }
            catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    long mentor(Mentor m) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                long id = base(c, m);
                try (PreparedStatement p = c.prepareStatement("INSERT INTO mentores(usuario_id,formacao_academica) VALUES(?,?)")) {
                    p.setLong(1, id);
                    p.setString(2, m.formacao);
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO mentor_disciplina(mentor_id,disciplina_id) VALUES(?,?)")) {
                    for (long d : m.disciplinas) {
                        p.setLong(1, id);
                        p.setLong(2, d);
                        p.addBatch();
                    }
                    p.executeBatch();
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO disponibilidades_mentor(mentor_id,dia_semana,horario_inicio,horario_fim) VALUES(?,?,?,?)")) {
                    for (Disponibilidade d : m.disponibilidades) {
                        p.setLong(1, id);
                        p.setInt(2, d.dia().getValue());
                        p.setTime(3, Time.valueOf(d.inicio()));
                        p.setTime(4, Time.valueOf(d.fim()));
                        p.addBatch();
                    }
                    p.executeBatch();
                }
                c.commit();
                return id;
            }
            catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    long coordenador(Coordenador x) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                long id = base(c, x);
                try (PreparedStatement p = c.prepareStatement("INSERT INTO coordenadores(usuario_id,matricula_funcional) VALUES(?,?)")) {
                    p.setLong(1, id);
                    p.setString(2, x.matricula);
                    p.executeUpdate();
                }
                c.commit();
                return id;
            }
            catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private long base(Connection c, Usuario u) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO usuarios(tipo,nome_completo,email,telefone) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, u.tipo());
            p.setString(2, u.nome());
            p.setString(3, u.email());
            p.setString(4, u.telefone());
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                r.next();
                return r.getLong(1);
            }
        }
    }

    boolean habilitado(long mentor, long disciplina) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement("SELECT 1 FROM mentor_disciplina WHERE mentor_id=? AND disciplina_id=?")) {
            p.setLong(1, mentor);
            p.setLong(2, disciplina);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    boolean disponivel(long mentor, DayOfWeek dia, LocalTime ini, LocalTime fim) throws SQLException {
        String q = "SELECT 1 FROM disponibilidades_mentor WHERE mentor_id=? AND dia_semana=? AND horario_inicio<=? AND horario_fim>=? LIMIT 1";
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement(q)) {
            p.setLong(1, mentor);
            p.setInt(2, dia.getValue());
            p.setTime(3, Time.valueOf(ini));
            p.setTime(4, Time.valueOf(fim));
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    List<String> listar() throws SQLException {
        String q = "SELECT u.id,u.tipo,u.nome_completo,u.email,u.telefone,COALESCE(a.ano_serie,m.formacao_academica,c.matricula_funcional,'') detalhe FROM usuarios u LEFT JOIN alunos a ON a.usuario_id=u.id LEFT JOIN mentores m ON m.usuario_id=u.id LEFT JOIN coordenadores c ON c.usuario_id=u.id ORDER BY u.nome_completo";
        List<String> x = new ArrayList<> ();
        try (Connection c = Database.open(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(q)) {
            while (r.next()) x.add("#" + r.getLong(1) + " | " + r.getString(2) + " | " + r.getString(3) + " | " + r.getString(4) + " | " + r.getString(5) + " | " + r.getString(6));
        }
        return x;
    }

    void bloquear(Connection c, long mentor) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT usuario_id FROM mentores WHERE usuario_id=? FOR UPDATE")) {
            p.setLong(1, mentor);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("Mentor não encontrado.");
            }
        }
    }

}

class DisciplinaDAO {
    long criar(Disciplina d) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement("INSERT INTO disciplinas(nome,nivel_ensino,descricao) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, d.nome());
            p.setString(2, d.nivel());
            p.setString(3, d.descricao());
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                r.next();
                return r.getLong(1);
            }
        }
    }

    List<Disciplina> listar() throws SQLException {
        List<Disciplina> x = new ArrayList<> ();
        try (Connection c = Database.open(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT id,nome,nivel_ensino,descricao FROM disciplinas ORDER BY nome")) {
            while (r.next()) x.add(new Disciplina(r.getLong(1), r.getString(2), r.getString(3), r.getString(4)));
        }
        return x;
    }

}

class PlanoDAO {
    long criar(PlanoDeEstudo x) throws SQLException {
        String q = "INSERT INTO planos_estudo(aluno_id,titulo,objetivo,data_inicio,data_fim) VALUES(?,?,?,?,?)";
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            p.setLong(1, x.alunoId());
            p.setString(2, x.titulo());
            p.setString(3, x.objetivo());
            p.setDate(4, java.sql.Date.valueOf(x.inicio()));
            p.setDate(5, java.sql.Date.valueOf(x.fim()));
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                r.next();
                return r.getLong(1);
            }
        }
    }

    boolean pertence(long plano, long aluno, LocalDate data) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement("SELECT 1 FROM planos_estudo WHERE id=? AND aluno_id=? AND data_inicio<=? AND data_fim>=?")) {
            p.setLong(1, plano);
            p.setLong(2, aluno);
            p.setDate(3, java.sql.Date.valueOf(data));
            p.setDate(4, java.sql.Date.valueOf(data));
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    List<String> listar() throws SQLException {
        List<String> x = new ArrayList<> ();
        String q = "SELECT p.id,p.aluno_id,u.nome_completo,p.titulo,p.objetivo,p.data_inicio,p.data_fim,COUNT(s.id) FROM planos_estudo p JOIN usuarios u ON u.id=p.aluno_id LEFT JOIN sessoes s ON s.plano_id=p.id GROUP BY p.id ORDER BY p.data_inicio";
        try (Connection c = Database.open(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(q)) {
            while (r.next()) x.add("#" + r.getLong(1) + " | Aluno #" + r.getLong(2) + " " + r.getString(3) + " | " + r.getString(4) + " | " + r.getString(5) + " | " + r.getDate(6) + " a " + r.getDate(7) + " | " + r.getInt(8) + " sessão(ões)");
        }
        return x;
    }

    void atualizar(long id, String titulo, String objetivo, LocalDate ini, LocalDate fim) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement("UPDATE planos_estudo SET titulo=?,objetivo=?,data_inicio=?,data_fim=? WHERE id=?")) {
            p.setString(1, titulo);
            p.setString(2, objetivo);
            p.setDate(3, java.sql.Date.valueOf(ini));
            p.setDate(4, java.sql.Date.valueOf(fim));
            p.setLong(5, id);
            if (p.executeUpdate() == 0) throw new SQLException("Plano não encontrado.");
        }
    }

    void excluir(long id) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement("DELETE FROM planos_estudo WHERE id=?")) {
            p.setLong(1, id);
            if (p.executeUpdate() == 0) throw new SQLException("Plano não encontrado.");
        }
    }

}

class SessaoDAO {
    private static final String ONE = "SELECT id,aluno_id,mentor_id,disciplina_id,plano_id,data_sessao,horario_inicio,horario_fim,modalidade,status,sala,endereco,plataforma,link_acesso FROM sessoes ";
    long criar(Connection c, SessaoMentoria x) throws SQLException {
        String q = "INSERT INTO sessoes(aluno_id,mentor_id,disciplina_id,plano_id,data_sessao,horario_inicio,horario_fim,modalidade,sala,endereco,plataforma,link_acesso) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement p = c.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            bind(p, x);
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                r.next();
                return r.getLong(1);
            }
        }
    }

    void atualizar(Connection c, SessaoMentoria x) throws SQLException {
        String q = "UPDATE sessoes SET aluno_id=?,mentor_id=?,disciplina_id=?,plano_id=?,data_sessao=?,horario_inicio=?,horario_fim=?,modalidade=?,sala=?,endereco=?,plataforma=?,link_acesso=? WHERE id=? AND status='AGENDADA'";
        try (PreparedStatement p = c.prepareStatement(q)) {
            bind(p, x);
            p.setLong(13, x.id);
            if (p.executeUpdate() == 0) throw new SQLException("Sessão não encontrada ou encerrada.");
        }
    }

    private void bind(PreparedStatement p, SessaoMentoria x) throws SQLException {
        p.setLong(1, x.alunoId);
        p.setLong(2, x.mentorId);
        p.setLong(3, x.disciplinaId);
        if (x.planoId == null) p.setNull(4, Types.BIGINT);
        else p.setLong(4, x.planoId);
        p.setDate(5, java.sql.Date.valueOf(x.data));
        p.setTime(6, Time.valueOf(x.inicio));
        p.setTime(7, Time.valueOf(x.fim));
        p.setString(8, x.modalidade());
        p.setString(9, x instanceof SessaoPresencial?x.local():null);
        p.setString(10, x instanceof SessaoPresencial?x.acesso():null);
        p.setString(11, x instanceof SessaoOnline?x.local():null);
        p.setString(12, x instanceof SessaoOnline?x.acesso():null);
    }

    boolean conflito(Connection c, long mentor, LocalDate data, LocalTime ini, LocalTime fim, Long ignorar) throws SQLException {
        String q = "SELECT 1 FROM sessoes WHERE mentor_id=? AND data_sessao=? AND status<>'CANCELADA' AND horario_inicio<? AND horario_fim>?" + (ignorar == null?"":" AND id<>?") + " LIMIT 1";
        try (PreparedStatement p = c.prepareStatement(q)) {
            p.setLong(1, mentor);
            p.setDate(2, java.sql.Date.valueOf(data));
            p.setTime(3, Time.valueOf(fim));
            p.setTime(4, Time.valueOf(ini));
            if (ignorar != null) p.setLong(5, ignorar);
            try (ResultSet r = p.executeQuery()) {
                return r.next();
            }
        }
    }

    Optional<SessaoMentoria> buscar(long id) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement(ONE + "WHERE id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                return r.next()?Optional.of(map(r)):Optional.empty();
            }
        }
    }

    private SessaoMentoria map(ResultSet r) throws SQLException {
        long planoN = r.getLong(5);
        Long plano = r.wasNull()?null:planoN;
        long id = r.getLong(1), a = r.getLong(2), m = r.getLong(3), d = r.getLong(4);
        LocalDate data = r.getDate(6).toLocalDate();
        LocalTime ini = r.getTime(7).toLocalTime(), fim = r.getTime(8).toLocalTime();
        String mod = r.getString(9), st = r.getString(10);
        if (mod.equals("PRESENCIAL")) return new SessaoPresencial(id, a, m, d, plano, data, ini, fim, st, r.getString(11), r.getString(12));
        return new SessaoOnline(id, a, m, d, plano, data, ini, fim, st, r.getString(13), r.getString(14));
    }

    List<SessaoResumo> listar(LocalDate de, LocalDate ate, Long mentor, Long disciplina) throws SQLException {
        StringBuilder q = new StringBuilder("SELECT s.id,s.data_sessao,s.horario_inicio,s.horario_fim,s.modalidade,s.status,a.nome_completo,m.nome_completo,d.nome,CASE WHEN s.modalidade='PRESENCIAL' THEN CONCAT('Sala: ',s.sala,' | ',s.endereco) ELSE CONCAT('Plataforma: ',s.plataforma,' | ',s.link_acesso) END,s.motivo_cancelamento,r.presenca,r.nota_evolucao,r.comentario FROM sessoes s JOIN usuarios a ON a.id=s.aluno_id JOIN usuarios m ON m.id=s.mentor_id JOIN disciplinas d ON d.id=s.disciplina_id LEFT JOIN registros_sessao r ON r.sessao_id=s.id WHERE 1=1");
        List<Object> ps = new ArrayList<> ();
        if (de != null) {
            q.append(" AND s.data_sessao>=?");
            ps.add(java.sql.Date.valueOf(de));
        }
        if (ate != null) {
            q.append(" AND s.data_sessao<=?");
            ps.add(java.sql.Date.valueOf(ate));
        }
        if (mentor != null) {
            q.append(" AND s.mentor_id=?");
            ps.add(mentor);
        }
        if (disciplina != null) {
            q.append(" AND s.disciplina_id=?");
            ps.add(disciplina);
        }
        q.append(" ORDER BY s.data_sessao,s.horario_inicio");
        List<SessaoResumo> out = new ArrayList<> ();
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement(q.toString())) {
            for (int i = 0;i < ps.size();i ++) p.setObject(i + 1, ps.get(i));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    int n = r.getInt(13);
                    Integer nota = r.wasNull()?null:n;
                    out.add(new SessaoResumo(r.getLong(1), r.getDate(2).toLocalDate(), r.getTime(3).toLocalTime(), r.getTime(4).toLocalTime(), r.getString(5), r.getString(6), r.getString(7), r.getString(8), r.getString(9), r.getString(10), r.getString(11), r.getString(12), nota, r.getString(14)));
                }
            }
        }
        return out;
    }

    void cancelar(long id, String motivo) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement("UPDATE sessoes SET status='CANCELADA',motivo_cancelamento=? WHERE id=? AND status='AGENDADA'")) {
            p.setString(1, motivo);
            p.setLong(2, id);
            if (p.executeUpdate() == 0) throw new SQLException("Sessão não encontrada ou não está agendada.");
        }
    }

    void registrar(RegistroSessao x) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement lock = c.prepareStatement("SELECT status FROM sessoes WHERE id=? FOR UPDATE")) {
                    lock.setLong(1, x.sessaoId());
                    try (ResultSet r = lock.executeQuery()) {
                        if (!r.next()) throw new SQLException("Sessão não encontrada.");
                        if (r.getString(1).equals("CANCELADA")) throw new SQLException("Sessão cancelada não recebe resultado.");
                    }
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO registros_sessao(sessao_id,presenca,nota_evolucao,comentario) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE presenca=VALUES(presenca),nota_evolucao=VALUES(nota_evolucao),comentario=VALUES(comentario),registrado_em=CURRENT_TIMESTAMP")) {
                    p.setLong(1, x.sessaoId());
                    p.setString(2, x.presente()?"PRESENTE":"AUSENTE");
                    p.setInt(3, x.nota());
                    p.setString(4, x.comentario());
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE sessoes SET status='REALIZADA' WHERE id=?")) {
                    p.setLong(1, x.sessaoId());
                    p.executeUpdate();
                }
                c.commit();
            }
            catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    double totalHoras(LocalDate de, LocalDate ate, Long mentor, Long disciplina) throws SQLException {
        StringBuilder q = new StringBuilder("SELECT COALESCE(SUM(TIMESTAMPDIFF(MINUTE,horario_inicio,horario_fim))/60.0,0) FROM sessoes WHERE status='REALIZADA'");
        List<Object> ps = new ArrayList<> ();
        if (de != null) {
            q.append(" AND data_sessao>=?");
            ps.add(java.sql.Date.valueOf(de));
        }
        if (ate != null) {
            q.append(" AND data_sessao<=?");
            ps.add(java.sql.Date.valueOf(ate));
        }
        if (mentor != null) {
            q.append(" AND mentor_id=?");
            ps.add(mentor);
        }
        if (disciplina != null) {
            q.append(" AND disciplina_id=?");
            ps.add(disciplina);
        }
        try (Connection c = Database.open(); PreparedStatement p = c.prepareStatement(q.toString())) {
            for (int i = 0;i < ps.size();i ++) p.setObject(i + 1, ps.get(i));
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getDouble(1);
            }
        }
    }

}
