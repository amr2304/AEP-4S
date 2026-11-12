# Reforça

Aplicação de terminal para gestão de mentoria escolar voluntária. Java 17, JDBC, MySQL e camadas MVC. O escopo cobre os requisitos 01–09 da Entrega 1.

## Requisitos
- Java 17+
- Maven 3.9+
- MySQL 8+

## Preparar e executar
1. Inicie o MySQL e execute `database/schema.sql`.
2. Configure `MYSQL_URL`, `MYSQL_USER` e `MYSQL_PASSWORD`. Exemplo PowerShell:

```powershell
$env:MYSQL_URL="jdbc:mysql://localhost:3306/reforca?serverTimezone=America/Sao_Paulo&useSSL=false&allowPublicKeyRetrieval=true"
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="sua_senha"
```

No Linux/macOS use `export` para definir as mesmas variáveis.

3. Na raiz do projeto execute:

```bash
mvn compile exec:java
```

## Funcionalidades
- Cadastro de alunos, mentores, coordenadores e disciplinas.
- Associação de disciplinas e faixas semanais de disponibilidade aos mentores.
- CRUD de planos de estudo.
- Agendar, listar/filtrar, alterar e cancelar sessões. Cancelamento é exclusão lógica e exige motivo para preservar histórico.
- Registrar presença, evolução de 1 a 5 e comentário; sessão passa a REALIZADA.
- Relatório por período, mentor e disciplina, com soma de horas realizadas.

## Regras de domínio
- `Usuario` é abstrata e tem `Aluno`, `Mentor` e `Coordenador`.
- `SessaoMentoria` é abstrata; `SessaoPresencial` e `SessaoOnline` sobrescrevem `obterInstrucoesDeAcesso()`.
- Mentor precisa estar habilitado na disciplina e disponível na faixa informada.
- Conflito do mesmo mentor/data ocorre quando `existente.inicio < solicitado.fim && existente.fim > solicitado.inicio`. A validação ocorre na transação com bloqueio da linha do mentor. Sessões canceladas são ignoradas.
- Plano pode agrupar sessões; se definido, deve pertencer ao mesmo aluno e conter a data da sessão em seu período. Excluir um plano apaga em cascata as sessões vinculadas, conforme a composição 1:N.
- A entidade principal é sessão: Create, Read, Update e Delete (cancelamento lógico com motivo).

## Organização
- `src/main/java`: modelos, persistência JDBC, serviços e interface de terminal.
- `database/schema.sql`: banco e tabelas.
- `docs/classes.md` e `docs/der.md`: diagramas UML e DER.

Datas no terminal: `AAAA-MM-DD`; horas: `HH:MM`. Os IDs são exibidos após cadastro. O README deve ser mantido em sincronia com os requisitos e o cronograma da equipe.

## Cronograma da Entrega 1
| Sprint | Período | Responsável principal |
|---|---|---|
| 1 | 14/09–27/09 | Mateus: núcleo de usuários |
| 2 | 28/09–11/10 | Felipe: catálogo e agenda |
| 3 | 12/10–25/10 | André: regra de conflito |
| 4 | 26/10–08/11 | Mateus e Felipe: acompanhamento |
| 5 | 09/11–22/11 | Felipe e André: consultas e entrega |

Todos devem contribuir com commits próprios no repositório público da equipe.
