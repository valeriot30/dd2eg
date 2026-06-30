package com.dd2eg.backend.controller;

import com.dd2eg.backend.DTO.CreateDevReportDTO;
import com.dd2eg.backend.model.User;
import com.dd2eg.backend.service.UserService;
import com.dd2eg.backend.utils.UserType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            summary = "Get user by username",
            description = "Returns a user based on their username"
    )
    @ApiResponse(responseCode = "200", description = "User found successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/users/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            return ResponseEntity.ok(userService.getUserByUsername(username));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Update user skills",
            description = "Updates the list of skills for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Skills updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PutMapping("/users/skills")
    public ResponseEntity<?> updateUserSkills(
            @RequestBody List<String> skills,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        try {
            User updatedUser = userService.updateUserSkills(currentUser.getId(), skills);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Ban a user",
            description = "Disables a user account. Accessible only by administrators."
    )
    @ApiResponse(responseCode = "200", description = "User banned successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/users/{id}/ban")
    public ResponseEntity<?> banUser(@PathVariable String id, @AuthenticationPrincipal User currentUser) {
        try {

            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            if(currentUser.getUserType() != UserType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
            }

            userService.updateUserStatus(id, false);
            return ResponseEntity.ok("User has been successfully banned.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Unban a user",
            description = "Re-enables a suspended user account. Accessible only by administrators."
    )
    @ApiResponse(responseCode = "200", description = "User unbanned successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/users/{id}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable String id) {
        try {
            userService.updateUserStatus(id, true);
            return ResponseEntity.ok("User has been successfully unbanned.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Report a developer",
            description = "Creates a developer report from the authenticated enterprise"
    )
    @ApiResponse(responseCode = "200", description = "Report created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or target user")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ENTERPRISE role")
    @PreAuthorize("hasAuthority('ENTERPRISE')")
    @PostMapping("/users/{id}/reports")
    public ResponseEntity<?> createDevReport(
            @PathVariable String id,
            @RequestBody CreateDevReportDTO request,
            @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(userService.createDevReport(id, request, currentUser));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get reported developers",
            description = "Returns developers whose report count is above the requested threshold, ordered by report count descending"
    )
    @ApiResponse(responseCode = "200", description = "Reported developers retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid threshold")
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/admin/users/reports")
    public ResponseEntity<?> getReportedDevelopers(@RequestParam int threshold) {
        if (threshold < 0) {
            return ResponseEntity.badRequest().body("Threshold must be greater than or equal to zero");
        }

        return ResponseEntity.ok(userService.getReportedDevelopersAboveThreshold(threshold));
    }

    @Operation(
            summary = "Get developer reports",
            description = "Returns all reports created for the specified developer. Accessible only by administrators."
    )
    @ApiResponse(responseCode = "200", description = "Developer reports retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    @ApiResponse(responseCode = "404", description = "Developer not found")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/admin/users/{id}/reports")
    public ResponseEntity<?> getDevReportsByDeveloperId(@PathVariable String id) {
        try {
            return ResponseEntity.ok(userService.getDevReportsByDeveloperId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
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
            summary = "Get user by ID",
            description = "Returns a user based on their unique ID"
    )
    @ApiResponse(responseCode = "200", description = "User found successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with id: " + id);
        }
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

    @Operation(
            summary = "Get projects funded by enterprise",
            description = "Returns list of projects funded by the authenticated enterprise"
    )
    @ApiResponse(responseCode = "200", description = "List of funded projects retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden - User is not an enterprise")
    @GetMapping("/users/enterprise/funded-projects")
    public ResponseEntity<?> getFundedProjects(@AuthenticationPrincipal User currentUser) {
        if (currentUser.getUserType() != UserType.ENTERPRISE) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Not authorized: This endpoint is for enterprises only");
        }

        return ResponseEntity.ok(userService.getProjectsFundedByEnterprise(currentUser.getId()));
    }
}
