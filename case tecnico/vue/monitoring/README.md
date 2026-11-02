# INSTRUÇÕES

1. Configure a url de comunicação com o servidor de aplicação, arquivo [.env.local]. Se back e front executarem no mesmo servidor, matenha o default[localhost]

```env
VITE_API_URL=http://localhost:8080
```

Porta padrão da app back-end[8080].

O Docker executa o build e incorpora `VITE_API_URL` ao JavaScript. Ao alterar
`.env.local`, reconstrua a imagem e recrie o container. Passar a variavel apenas
no `docker run` nao altera o frontend compilado.
