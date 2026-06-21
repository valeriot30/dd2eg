package com.dd2eg.backend.controller;

import com.dd2eg.backend.DTO.LoginRequestDTO;
import com.dd2eg.backend.DTO.SignupRequestDTO;
import com.dd2eg.backend.service.TokenService;
import com.dd2eg.backend.model.User;
import com.dd2eg.backend.service.UserService;
import com.dd2eg.backend.utils.UserType;
import com.dd2eg.backend.utils.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication APIs (login, signup, user session)")
public class AuthController {

    private final UserService userService;
    private final TokenService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Operation(
            summary = "Login user",
            description = "Authenticates user and returns JWT token"
    )
    @ApiResponse(responseCode = "200", description = "Login successful (JWT token returned)")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {

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

    @Operation(
            summary = "Get current authenticated user",
            description = "Returns information about the logged-in user based on JWT token"
    )
    @ApiResponse(responseCode = "200", description = "User retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "userType", user.getUserType()
        ));
    }

    @Operation(
            summary = "Register new user",
            description = "Creates a new user and returns JWT token"
    )
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequestDTO request) {

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