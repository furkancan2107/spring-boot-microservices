package com.rf.user_service.service;

import com.rf.user_service.dto.LoginRequest;
import com.rf.user_service.dto.RegisterUserRequest;
import com.rf.user_service.dto.UserDto;
import com.rf.user_service.entity.User;
import com.rf.user_service.exception.AuthenticateException;
import com.rf.user_service.exception.NotFoundException;
import com.rf.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final KafkaTemplate kafkaTemplate;
    public UserDto register(RegisterUserRequest request) {
        if(repository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Kullanici zaten kayitli");
        }
        User user=User.builder().email(request.getEmail()).password(encoder.encode(request.getPassword())).name(request.getName()).build();
        repository.save(user);
        return UserDto.builder().id(user.getId()).email(user.getEmail()).name(user.getName()).build();
    }

    public UserDto getUser(Long id) {
       User user=repository.findById(id).orElseThrow(()->new NotFoundException("kullanici"));
       return UserDto.builder().name(user.getName()).email(user.getEmail()).id(user.getId()).build();
    }
    public User findByEmail(String email){
        User user=repository.findByEmail(email).orElseThrow(()->new NotFoundException("email"));
        return user;
    }
    public UserDto authenticate(LoginRequest request){
        User user=findByEmail(request.getEmail());
        if(!encoder.matches(request.getPassword(), user.getPassword())) throw new AuthenticateException();
        return UserDto.builder().name(user.getName()).email(user.getEmail()).id(user.getId()).build();
    }

    public UserDto getUserForEmail(String email) {
        User user=findByEmail(email);
        return UserDto.builder().name(user.getName()).email(user.getEmail()).id(user.getId()).build();
    }
    public void deleteUser(Long id){
        repository.deleteById(id);
        kafkaTemplate.send("user-delete-event",id);

        System.out.println("Kullanıcı Silindi");
    }
}
