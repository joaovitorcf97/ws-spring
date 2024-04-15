package tech.joaovitor.ws.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tech.joaovitor.ws.data.User;
import tech.joaovitor.ws.data.UserRepository;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public List<User> findChatUsers() {
        List<User> users = userRepository.findAll();
        System.out.println("Passou aqui" + users);
        return userRepository.findAll();
    }
}
