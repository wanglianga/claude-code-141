package com.school.canteen.service;

import com.school.canteen.dto.AuthDtos.LoginRequest;
import com.school.canteen.dto.AuthDtos.LoginResponse;
import com.school.canteen.entity.User;
import com.school.canteen.exception.ApiException;
import com.school.canteen.repository.UserRepository;
import com.school.canteen.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expireMinutes;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, @Value("${app.jwt.expire-minutes}") long expireMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.expireMinutes = expireMinutes;
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .filter(User::isEnabled)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwtService.generate(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, user.getUsername(), user.getName(), user.getRole().name(), expireMinutes);
    }
}
