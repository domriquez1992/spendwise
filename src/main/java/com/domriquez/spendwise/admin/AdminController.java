package com.domriquez.spendwise.admin;

import com.domriquez.spendwise.user.UserService;
import com.domriquez.spendwise.user.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administrative endpoints, restricted to users holding the ADMIN role. The class-level
 * {@code @PreAuthorize} is enforced by a Spring AOP interceptor before any method runs; a
 * non-admin caller is rejected with 403 before the handler executes.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return userService.findAllUsers();
    }
}
