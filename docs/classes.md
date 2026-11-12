# Diagrama de classes

```mermaid
classDiagram
 class Usuario {<<abstract>> +id +nomeCompleto +email +telefone +getTipo() }
 class Aluno
 class Mentor
 class Coordenador
 Usuario <|-- Aluno
 Usuario <|-- Mentor
 Usuario <|-- Coordenador
 class SessaoMentoria {<<abstract>> +obterInstrucoesDeAcesso()* }
 class SessaoPresencial { +sala +endereco +obterInstrucoesDeAcesso() }
 class SessaoOnline { +plataforma +link +obterInstrucoesDeAcesso() }
 SessaoMentoria <|-- SessaoPresencial
 SessaoMentoria <|-- SessaoOnline
 class PlanoDeEstudo
 PlanoDeEstudo "1" *-- "0..*" SessaoMentoria
 Mentor "0..*" -- "0..*" Disciplina
 Aluno "1" -- "0..*" SessaoMentoria
 Mentor "1" -- "0..*" SessaoMentoria
 Disciplina "1" -- "0..*" SessaoMentoria
 SessaoMentoria "1" -- "0..1" RegistroSessao
```

`Usuario` e `SessaoMentoria` são classes abstratas. A segunda declara `obterInstrucoesDeAcesso`, implementado de forma diferente pelas subclasses presenciais e online. Uma sessão pode ficar sem plano; quando associada, integra a composição 1:N e é removida se o plano for excluído.
