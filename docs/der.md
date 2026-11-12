# Diagrama de entidade e relacionamento

```mermaid
erDiagram
 USUARIOS ||--o| ALUNOS : perfil
 USUARIOS ||--o| MENTORES : perfil
 USUARIOS ||--o| COORDENADORES : perfil
 MENTORES ||--o{ DISPONIBILIDADES_MENTOR : informa
 MENTORES ||--o{ MENTOR_DISCIPLINA : habilita
 DISCIPLINAS ||--o{ MENTOR_DISCIPLINA : atende
 ALUNOS ||--o{ PLANOS_ESTUDO : possui
 ALUNOS ||--o{ SESSOES : participa
 MENTORES ||--o{ SESSOES : conduz
 DISCIPLINAS ||--o{ SESSOES : classifica
 PLANOS_ESTUDO o|--o{ SESSOES : organiza
 SESSOES ||--o| REGISTROS_SESSAO : registra
```

O SQL completo está em `database/schema.sql`. A regra de conflito compara intervalos na camada de serviço e bloqueia a linha do mentor durante a transação. Uma chave única em mentor, data e hora inicial não detectaria sobreposições com durações diferentes.
