package org.sofumar.portal.core.businesslogic.impl

import org.sofumar.portal.constants.FieldConstants
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.sofumar.portal.constants.RoleConstants
import org.sofumar.portal.core.repo.UserRepository
import org.sofumar.portal.core.vo.UserVO
import org.sofumar.portal.data.dto.response.UserResponseDto
import org.sofumar.portal.data.dto.response.UserProfileDto
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto
import org.sofumar.portal.data.dto.request.UserRequestDto
import org.sofumar.portal.data.transformer.UserResponseDtoTransformer
import org.sofumar.portal.data.transformer.UserVOTransformer
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.framework.exception.DuplicateRecordException
import org.sofumar.portal.framework.exception.RecordNotFoundException
import org.sofumar.portal.framework.service.RefreshTokenService
import org.sofumar.portal.framework.service.TokenBlacklistService
import org.sofumar.portal.framework.util.MySQLConstraintResolver
import org.sofumar.portal.service.validation.UserValidator
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Subject
import spock.lang.Unroll

class UserSpec extends BaseSpecification {

    UserRepository userRepo = Mock()
    PasswordEncoder encoder = Mock()
    UserResponseDtoTransformer dtoTransformer = Mock()
    UserVOTransformer voTransformer = Mock()
    UserValidator validator = Mock()
    TokenBlacklistService blacklistService = Mock()
    RefreshTokenService refreshTokenService = Mock()
    MySQLConstraintResolver constraintResolver = Mock()

    @Subject
    UserImpl userService = new UserImpl(userRepo, encoder, dtoTransformer, voTransformer, validator, blacklistService, refreshTokenService)

    def setup() {
        userService.secret = Base64.getEncoder().encodeToString("super-secret-key-that-is-long-enough-32-chars".getBytes())
        userService.expMin = 60
        ReflectionTestUtils.setField(userService, "constraintResolver", constraintResolver)
    }

