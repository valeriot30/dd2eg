package com.dd2eg.backend.controller;

import com.dd2eg.backend.model.User;
import com.dd2eg.backend.service.UserService;
import com.dd2eg.backend.utils.UserType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "Users management API")
@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get all users",
            description = "Returns a list of all registered users"
    )
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @Operation(
            summary = "Create a new user",
            description = "Creates a new user in the system"
    )
    @ApiResponse(responseCode = "200", description = "User created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid user data")
    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @Operation(
            summary = "Get user by email",
            description = "Returns a user based on their email address"
    )
    @ApiResponse(responseCode = "200", description = "User found successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/users/profile/{email}")
    public User getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email).orElseThrow(null);
    }

    @Operation(
            summary = "Get developer dashboard",
            description = "Returns dashboard statistics for the authenticated developer"
    )
    @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - User is not a developer")
    @GetMapping("/users/dashboard/developer")
    public ResponseEntity<?> getDeveloperDashboard(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getUserType() != UserType.DEVELOPER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Not authorized: This dashboard is for developers only");
        }

        return ResponseEntity.ok(userService.getDeveloperStats(currentUser.getId()));
    }

    @Operation(
            summary = "Get enterprise dashboard",
            description = "Returns dashboard statistics for the authenticated enterprise"
    )
    @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - User is not an enterprise")
    @GetMapping("/users/dashboard/enterprise")
    public ResponseEntity<?> getEnterpriseDashboard(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getUserType() != UserType.ENTERPRISE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Not authorized: This dashboard is for enterprises only");
        }

        return ResponseEntity.ok(userService.getEnterpriseDashboardStats(currentUser.getId()));
    }
}
