# No servidor que irá hospedar as aplicações utilize o docker compose.

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
