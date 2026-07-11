package com.mos.auth.service;

import com.mos.auth.dto.LoginRequest;
import com.mos.auth.dto.LoginResponse;
import com.mos.auth.dto.LoginSessionResponse;
import com.mos.auth.dto.LoginUserResponse;
import com.mos.common.exception.InvalidCredentialsException;
import com.mos.common.exception.UserInactiveException;
import com.mos.security.JwtTokenProvider;
import com.mos.session.entity.GameSession;
import com.mos.session.entity.SessionParticipant;
import com.mos.session.repository.SessionParticipantRepository;
import com.mos.session.service.SessionResolver;
import com.mos.user.entity.User;
import com.mos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final SessionParticipantRepository sessionParticipantRepository;
    private final SessionResolver sessionResolver;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new UserInactiveException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        GameSession session = sessionResolver.resolveCurrentSession();

        SessionParticipant participant = sessionParticipantRepository
                .findByUserIdAndGameSessionId(user.getId(), session.getId())
                .orElseThrow(() -> new InvalidCredentialsException("User is not a participant of the current session"));

        String token = jwtTokenProvider.createToken(user.getId(), session.getId(), participant.getRole());

        log.info("User logged in: username={} userId={} sessionId={} role={}",
                user.getUsername(), user.getId(), session.getId(), participant.getRole());

        return new LoginResponse(
                token,
                LoginUserResponse.from(user, participant.getRole()),
                LoginSessionResponse.from(session)
        );
    }
}
