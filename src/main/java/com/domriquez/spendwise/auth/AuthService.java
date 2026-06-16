package com.domriquez.spendwise.auth;

import com.domriquez.spendwise.audit.AuditEventType;
import com.domriquez.spendwise.audit.AuditableEvent;
import com.domriquez.spendwise.auth.dto.AuthResponse;
import com.domriquez.spendwise.auth.dto.LoginRequest;
import com.domriquez.spendwise.auth.dto.RegisterRequest;
import com.domriquez.spendwise.exception.UsernameAlreadyExistsException;
import com.domriquez.spendwise.security.JwtService;
import com.domriquez.spendwise.user.Role;
import com.domriquez.spendwise.user.User;
import com.domriquez.spendwise.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login. Passwords are persisted only as BCrypt hashes; login compares the
 * supplied password against the stored hash and, on success, returns a freshly signed JWT.
 *
 * <p>Successful registrations and logins publish an {@link AuditableEvent}, recorded to the audit
 * log after the surrounding transaction commits. Failed logins deliberately raise an exception
 * before reaching the publish, so only genuine successes are recorded here.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }
        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                Role.USER);
        userRepository.save(user);
        eventPublisher.publishEvent(new AuditableEvent(
                AuditEventType.USER_REGISTERED, user.getUsername(), "Registered"));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // One generic message for both "no such user" and "wrong password" so the API does not
        // reveal which usernames are registered.
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername());
        eventPublisher.publishEvent(new AuditableEvent(
                AuditEventType.LOGIN_SUCCESS, user.getUsername(), "Logged in"));
        return new AuthResponse(token, "Bearer", user.getUsername());
    }
}
