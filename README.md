# Instalação e instruções.

Faça o download dos artefatos:

> velsis/case tecnico/compose/composetar.tar

Envie o arquivo composetar.tar para o servidor.

Descompacte o arquivo com o comando:

```sh
tar -xvf composetar.tar
```

# Habilite o cors
No arquivo .env habilite os endereço de ip para o cors, apontando o ip do servidor onde as aplicações irão executar.

`A porta padrão é 5173`

#### Verifique a instalação do docker

```sh
docker --version
```

> Obs.: Necessário instalar docker [https://docs.docker.com/engine/install/ubuntu/]

O Compose utiliza imagens prontas e não depende do código-fonte local.
Inicie as aplicações com o comando.

```sh
docker compose up -d
```

> Execute docker logs -f velsis-monitoring-monitoring-1

> > Aguarde o app iniciar.

> A porta 5444 precisa estar aberta no servidor ou na máquina de execução do programa.

`Agora é só acessar a aplcaição no endereço: http://<ip.do.server>:5173/login`


## Se ao executar o compose, ocorrer erro semelhante a "permission denied while trying ..."
#### Faça

```sh
permission denied while trying to connect to the docker API at unix:///var/run/docker.sock
# Execute os comandos abaixo para adicionar o usuário corrente ao grupo do docker.
qembau@qembau:~/projects/prod/velsis-monitoring$ sudo usermod -aG docker $USER
qembau@qembau:~/projects/prod/velsis-monitoring$ newgrp docker
qembau@qembau:~/projects/prod/velsis-monitoring$ groups
docker adm cdrom sudo dip plugdev lxd qembau
```
