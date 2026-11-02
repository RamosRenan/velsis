# Autenticação

A autenticação usa sessão HTTP (`JSESSIONID`). O Spring Security consulta a tabela
`"user"` pelo campo `usuario` e compara a senha recebida com o hash BCrypt de `senha`.
As rotas `/api/public/**` permitem acesso anônimo; as demais exigem autenticação.

## Uso

1. Faça `GET /api/public/csrf` e preserve o cookie de sessão. A resposta contém
   `headerName` e `token`.
2. Faça `POST /api/public/login` com `Content-Type: application/x-www-form-urlencoded`,
   campos `usuario` e `senha`, cookie de sessão e o cabeçalho CSRF retornado.
   Sucesso retorna `204`; credenciais inválidas retornam `401`.
3. Use o cookie `JSESSIONID` recebido no login para acessar as rotas protegidas.
4. Obtenha um novo token em `GET /api/public/csrf` após o login e envie-o nas
   operações de escrita, incluindo `POST /api/logout`. O logout retorna `204`,
   invalida a sessão e remove o cookie.

A proteção CSRF também se aplica às operações de escrita públicas. A ausência
ou invalidade do token retorna `403`. Requisições GET protegidas sem sessão
autenticada retornam `401`.

## Banco de dados

O Liquibase cria a tabela `"user"` na inicialização, com `usuario` como chave
primária, `senha` com 60 caracteres e `created_at` preenchido pelo banco.
O nome da tabela é delimitado por aspas porque `user` é reservado no PostgreSQL.

Para provisionar um usuário, gere o hash com o bean `PasswordEncoder.encode(senha)`
e insira `usuario` e esse hash em `"user"`. Nunca armazene a senha em texto puro.
O Liquibase provisiona o usuário inicial `admin` com senha `admin`, armazenada como
hash BCrypt. Troque essa senha antes de disponibilizar a aplicação em produção.
Se `admin` já existir, a migração preserva o registro e sua senha. `created_at`
recebe o valor padrão do banco. Não há endpoint público de cadastro.
O nome `admin` não concede privilégios especiais: a configuração atual atribui
`ROLE_USER` a todos os usuários autenticados.

Os testes `AuthenticationTests` verificam o fluxo HTTP com o repositório simulado;
a migração e a integração real com PostgreSQL devem ser verificadas com o banco
configurado em `application.yml` e as variáveis `POSTGRES_USER` e `POSTGRES_PASSWORD`.
