-- Dados ficticios para testar o Reforca.
-- Pode executar este arquivo mais de uma vez sem duplicar os dados de exemplo.
USE reforca;

INSERT IGNORE INTO usuarios (tipo, nome_completo, email, telefone) VALUES
 ('ALUNO', 'Ana Souza (teste)', 'ana.teste@reforca.local', '11999990001'),
 ('MENTOR', 'Carlos Lima (teste)', 'carlos.teste@reforca.local', '11999990002'),
 ('COORDENADOR', 'Marina Costa (teste)', 'marina.teste@reforca.local', '11999990003');

SET @aluno = (SELECT id FROM usuarios WHERE email = 'ana.teste@reforca.local');
SET @mentor = (SELECT id FROM usuarios WHERE email = 'carlos.teste@reforca.local');
SET @coordenador = (SELECT id FROM usuarios WHERE email = 'marina.teste@reforca.local');

INSERT IGNORE INTO alunos (usuario_id, data_nascimento, ano_serie, escola_origem, nome_responsavel, telefone_responsavel)
VALUES (@aluno, '2010-05-14', '9 ano', 'Escola Exemplo', 'Paula Souza', '11988880001');
INSERT IGNORE INTO mentores (usuario_id, formacao_academica) VALUES (@mentor, 'Licenciatura em Matematica');
INSERT IGNORE INTO coordenadores (usuario_id, matricula_funcional) VALUES (@coordenador, 'TESTE-001');

INSERT INTO disciplinas (nome, nivel_ensino, descricao)
VALUES ('Matematica', 'Ensino Fundamental II', 'Reforco de matematica para testes')
ON DUPLICATE KEY UPDATE descricao = VALUES(descricao);
SET @disciplina = (SELECT id FROM disciplinas WHERE nome = 'Matematica' AND nivel_ensino = 'Ensino Fundamental II');

INSERT IGNORE INTO mentor_disciplina (mentor_id, disciplina_id) VALUES (@mentor, @disciplina);

-- Disponibilidade de segunda a sexta, das 8h as 18h.
INSERT INTO disponibilidades_mentor (mentor_id, dia_semana, horario_inicio, horario_fim)
SELECT @mentor, dias.dia, '08:00:00', '18:00:00'
FROM (SELECT 1 AS dia UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) AS dias
WHERE NOT EXISTS (
  SELECT 1 FROM disponibilidades_mentor d
  WHERE d.mentor_id = @mentor AND d.dia_semana = dias.dia
    AND d.horario_inicio = '08:00:00' AND d.horario_fim = '18:00:00'
);

INSERT INTO planos_estudo (aluno_id, titulo, objetivo, data_inicio, data_fim)
SELECT @aluno, 'Plano de reforco - teste', 'Praticar matematica e acompanhar a evolucao.', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY)
WHERE NOT EXISTS (
  SELECT 1 FROM planos_estudo WHERE aluno_id = @aluno AND titulo = 'Plano de reforco - teste'
);
SET @plano = (SELECT id FROM planos_estudo WHERE aluno_id = @aluno AND titulo = 'Plano de reforco - teste' ORDER BY id DESC LIMIT 1);

-- Cria uma sessao de teste na proxima segunda-feira, as 14h.
SET @data_sessao = DATE_ADD(CURDATE(), INTERVAL (7 - WEEKDAY(CURDATE())) DAY);
INSERT INTO sessoes (aluno_id, mentor_id, disciplina_id, plano_id, data_sessao, horario_inicio, horario_fim, modalidade, status, sala, endereco)
SELECT @aluno, @mentor, @disciplina, @plano, @data_sessao, '14:00:00', '15:00:00', 'PRESENCIAL', 'AGENDADA', 'Sala 2', 'Rua da Escola, 100'
WHERE NOT EXISTS (
  SELECT 1 FROM sessoes
  WHERE aluno_id = @aluno AND mentor_id = @mentor AND disciplina_id = @disciplina
    AND data_sessao = @data_sessao AND horario_inicio = '14:00:00'
);

SELECT 'Dados de teste carregados. Veja as IDs abaixo:' AS resultado;
SELECT 'ALUNO' AS tipo, id, nome_completo FROM usuarios WHERE email = 'ana.teste@reforca.local'
UNION ALL SELECT 'MENTOR', id, nome_completo FROM usuarios WHERE email = 'carlos.teste@reforca.local'
UNION ALL SELECT 'COORDENADOR', id, nome_completo FROM usuarios WHERE email = 'marina.teste@reforca.local';
SELECT id AS disciplina_id, nome FROM disciplinas WHERE id = @disciplina;
SELECT id AS plano_id, titulo FROM planos_estudo WHERE id = @plano;
SELECT id AS sessao_id, data_sessao, horario_inicio FROM sessoes
WHERE aluno_id = @aluno AND mentor_id = @mentor AND data_sessao = @data_sessao AND horario_inicio = '14:00:00';