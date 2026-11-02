package com.velsis.monitoring.hardware.cam.service;

import com.velsis.monitoring.hardware.cam.model.CameraPage;
import com.velsis.monitoring.hardware.cam.model.CameraRequest;
import com.velsis.monitoring.hardware.cam.model.CameraResponse;
import com.velsis.monitoring.hardware.cam.repository.CameraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

@Service
public class CameraService {
    private final CameraRepository repository;

    public CameraService(CameraRepository repository) {
        this.repository = repository;
    }

    public CameraPage list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) invalid("Paginação inválida");
        return new CameraPage(repository.findPage(page, size), repository.count(), page, size);
    }

    public CameraResponse find(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Câmera não encontrada"));
    }

    @Transactional
    public CameraResponse create(CameraRequest request) {
        required(request.nome(), 150, "Nome");
        required(request.usuario(), 100, "Usuário");
        required(request.senha(), 255, "Senha");
        required(request.enderecoip(), 45, "Endereço IP");
        if (request.descricao() != null && request.descricao().length() > 1000) invalid("Descrição muito longa");
        if (!validIp(request.enderecoip().trim())) invalid("Endereço IP inválido");
        port(request.portaHttp());
        port(request.portaRtsp());
        if (request.status() == null) invalid("Status obrigatório");
        try {
            return find(repository.insert(request));
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe uma camera com esse nome");
        }
    }

    @Transactional
    public void delete(long id) {
        if (!repository.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Câmera não encontrada");
        }
    }

    private static boolean validIp(String value) {
        if ("localhost".equalsIgnoreCase(value)) return true;
        if (value.contains(":") && value.matches("[0-9a-fA-F:.]+")) {
            try {
                InetAddress.getByName(value);
                return true;
            } catch (UnknownHostException exception) {
                return false;
            }
        }
        String[] parts = value.split("\\.", -1);
        return parts.length == 4 && Arrays.stream(parts)
                .allMatch(part -> part.matches("[0-9]{1,3}") && Integer.parseInt(part) <= 255);
    }

    private static void required(String value, int limit, String field) {
        if (value == null || value.isBlank() || value.length() > limit) invalid(field + " inválido");
    }

    private static void port(Integer port) {
        if (port == null || port < 1 || port > 65535) invalid("Porta deve estar entre 1 e 65535");
    }

    private static void invalid(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
