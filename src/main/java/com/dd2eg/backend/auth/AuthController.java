package com.dd2eg.backend.auth;

import com.dd2eg.backend.auth.dto.LoginRequest;
import com.dd2eg.backend.auth.dto.SignupRequest;
import com.dd2eg.backend.users.User;
import com.dd2eg.backend.users.UserService;
import com.dd2eg.backend.users.UserType;
import com.dd2eg.backend.utils.Message;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@AllArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final TokenService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        Optional<User> userOptional = userService.getUserByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new Message("USER_NOT_FOUND", "User not found"));
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new Message("INVALID_CREDENTIALS", "Invalid credentials"));
        }

        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(token);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {

        Optional<User> existingUser = userService.getUserByEmail(request.getEmail());

        if (existingUser.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new Message(
                            "EMAIL_ALREADY_EXISTS",
                            "A user with this email already exists"
                    ));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setUserType(
                request.getUserType() != null
                        ? request.getUserType()
                        : UserType.DEVELOPER
        );

        User savedUser = userService.createUser(user);

        String token = jwtService.generateToken(savedUser);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(token);
    }
}