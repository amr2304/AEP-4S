CREATE DATABASE IF NOT EXISTS reforca CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE reforca;

CREATE TABLE IF NOT EXISTS usuarios (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 tipo ENUM('ALUNO','MENTOR','COORDENADOR') NOT NULL,
 nome_completo VARCHAR(160) NOT NULL,
 email VARCHAR(190) NOT NULL UNIQUE,
 telefone VARCHAR(30) NOT NULL,
 criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS alunos (
 usuario_id BIGINT PRIMARY KEY,
 data_nascimento DATE NOT NULL,
 ano_serie VARCHAR(40) NOT NULL,
 escola_origem VARCHAR(160) NOT NULL,
 nome_responsavel VARCHAR(160) NOT NULL,
 telefone_responsavel VARCHAR(30) NOT NULL,
 FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS mentores (
 usuario_id BIGINT PRIMARY KEY,
 formacao_academica VARCHAR(240) NOT NULL,
 FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS coordenadores (
 usuario_id BIGINT PRIMARY KEY,
 matricula_funcional VARCHAR(60) NOT NULL UNIQUE,
 FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS disponibilidades_mentor (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 mentor_id BIGINT NOT NULL,
 dia_semana TINYINT NOT NULL COMMENT '1=segunda ... 7=domingo',
 horario_inicio TIME NOT NULL,
 horario_fim TIME NOT NULL,
 FOREIGN KEY (mentor_id) REFERENCES mentores(usuario_id) ON DELETE CASCADE,
 CHECK (dia_semana BETWEEN 1 AND 7), CHECK (horario_inicio < horario_fim),
 INDEX ix_disp_mentor_dia (mentor_id,dia_semana)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS disciplinas (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 nome VARCHAR(120) NOT NULL,
 nivel_ensino VARCHAR(80) NOT NULL,
 descricao VARCHAR(500) NOT NULL,
 UNIQUE KEY uq_disciplina_nivel (nome,nivel_ensino)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS mentor_disciplina (
 mentor_id BIGINT NOT NULL, disciplina_id BIGINT NOT NULL,
 PRIMARY KEY (mentor_id,disciplina_id),
 FOREIGN KEY (mentor_id) REFERENCES mentores(usuario_id) ON DELETE CASCADE,
 FOREIGN KEY (disciplina_id) REFERENCES disciplinas(id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS planos_estudo (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 aluno_id BIGINT NOT NULL,
 titulo VARCHAR(160) NOT NULL,
 objetivo VARCHAR(700) NOT NULL,
 data_inicio DATE NOT NULL,
 data_fim DATE NOT NULL,
 criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (aluno_id) REFERENCES alunos(usuario_id) ON DELETE RESTRICT,
 CHECK (data_inicio <= data_fim), INDEX ix_plano_aluno(aluno_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS sessoes (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 aluno_id BIGINT NOT NULL, mentor_id BIGINT NOT NULL, disciplina_id BIGINT NOT NULL,
 plano_id BIGINT NULL,
 data_sessao DATE NOT NULL, horario_inicio TIME NOT NULL, horario_fim TIME NOT NULL,
 modalidade ENUM('PRESENCIAL','ONLINE') NOT NULL,
 status ENUM('AGENDADA','REALIZADA','CANCELADA') NOT NULL DEFAULT 'AGENDADA',
 sala VARCHAR(120) NULL, endereco VARCHAR(300) NULL,
 plataforma VARCHAR(120) NULL, link_acesso VARCHAR(500) NULL,
 motivo_cancelamento VARCHAR(700) NULL,
 criada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 atualizada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 FOREIGN KEY (aluno_id) REFERENCES alunos(usuario_id) ON DELETE RESTRICT,
 FOREIGN KEY (mentor_id) REFERENCES mentores(usuario_id) ON DELETE RESTRICT,
 FOREIGN KEY (disciplina_id) REFERENCES disciplinas(id) ON DELETE RESTRICT,
 FOREIGN KEY (plano_id) REFERENCES planos_estudo(id) ON DELETE CASCADE,
 CHECK (horario_inicio < horario_fim),
 CHECK ((modalidade='PRESENCIAL' AND sala IS NOT NULL AND endereco IS NOT NULL AND plataforma IS NULL AND link_acesso IS NULL) OR (modalidade='ONLINE' AND plataforma IS NOT NULL AND link_acesso IS NOT NULL AND sala IS NULL AND endereco IS NULL)),
 CHECK (status<>'CANCELADA' OR motivo_cancelamento IS NOT NULL),
 INDEX ix_sessao_conflito(mentor_id,data_sessao,horario_inicio,horario_fim,status),
 INDEX ix_sessao_filtros(data_sessao,mentor_id,disciplina_id,status)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS registros_sessao (
 sessao_id BIGINT PRIMARY KEY,
 presenca ENUM('PRESENTE','AUSENTE') NOT NULL,
 nota_evolucao TINYINT NOT NULL,
 comentario VARCHAR(1000) NOT NULL,
 registrado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (sessao_id) REFERENCES sessoes(id) ON DELETE CASCADE,
 CHECK (nota_evolucao BETWEEN 1 AND 5)
) ENGINE=InnoDB;
