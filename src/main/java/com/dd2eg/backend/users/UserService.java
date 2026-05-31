package com.dd2eg.backend.users;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@AllArgsConstructor
@Service
public class UserService {


    @Autowired
    PasswordEncoder passwordEncoder;

    private final UserMongoRepository userRepository;

    private User getUserById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserByEmail(String email) {
        return userRepository.findAll().stream().filter(u -> u.getEmail().equals(email)).findFirst().orElse(null);
    }

    /**
     * Create a user
     * @param the created user
     * @return The created user
     */
    public User createUser(@RequestBody User user) {

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser).getBody();
    }


}
