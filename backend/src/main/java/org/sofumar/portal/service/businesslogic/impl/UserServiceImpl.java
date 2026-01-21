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
import org.sofumar.portal.data.dto.UserDto;
import org.sofumar.portal.data.dto.UserProfileDto;
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto;
import org.sofumar.portal.data.dto.request.UserRequestDto;
import org.sofumar.portal.data.transformer.UserDtoTransformer;
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
import org.sofumar.portal.framework.service.RefreshTokenService;
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

import static org.sofumar.portal.constants.MessagesConstants.RECORD_UPDATED;

@Service
public class UserServiceImpl extends AbstractBusinessLogic<UserVO, UserRepository> implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expirationMinutes}")
    private int expMin;

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final UserDtoTransformer dtoTransformer;
    private final UserVOTransformer voTransformer;
    private final UserValidator validator;
    private final TokenBlacklistService blacklistService;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public UserServiceImpl(final UserRepository userRepo, final PasswordEncoder encoder, UserDtoTransformer dtoTransformer, final UserVOTransformer voTransformer, final UserValidator validator, TokenBlacklistService blacklistService, RefreshTokenService refreshTokenService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.dtoTransformer = dtoTransformer;
        this.voTransformer = voTransformer;
        this.validator = validator;
        this.blacklistService = blacklistService;
        this.refreshTokenService = refreshTokenService;
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
        UserVO userVO = voTransformer.transform(requestDto);
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

        String accessToken = generateAccessToken(userVO);
        String refreshToken = refreshTokenService.createRefreshToken(userVO.getUsername());
        
        return ResponseUtils.withMap(Map.of(
            "token", accessToken, 
            "refreshToken", refreshToken,
            "role", userVO.getRole(), 
            "firstName", userVO.getFirstName()
        ));
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            try {
                // Parse JWT to get expiration
                var claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                        .build()
                        .parseClaimsJws(accessToken)
                        .getBody();

                long expSeconds = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;

                if (expSeconds > 0) {
                    blacklistService.revokeToken(accessToken, expSeconds);
                }
            } catch (JwtException e) {
                // If token is already expired or invalid, we don't need to blacklist it.
            }
        }
        
        if (refreshToken != null) {
            refreshTokenService.deleteRefreshToken(refreshToken);
        }
    }

    @Override
    public ResponseEntity<?> refreshToken(String token) {
        try {
            String newRefreshToken = refreshTokenService.rotateRefreshToken(token);
            String username = refreshTokenService.validateRefreshToken(newRefreshToken).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token state"));
            
            UserVO userVO = userRepo.findOne(UserSpecifications.hasUsername(username)).orElseThrow(() -> new RecordNotFoundException("User not found"));
            
            if (!userVO.isActive()) {
                // Identify inactive user, revoke token
                refreshTokenService.deleteRefreshToken(newRefreshToken);
                return ResponseUtils.withStatus(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "Account inactive.");
            }
            
            String newAccessToken = generateAccessToken(userVO);
            
            return ResponseUtils.withMap(Map.of(
                "token", newAccessToken,
                "refreshToken", newRefreshToken
            ));
        } catch (IllegalArgumentException e) {
            return ResponseUtils.withStatus(HttpStatus.UNAUTHORIZED, Message.Type.ERROR, "Invalid or expired refresh token.");
        }
    }

    private String generateAccessToken(UserVO userVO) {
        return Jwts.builder()
                .setSubject(userVO.getUsername()).claim("roles", List.of(userVO.getRole()))
                .setIssuer("sofumar.portal").setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(expMin, ChronoUnit.MINUTES)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)), SignatureAlgorithm.HS256).compact();
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

    @Override
    public ResponseEntity<GlobalResponse<List<UserDto>>> getAllUsers() {
        List<UserVO> result = userRepo.findAll();
        return ResponseUtils.okWithData(dtoTransformer.transformList(result));
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> updateUserRole(Integer userId, String newRole) {
        UserVO userVO = userRepo.findById(userId).orElseThrow(() -> new RecordNotFoundException("User not found"));
        
        // Prevent removing the last active admin
        if (RoleConstants.ROLE_ADMIN.equals(userVO.getRole()) && !RoleConstants.ROLE_ADMIN.equals(newRole)) {
            if (userVO.isActive() && countActiveAdmins() <= 1) {
                return ResponseUtils.badRequest("Cannot update role for the last active " + RoleConstants.ROLE_ADMIN + ".");
            }
        }
        
        userVO.setRole(newRole);
        update(userVO);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("User role").getMessageString());
    }

    @Override
    @Transactional
    public ResponseEntity<GlobalResponse<Void>> toggleUserStatus(Integer userId, boolean active) {
        UserVO userVO = userRepo.findById(userId).orElseThrow(() -> new RecordNotFoundException("User not found"));
        
        // Prevent deactivating the last active admin
        if (!active && RoleConstants.ROLE_ADMIN.equals(userVO.getRole()) && countActiveAdmins() <= 1) {
            return ResponseUtils.badRequest("Cannot deactivate the last active " + RoleConstants.ROLE_ADMIN + ".");
        }
        
        userVO.setActive(active);
        update(userVO);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("User status").getMessageString());
    }

    private long countActiveAdmins() {
        return userRepo.count(UserSpecifications.hasRole(RoleConstants.ROLE_ADMIN).and(UserSpecifications.isActive(true)));
    }

}