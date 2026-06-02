package com.example.iptvmanager.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.iptvmanager.exception.BadRequestException;
import com.example.iptvmanager.exception.ResourceNotFoundException;
import com.example.iptvmanager.user.dto.UpdateUserRequest;
import com.example.iptvmanager.user.dto.UserDTO;
import com.example.iptvmanager.user.entity.User;
import com.example.iptvmanager.user.mapper.UserMapper;
import com.example.iptvmanager.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDTO findById(Long id) {
        return UserMapper.toDTO(findEntity(id));
    }

    @Transactional
    public UserDTO update(Long id, UpdateUserRequest request) {
        User user = findEntity(id);
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(email)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> {
                    throw new BadRequestException("Email ja cadastrado");
                });

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.setActive(request.active());
        return UserMapper.toDTO(userRepository.save(user));
    }

    @Transactional
    public void deactivate(Long id) {
        User user = findEntity(id);
        user.setActive(false);
        userRepository.save(user);
    }

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
    }
}