    def "test - register: Should save user"() {
        given: "A registration request and capture variables"
        String username = "user"
        String password = "p"
        UserRequestDto request = new UserRequestDto(username: username, password: password)
        UserVO vo = new UserVO(username: username, password: password)
        UserVO savedVo = null

        when: "The target method executed"
        userService.register(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(request) >> vo
        1 * validator.validate(vo)
        1 * encoder.encode("p") >> "enc"
        1 * userRepo.save(vo) >> { UserVO u -> savedVo = u; vo }
        0 * _

        and: "The expected result"
        savedVo.role == RoleConstants.ROLE_ANONYMOUS
        savedVo.active
        noExceptionThrown()
    }

    def "test - register: Duplicate Handling"() {
        given: "A duplicate registration scenario"
        UserRequestDto request = new UserRequestDto(username: "u")
        UserVO vo = new UserVO(username: "u")

        when: "The target method executed"
        userService.register(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> vo
        1 * validator.validate(_)
        1 * encoder.encode(_) >> "enc"
        1 * userRepo.save(_) >> { throw new DataIntegrityViolationException("Dup", new RuntimeException("Duplicate entry 'u' for key 'UK_username'")) }
        1 * constraintResolver.resolveFields(_) >> ["username"]
        0 * _

        and: "The expected result"
        thrown(DuplicateRecordException)
    }

    def "test - register: General DB Error"() {
        given: "A DB error scenario during registration"
        UserRequestDto request = new UserRequestDto(username: "u")
        UserVO vo = new UserVO(username: "u")

        when: "The target method executed"
        userService.register(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> vo
        1 * validator.validate(_)
        1 * encoder.encode(_) >> "enc"
        1 * userRepo.save(_) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    @Unroll
    def "test - login: Handling all failure branches [exists: #exists, passes: #passes, active: #active, role: #role, expectedStatus: #expectedStatus]"() {
        given: "A user setup for login permutations"
        String username = "user"
        String password = "pass"
        String encoded = "enc"
        UserVO user = new UserVO(username: username, password: encoded, active: active, role: role, firstName: "First", lastName: "Last")
        ResponseEntity<?> response
        JpaSpecification capturedSpec

        when: "The target method executed"
        response = userService.login(username, password)

        then: "The expected calls are made"
        1 * userRepo.findOne(_ as JpaSpecification) >> { JpaSpecification spec ->
            capturedSpec = spec
            return (exists ? Optional.of(user) : Optional.empty())
        }
        if (exists) {
            1 * encoder.matches("pass", "enc") >> passes
            if (passes && active && role != RoleConstants.ROLE_ANONYMOUS) {
                1 * refreshTokenService.createRefreshToken("user") >> "refresh"
            }
        }
        0 * _

        and: "The expected result"
        response.statusCode == expectedStatus
        noExceptionThrown()
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([FieldConstants.USERNAME])
        inspection.values.containsAll([username])

        where:
        exists | passes | active | role                         | expectedStatus
        false  | _      | _      | _                            | HttpStatus.UNAUTHORIZED
        true   | false  | _      | _                            | HttpStatus.UNAUTHORIZED
        true   | true   | false  | _                            | HttpStatus.UNAUTHORIZED
        true   | true   | true   | RoleConstants.ROLE_ANONYMOUS | HttpStatus.OK
        true   | true   | true   | RoleConstants.ROLE_ADMIN     | HttpStatus.OK
    }

    @Unroll
    def "test - logout: Covering JWT exceptions and null tokens [access: #access, refresh: #refresh]"() {
        given: "Logout tokens"

        when: "The target method executed"
        userService.logout(access, refresh)

        then: "The expected calls are made"
        if (refresh != null) {
            1 * refreshTokenService.deleteRefreshToken(refresh)
        }
        0 * _

        and: "The expected result"
        noExceptionThrown()

        where:
        access                                          | refresh
        null                                            | "ref"
        "invalid"                                       | "ref"
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.t-x9" | null
    }

    def "test - logout: Valid token should be blacklisted"() {
        given: "A valid generated token"
        String secretRaw = "super-secret-key-that-is-long-enough-32-chars"
        String base64Secret = Base64.getEncoder().encodeToString(secretRaw.getBytes())
        // Ensure the service has the matching secret (already set in setup, but good to be explicit/safe)
        userService.secret = base64Secret
        // Generate token
        Instant now = Instant.now()
        String validToken = Jwts.builder()
                .setSubject("user")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret)), SignatureAlgorithm.HS256)
                .compact()
        String refreshToken = "refresh-token"

        when: "The target method executed"
        userService.logout(validToken, refreshToken)

        then: "The expected calls are made"
        1 * blacklistService.revokeToken(validToken, _ as Long)
        1 * refreshTokenService.deleteRefreshToken(refreshToken)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    @Unroll
    def "test - refreshToken: Handling rotation and user state [isValid: #isValid, userActive: #userActive, expectedStatus: #expectedStatus]"() {
        given: "A refresh token scenario"
        String oldToken = "old-token"
        String newToken = "new-token"
        String username = "user"
        ResponseEntity<?> response

        when: "The target method executed"
        response = userService.refreshToken(oldToken)

        then: "The expected calls are made"
        1 * refreshTokenService.rotateRefreshToken(oldToken) >> { if (isValid) return newToken; else throw new IllegalArgumentException() }
        if (isValid) {
            1 * refreshTokenService.validateRefreshToken(newToken) >> Optional.of(username)
            1 * userRepo.findOne(_) >> Optional.of(new UserVO(username: username, active: userActive, role: RoleConstants.ROLE_ADMIN, firstName: "F"))
            if (!userActive) {
                1 * refreshTokenService.deleteRefreshToken(newToken)
            }
        }
        0 * _

        and: "The expected result"
        response.statusCode == expectedStatus
        noExceptionThrown()

        where:
        isValid | userActive | expectedStatus
        false   | _          | HttpStatus.UNAUTHORIZED
        true    | false      | HttpStatus.UNAUTHORIZED
        true    | true       | HttpStatus.OK
    }

    def "test - getProfile: Handling missing user"() {
        given: "A missing username"
        String username = "missing"
        ResponseEntity<GlobalResponse<UserProfileDto>> response

        when: "The target method executed"
        response = userService.getProfile(username)

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.empty()
        0 * _

        and: "The expected result"
        response.statusCode == HttpStatus.UNAUTHORIZED
        noExceptionThrown()
    }

    def "test - getProfile: Success"() {
        given: "A valid user"
        String username = "user"
        String role = RoleConstants.ROLE_ADMIN
        String firstName = "First"
        String lastName = "Last"
        String email = "user@example.com"
        UserVO user = new UserVO(username: username, role: role, firstName: firstName, lastName: lastName, email: email)
        ResponseEntity<GlobalResponse<UserProfileDto>> response

        when: "The target method executed"
        response = userService.getProfile(username)

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.of(user)
        0 * _

        and: "The expected result"
        response.statusCode == HttpStatus.OK
        response.body.responseData.username == username
        response.body.responseData.role == role
        response.body.responseData.firstName == firstName
        noExceptionThrown()
    }

    @Unroll
    def "test - updatePassword: Validation failures [matchOld: #matchOld, conf: #conf, token: #token, expectedStatus: #expectedStatus]"() {
        given: "Password update request and existing user"
        String username = "user"
        String oldPass = "old"
        String newPass = "new"
        String oldEnc = "oldEnc"
        String newEnc = "newEnc"
        UserVO user = new UserVO(username: username, password: oldEnc)
        PasswordUpdateRequestDto req = new PasswordUpdateRequestDto(oldPassword: oldPass, newPassword: newPass, confirmPassword: conf)
        ResponseEntity<GlobalResponse<Void>> response

        when: "The target method executed"
        response = userService.updatePassword(username, token, req)

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.of(user)
        if (matchOld) {
            1 * encoder.matches(oldPass, oldEnc) >> true
            if (req.newPassword == req.confirmPassword) {
                1 * validator.validate(user)
                1 * encoder.encode(newPass) >> newEnc
                if (token != null) {
                    1 * blacklistService.revokeToken(token, _)
                }
                1 * userRepo.save(user) >> user
            }
        } else {
            1 * encoder.matches(oldPass, oldEnc) >> false
        }
        0 * _

        and: "The expected result"
        response.statusCode == expectedStatus
        noExceptionThrown()

        where:
        matchOld | conf  | token | expectedStatus
        false    | "new" | "t"   | HttpStatus.BAD_REQUEST
        true     | "bad" | "t"   | HttpStatus.BAD_REQUEST
        true     | "new" | "t"   | HttpStatus.OK
        true     | "new" | null  | HttpStatus.OK
    }

    def "test - updatePassword: DB Error"() {
        given: "A DB error scenario"
        String username = "user"
        UserVO user = new UserVO(username: username, password: "old")
        PasswordUpdateRequestDto req = new PasswordUpdateRequestDto(oldPassword: "old", newPassword: "new", confirmPassword: "new")

        when: "The target method executed"
        userService.updatePassword(username, null, req)

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.of(user)
        1 * encoder.matches(_, _) >> true
        1 * validator.validate(_)
        1 * encoder.encode(_) >> "enc"
        1 * userRepo.save(_) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        user.hasErrors()
        noExceptionThrown()
    }

    @Unroll
    def "test - admin guards: Protecting last admin [op: #op, onlyOne: #onlyOne, expectedStatus: #expectedStatus]"() {
        given: "An admin user and operational setup"
        Integer adminId = 1
        String targetRole = RoleConstants.ROLE_MEMBER
        UserVO admin = new UserVO(userID: adminId, role: RoleConstants.ROLE_ADMIN, active: true)
        ResponseEntity<GlobalResponse<Void>> response
        JpaSpecification capturedSpec

        when: "The target method executed"
        response = (op == "role") ?
                userService.updateUserRole(adminId, targetRole) :
                userService.toggleUserStatus(adminId, false)

        then: "The expected calls are made"
        1 * userRepo.findById(adminId) >> Optional.of(admin)
        1 * userRepo.count(_ as JpaSpecification) >> { JpaSpecification spec ->
            capturedSpec = spec
            return (onlyOne ? 1 : 2)
        }
        if (!onlyOne) {
            1 * userRepo.save(_) >> admin
        }
        0 * _

        and: "The expected result"
        response.statusCode == expectedStatus
        noExceptionThrown()
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([FieldConstants.ROLE, FieldConstants.ACTIVE])
        inspection.values.containsAll([RoleConstants.ROLE_ADMIN, true])

        where:
        op       | onlyOne | expectedStatus
        "role"   | true    | HttpStatus.BAD_REQUEST
        "role"   | false   | HttpStatus.OK
        "status" | true    | HttpStatus.BAD_REQUEST
        "status" | false   | HttpStatus.OK
    }

    def "test - updateUserRole: DB Error"() {
        given: "A DB error scenario"
        Integer id = 1
        UserVO user = new UserVO(userID: id, role: RoleConstants.ROLE_ADMIN, active: true)
        JpaSpecification capturedSpec

        when: "The target method executed"
        userService.updateUserRole(id, RoleConstants.ROLE_MEMBER)

        then: "The expected calls are made"
        1 * userRepo.findById(id) >> Optional.of(user)
        1 * userRepo.count(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; 2 }
        1 * userRepo.save(_) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        user.hasErrors()
        noExceptionThrown()
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([FieldConstants.ROLE, FieldConstants.ACTIVE])
        inspection.values.containsAll([RoleConstants.ROLE_ADMIN, true])
    }

    def "test - toggleUserStatus: DB Error"() {
        given: "A DB error scenario"
        Integer id = 1
        UserVO user = new UserVO(userID: id, role: RoleConstants.ROLE_ADMIN, active: true)
        JpaSpecification capturedSpec

        when: "The target method executed"
        userService.toggleUserStatus(id, false)

        then: "The expected calls are made"
        1 * userRepo.findById(id) >> Optional.of(user)
        1 * userRepo.count(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; 2 }
        1 * userRepo.save(_) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        user.hasErrors()
        noExceptionThrown()
        capturedSpec != null
    }

    def "test - updatePassword: User not found"() {
        given: "A missing password update username"
        String username = "missing"
        String token = "token"

        when: "The target method executed"
        userService.updatePassword(username, token, new PasswordUpdateRequestDto())

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    @Unroll
    def "test - updateUserRole/Status: User not found [op: #op]"() {
        given: "A missing user ID for role/status change"
        Integer userId = 99

        when: "The target method executed"
        if (op == "role") userService.updateUserRole(userId, "ROLE")
        else userService.toggleUserStatus(userId, true)

        then: "The expected calls are made"
        1 * userRepo.findById(userId) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)

        where:
        op << ["role", "status"]
    }

    def "test - getAllUsers: Should return all users"() {
        given: "A list request"

        when: "The target method executed"
        userService.getAllUsers()

        then: "The expected calls are made"
        1 * userRepo.findAll() >> []
        1 * dtoTransformer.transformList(_) >> []
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    @Unroll
    def "test - adminUserExists: Should check for admin existence [exists: #exists]"() {
        given: "A repository state"
        JpaSpecification capturedSpec

        when: "The target method executed"
        def result = userService.adminUserExists()

        then: "The expected calls are made"
        1 * userRepo.exists(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; exists }
        0 * _

        and: "The expected result"
        result == exists
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([FieldConstants.ROLE])
        inspection.values.containsAll([RoleConstants.ROLE_ADMIN])

        where:
        exists << [true, false]
    }
}