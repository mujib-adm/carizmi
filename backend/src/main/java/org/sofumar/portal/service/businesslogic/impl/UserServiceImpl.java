package org.sofumar.portal.service.businesslogic.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.RoleConstants;
import org.sofumar.portal.data.dto.UserProfileDto;
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto;
import org.sofumar.portal.data.dto.request.UserRequestDto;
import org.sofumar.portal.data.transformer.UserVOTransformer;
import org.sofumar.portal.data.vo.UserVO;
import org.sofumar.portal.framework.bl.AbstractBusinessLogic;
import org.sofumar.portal.framework.data.msg.Message;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.framework.service.TokenBlacklistService;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.repo.UserRepository;
import org.sofumar.portal.repo.jpaspec.UserSpecifications;
import org.sofumar.portal.service.businesslogic.UserService;
import org.sofumar.portal.service.validation.UserValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class UserServiceImpl extends AbstractBusinessLogic<UserVO, UserRepository> implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expirationMinutes}")
    private int expMin;

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final UserVOTransformer transformer;
    private final UserValidator validator;
    private final TokenBlacklistService blacklistService;

    @Autowired
    public UserServiceImpl(final UserRepository userRepo, final PasswordEncoder encoder, final UserVOTransformer transformer, final UserValidator validator, TokenBlacklistService blacklistService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.transformer = transformer;
        this.validator = validator;
        this.blacklistService = blacklistService;
    }

    @Override
    protected UserRepository getRepository() {
        return userRepo;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

//    @Override
//    protected void validateInternal(UserVO vo, boolean isUpdate) {
//        validator.validate(vo);
//    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> register(UserRequestDto requestDto) {
        UserVO userVO = transformer.transform(requestDto);
        userVO.setRole(RoleConstants.ROLE_ANONYMOUS);
        userVO.setActive(true);
        validator.validate(userVO);
        userVO.setPassword(encoder.encode(userVO.getPassword()));
        add(userVO);
        return ResponseUtils.ok("Registered. Await role assignment.");
    }

    @Override
    public ResponseEntity<?> login(String username, String password) {
        UserVO userVO = userRepo.findOne(UserSpecifications.hasUsername(username)).orElse(null);
        if (userVO == null || !encoder.matches(password, userVO.getPassword())) {
            return ResponseUtils.withStatus(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "Invalid username or password.");
        } else if (!userVO.isActive()) {
            return ResponseUtils.withStatus(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "Your account is inactive. Please contact support for assistance.");
        } else if (RoleConstants.ROLE_ANONYMOUS.equalsIgnoreCase(userVO.getRole())) {
            return ResponseUtils.ok("Welcome! Your account is pending role assignment. Please contact support for assistance.");
        }

        String token = Jwts.builder()
                .setSubject(userVO.getUsername()).claim("roles", List.of(userVO.getRole()))
                .setIssuer("sofumar.portal").setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(expMin, ChronoUnit.MINUTES)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)), SignatureAlgorithm.HS256).compact();
        return ResponseUtils.withMap(Map.of("token", token, "role", userVO.getRole(), "firstName", userVO.getFirstName()));
    }

    @Override
    public void logout(String token) {
        try {
            // Parse JWT to get expiration
            var claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            long expSeconds = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;

            if (expSeconds > 0) {
                blacklistService.revokeToken(token, expSeconds);
            }
        } catch (JwtException e) {
            // If token is already expired or invalid, we don't need to blacklist it.
        }
    }

    @Override
    public ResponseEntity<GlobalResponse<UserProfileDto>> getProfile(String username) {
        logger.info("Fetching profile for user: {}", username);
        UserVO userVO = userRepo.findOne(UserSpecifications.hasUsername(username)).orElse(null);
        if (userVO == null) {
            logger.warn("User not found: {}", username);
            return ResponseUtils.withStatusAndData(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "User not found.");
        }
        
        UserProfileDto dto = UserProfileDto.builder()
                .username(userVO.getUsername())
                .role(userVO.getRole())
                .firstName(userVO.getFirstName())
                .lastName(userVO.getLastName())
                .email(userVO.getEmail())
                .build();
                
        logger.info("Returning profile DTO for user: {}", username);
        return ResponseUtils.okWithData(dto);
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updatePassword(String username, String token, PasswordUpdateRequestDto requestDto) {
        UserVO userVO = userRepo.findOne(UserSpecifications.hasUsername(username))
                .orElseThrow(() -> new RecordNotFoundException("User not found: " + username));

        if (!encoder.matches(requestDto.getOldPassword(), userVO.getPassword())) {
            return ResponseUtils.badRequest("Incorrect old password.");
        }

        if (!requestDto.getNewPassword().equals(requestDto.getConfirmPassword())) {
            return ResponseUtils.badRequest("New password and confirmation do not match.");
        }

        userVO.setPassword(requestDto.getNewPassword());
        validator.validate(userVO);
        userVO.setPassword(encoder.encode(userVO.getPassword()));
        userVO.setPasswordUpdatedAt(LocalDateTime.now());
        
        // Revoke the current token
        if (token != null) {
             blacklistService.revokeToken(token, expMin * 60L);
        }

        update(userVO);

        return ResponseUtils.ok("Password updated successfully! Please log in again.");
    }

}