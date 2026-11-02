# Instalação e instruções.

Faça o download dos artefatos:

> velsis/case tecnico/compose/composetar.tar

Envie o arquivo composetar.tar para o servidor.

Descompacte o arquivo com o comando:

```sh
tar -xvf composetar.tar
```

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

> > Aguarde a app iniciar.

> A porta 5444 precisa estar aberta no servidor ou na máquina de execução do programa

> Se ao executar o compose, ocorrer erro semelhante á: permission denied while trying ...

### Faça

```sh
permission denied while trying to connect to the docker API at unix:///var/run/docker.sock
# Execute os comandos abaixo para adicionar o usuário corrente ao grupo do docker.
qembau@qembau:~/projects/prod/velsis-monitoring$ sudo usermod -aG docker $USER
qembau@qembau:~/projects/prod/velsis-monitoring$ newgrp docker
qembau@qembau:~/projects/prod/velsis-monitoring$ groups
docker adm cdrom sudo dip plugdev lxd qembau
```
