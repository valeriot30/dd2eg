package com.dd2eg.backend.users;

import com.dd2eg.backend.users.dto.EnterpriseStatsDTO;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping("/users/profile/")
    public User getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email).orElseThrow(null);
    }

    @GetMapping("/users/dashboard")
    public ResponseEntity<EnterpriseStatsDTO> getMyDashboard(@AuthenticationPrincipal User currentUser) {

        EnterpriseStatsDTO stats = userService.getDashboardStats(currentUser.getId());

        return ResponseEntity.ok(stats);
    }
}
