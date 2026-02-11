package org.sofumar.portal.core.businesslogic.impl

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.constants.MessagesConstants
import org.sofumar.portal.constants.Role
import org.sofumar.portal.core.repo.UserRepository
import org.sofumar.portal.core.vo.UserVO
import org.sofumar.portal.data.dto.UserDto
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto
import org.sofumar.portal.data.dto.response.UserProfileDto
import org.sofumar.portal.data.dto.response.UserResponseDto
import org.sofumar.portal.security.JwtService
import org.sofumar.portal.security.SofumarUserDetails
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

import java.time.LocalDateTime

class UserSpec extends BaseSpecification {

    UserRepository userRepo = Mock()
    PasswordEncoder encoder = Mock()
    UserResponseDtoTransformer dtoTransformer = Mock()
    UserVOTransformer voTransformer = Mock()
    UserValidator validator = Mock()
    TokenBlacklistService blacklistService = Mock()
    RefreshTokenService refreshTokenService = Mock()
    JwtService jwtService = Mock()
    MySQLConstraintResolver constraintResolver = Mock()

    @Subject
    UserImpl userImpl = new UserImpl(userRepo, encoder, dtoTransformer, voTransformer, validator, blacklistService, refreshTokenService, jwtService)

    void setup() {
        userImpl.expMin = 60
        ReflectionTestUtils.setField(userImpl, "constraintResolver", constraintResolver)
    }

    def "test - register: Should save user"() {
        given: "A registration request and capture variables"
        String username = "Test"
        String password = "p"
        UserDto request = new UserDto(username: username, password: password)
        UserVO vo = new UserVO(username: username, password: password)
        UserVO savedVo = null

        when: "The target method executed"
        userImpl.register(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(request) >> vo
        1 * validator.validate(vo)
        1 * encoder.encode("p") >> "enc"
        1 * userRepo.save(vo) >> { UserVO u -> savedVo = u; vo }
        0 * _

        and: "The expected result"
        savedVo.role == Role.ANONYMOUS
        savedVo.active
        noExceptionThrown()
    }

    def "test - register: Duplicate Handling"() {
        given: "A duplicate registration scenario"
        UserDto request = new UserDto(username: "u")
        UserVO vo = new UserVO(username: "u")

        when: "The target method executed"
        userImpl.register(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> vo
        1 * validator.validate(_)
        1 * encoder.encode(_) >> "enc"
        1 * userRepo.save(_) >> { throw new DataIntegrityViolationException("Dup", new RuntimeException("Duplicate entry 'u' for key 'UK_username'")) }
        1 * constraintResolver.resolveFields(_) >> [FieldConstants.USERNAME]
        0 * _

        and: "The expected result"
        thrown(DuplicateRecordException)
    }

    def "test - register: General DB Error"() {
        given: "A DB error scenario during registration"
        UserDto request = new UserDto(username: "u")
        UserVO vo = new UserVO(username: "u")

        when: "The target method executed"
        userImpl.register(request)

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

    def "test - findUserForAuthentication: Should find user by username"() {
        given: "A username"
        String username = "Test"
        UserVO userVO = new UserVO(username: username)

        when: "The target method executed"
        UserVO result = userImpl.findUserForAuthentication(username)

        then: "The expected calls are made"
        1 * userRepo.findOne(_ as JpaSpecification) >> Optional.of(userVO)
        0 * _

        and: "The expected result"
        result == userVO
    }

    @Unroll
    def "test - onLoginSuccess: Should reset state if needed [attempts: #attempts, locked: #locked -> shouldUpdate: #shouldUpdate]"() {
        given: "A userVO with some failed state"
        String username = "Test"
        LocalDateTime lockoutTime = locked ? LocalDateTime.now() : null
        UserVO userVO = new UserVO(username: username, failedLoginAttempts: attempts, lockoutTime: lockoutTime)

        when: "The target method executed"
        userImpl.onLoginSuccess(username)

        then: "The expected calls are made"
        1 * userRepo.findOne(_ as JpaSpecification) >> Optional.of(userVO)
        if (shouldUpdate) {
            1 * userRepo.save(userVO) >> userVO
        }
        0 * _

        and: "The expected result"
        userVO.failedLoginAttempts == 0
        userVO.lockoutTime == null

        where:
        attempts | locked | shouldUpdate
        3        | true   | true
        3        | false  | true
        0        | true   | true
        0        | false  | false
    }

    def "test - onLoginFailure: Should increment attempts"() {
        given: "A userVO with 0 failures"
        String username = "Test"
        UserVO userVO = new UserVO(username: username, failedLoginAttempts: 0)

        when: "The target method executed"
        userImpl.onLoginFailure(username)

        then: "The expected calls are made"
        1 * userRepo.findOne(_ as JpaSpecification) >> Optional.of(userVO)
        1 * userRepo.save(userVO) >> userVO
        0 * _

        and: "The expected result"
        userVO.failedLoginAttempts == 1
        userVO.lockoutTime == null
    }

    def "test - onLoginFailure: Should lock account on 5th failure"() {
        given: "A userVO with 4 failures"
        String username = "Test"
        UserVO userVO = new UserVO(username: username, failedLoginAttempts: 4)

        when: "The target method executed"
        userImpl.onLoginFailure(username)

        then: "The expected calls are made"
        1 * userRepo.findOne(_ as JpaSpecification) >> Optional.of(userVO)
        1 * userRepo.save(userVO) >> userVO
        0 * _

        and: "The expected result"
        userVO.failedLoginAttempts == 5
        userVO.lockoutTime != null
    }

    @Unroll
    def "test - logout: Covering JWT exceptions and null tokens [access: #access, refresh: #refresh]"() {
        given: "Logout tokens"

        when: "The target method executed"
        userImpl.logout(access, refresh)

        then: "The expected calls are made"
        if (access != null) {
            1 * jwtService.getRemainingExpirationSeconds(access) >> 0
        }
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
        String validToken = "valid-token"
        String refreshToken = "refresh-token"

        when: "The target method executed"
        userImpl.logout(validToken, refreshToken)

        then: "The expected calls are made"
        1 * jwtService.getRemainingExpirationSeconds(validToken) >> 3600
        1 * blacklistService.revokeToken(validToken, 3600L)
        1 * refreshTokenService.deleteRefreshToken(refreshToken)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    @Unroll
    def "test - refreshToken: Handling rotation and user state [isValid: #isValid, userActive: #userActive, isLocked: #isLocked, expectedStatus: #expectedStatus]"() {
        given: "A refresh token scenario"
        String oldToken = "old-token"
        String newToken = "new-token"
        String username = "Test"
        UserVO userVO = new UserVO(
                username: username,
                active: userActive,
                role: Role.ADMIN,
                firstName: "F",
                lockoutTime: isLocked ? LocalDateTime.now() : null
        )
        ResponseEntity<?> response

        when: "The target method executed"
        response = userImpl.refreshToken(oldToken)

        then: "The expected calls are made"
        1 * refreshTokenService.rotateRefreshToken(oldToken) >> { if (isValid) return newToken; else throw new IllegalArgumentException() }
        if (isValid) {
            1 * refreshTokenService.validateRefreshToken(newToken) >> Optional.of(username)
            1 * userRepo.findOne(_) >> Optional.of(userVO)
            if (!userActive || isLocked) {
                1 * refreshTokenService.deleteRefreshToken(newToken)
            } else {
                1 * jwtService.generateAccessToken(_ as SofumarUserDetails) >> "new-access-token"
            }
        }
        0 * _

        and: "The expected result"
        response.statusCode == expectedStatus
        noExceptionThrown()

        where:
        isValid | userActive | isLocked | expectedStatus
        false   | _          | _        | HttpStatus.UNAUTHORIZED
        true    | false      | _        | HttpStatus.UNAUTHORIZED
        true    | true       | true     | HttpStatus.UNAUTHORIZED
        true    | true       | false    | HttpStatus.OK
    }

    def "test - getProfile: Handling missing user"() {
        given: "A missing username"
        String username = "missing"
        ResponseEntity<GlobalResponse<UserProfileDto>> response

        when: "The target method executed"
        response = userImpl.getProfile(username)

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.empty()
        0 * _

        and: "The expected result"
        response.statusCode == HttpStatus.UNAUTHORIZED
        noExceptionThrown()
    }

    def "test - getProfile: Success"() {
        given: "A valid userVO"
        String username = "Test"
        Role role = Role.ADMIN
        String firstName = "First"
        String lastName = "Last"
        String email = "test@example.com"
        UserVO userVO = new UserVO(username: username, role: role, firstName: firstName, lastName: lastName, email: email)
        ResponseEntity<GlobalResponse<UserProfileDto>> response

        when: "The target method executed"
        response = userImpl.getProfile(username)

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.of(userVO)
        0 * _

        and: "The expected result"
        response.statusCode == HttpStatus.OK
        response.body.responseData.username == username
        response.body.responseData.role == role.name()
        response.body.responseData.firstName == firstName
        noExceptionThrown()
    }

    @Unroll
    def "test - updatePassword: Validation failures [matchOld: #matchOld, conf: #conf, token: #token, expectedStatus: #expectedStatus]"() {
        given: "Password update request and existing userVO"
        String username = "Test"
        String oldPass = "old"
        String newPass = "new"
        String oldEnc = "oldEnc"
        String newEnc = "newEnc"
        UserVO userVO = new UserVO(username: username, password: oldEnc)
        PasswordUpdateRequestDto req = new PasswordUpdateRequestDto(oldPassword: oldPass, newPassword: newPass, confirmPassword: conf)
        ResponseEntity<GlobalResponse<Void>> response

        when: "The target method executed"
        response = userImpl.updatePassword(username, token, req)

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.of(userVO)
        if (matchOld) {
            1 * encoder.matches(oldPass, oldEnc) >> true
            if (req.newPassword == req.confirmPassword) {
                1 * validator.validate(userVO)
                1 * encoder.encode(newPass) >> newEnc
                if (token != null) {
                    1 * blacklistService.revokeToken(token, _)
                }
                1 * userRepo.save(userVO) >> userVO
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
        String username = "Test"
        UserVO userVO = new UserVO(username: username, password: "old")
        PasswordUpdateRequestDto req = new PasswordUpdateRequestDto(oldPassword: "old", newPassword: "new", confirmPassword: "new")

        when: "The target method executed"
        userImpl.updatePassword(username, null, req)

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.of(userVO)
        1 * encoder.matches(_, _) >> true
        1 * validator.validate(_)
        1 * encoder.encode(_) >> "enc"
        1 * userRepo.save(_) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        userVO.hasErrors()
        noExceptionThrown()
    }

    @Unroll
    def "test - updateUserRole: Invalid role validation New Role: #newRole"() {
        given: "A userVO and an invalid role"
        Integer userId = 1
        UserVO userVO = new UserVO(userID: userId, role: Role.MEMBER)
        ResponseEntity<GlobalResponse<Void>> response

        when: "The target method executed"
        response = userImpl.updateUserRole(userId, newRole)

        then: "The expected calls are made"
        1 * userRepo.findById(userId) >> Optional.of(userVO)
        1 * validator.isInvalidRole(newRole) >> true
        0 * _

        and: "The expected result"
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.globalMessages[0].message.contains(MessagesConstants.INVALID_ROLE.getMessageString())
        noExceptionThrown()

        where:
        newRole << ["", "   ", "INVALID_ROLE", null]
    }

    @Unroll
    def "test - updateUserRole: Protecting last active admin [role: #currentRole -> #targetRole, active: #active, onlyOne: #onlyOne, expectedStatus: #expectedStatus]"() {
        given: "A userVO setup"
        Integer userId = 1
        UserVO userVO = new UserVO(userID: userId, role: currentRole, active: active)
        ResponseEntity<GlobalResponse<Void>> response
        JpaSpecification capturedSpec
        int countCalls = (currentRole == Role.ADMIN && active && targetRole != Role.ADMIN.name()) ? 1 : 0
        int saveCalls = (expectedStatus == HttpStatus.OK) ? 1 : 0

        when: "The target method executed"
        response = userImpl.updateUserRole(userId, targetRole)

        then: "The expected calls are made"
        1 * userRepo.findById(userId) >> Optional.of(userVO)
        1 * validator.isInvalidRole(targetRole) >> false
        countCalls * userRepo.count(_ as JpaSpecification) >> { JpaSpecification spec ->
            capturedSpec = spec
            return (onlyOne ? 1 : 2)
        }
        saveCalls * userRepo.save(userVO) >> userVO
        0 * _

        and: "The expected result"
        response.statusCode == expectedStatus
        if (expectedStatus == HttpStatus.BAD_REQUEST) {
            assert response.body.globalMessages.any { it.message.contains("Cannot update role for the last active") }
        }
        if (countCalls > 0) {
            capturedSpec != null
            Map<String, List> inspection = inspectSpecification(capturedSpec)
            inspection.filters.containsAll([FieldConstants.ROLE, FieldConstants.ACTIVE])
            inspection.values.containsAll([Role.ADMIN, true])
        }
        noExceptionThrown()

        where:
        currentRole | active | onlyOne | targetRole         | expectedStatus
        Role.ADMIN  | true   | true    | Role.MEMBER.name() | HttpStatus.BAD_REQUEST   // Blocked: Last active admin
        Role.ADMIN  | true   | false   | Role.MEMBER.name() | HttpStatus.OK            // Allowed: Not the last one
        Role.ADMIN  | false  | true    | Role.MEMBER.name() | HttpStatus.OK            // Allowed: User is not active anyway
        Role.ADMIN  | true   | true    | Role.ADMIN.name()  | HttpStatus.OK            // Allowed: Role didn't actually change
        Role.MEMBER | true   | true    | Role.ADMIN.name()  | HttpStatus.OK            // Allowed: Upgrading to admin
    }

    @Unroll
    def "test - toggleUserStatus: Protecting last active admin [role: #role, active: #active -> #targetActive, onlyOne: #onlyOne, expectedStatus: #expectedStatus]"() {
        given: "A userVO setup"
        Integer userId = 1
        UserVO userVO = new UserVO(userID: userId, role: role, active: active)
        ResponseEntity<GlobalResponse<Void>> response
        int countCalls = (role == Role.ADMIN && !targetActive) ? 1 : 0
        int saveCalls = (expectedStatus == HttpStatus.OK) ? 1 : 0

        when: "The target method executed"
        response = userImpl.toggleUserStatus(userId, targetActive)

        then: "The expected calls are made"
        1 * userRepo.findById(userId) >> Optional.of(userVO)

        countCalls * userRepo.count(_ as JpaSpecification) >> { onlyOne ? 1 : 2 }

        saveCalls * userRepo.save(userVO) >> userVO
        0 * _

        and: "The expected result"
        response.statusCode == expectedStatus
        if (expectedStatus == HttpStatus.BAD_REQUEST) {
            assert response.body.globalMessages.any { it.message.contains("Cannot deactivate the last active ADMIN") }
        }
        noExceptionThrown()

        where:
        role        | active | targetActive | onlyOne | expectedStatus
        Role.ADMIN  | true   | false        | true    | HttpStatus.BAD_REQUEST   // Blocked: Last active admin
        Role.ADMIN  | true   | false        | false   | HttpStatus.OK            // Allowed: Not the last one
        Role.ADMIN  | false  | true         | true    | HttpStatus.OK            // Allowed: Activating an admin
        Role.MEMBER | true   | false        | true    | HttpStatus.OK            // Allowed: Not an admin
    }

    def "test - updateUserRole: DB Error"() {
        given: "A DB error scenario"
        Integer id = 1
        String newRole = Role.MEMBER.name()
        UserVO userVO = new UserVO(userID: id, role: Role.ADMIN, active: true)
        JpaSpecification capturedSpec

        when: "The target method executed"
        userImpl.updateUserRole(id, newRole)

        then: "The expected calls are made"
        1 * userRepo.findById(id) >> Optional.of(userVO)
        1 * validator.isInvalidRole(newRole) >> false
        1 * userRepo.count(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; 2 }
        1 * userRepo.save(_) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        userVO.hasErrors()
        noExceptionThrown()
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([FieldConstants.ROLE, FieldConstants.ACTIVE])
        inspection.values.containsAll([Role.ADMIN, true])
    }

    def "test - toggleUserStatus: DB Error"() {
        given: "A DB error scenario"
        Integer id = 1
        UserVO userVO = new UserVO(userID: id, role: Role.ADMIN, active: true)
        JpaSpecification capturedSpec

        when: "The target method executed"
        userImpl.toggleUserStatus(id, false)

        then: "The expected calls are made"
        1 * userRepo.findById(id) >> Optional.of(userVO)
        1 * userRepo.count(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; 2 }
        1 * userRepo.save(_) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        userVO.hasErrors()
        noExceptionThrown()
        capturedSpec != null
    }

    def "test - updatePassword: User not found"() {
        given: "A missing password update username"
        String username = "missing"
        String token = "token"

        when: "The target method executed"
        userImpl.updatePassword(username, token, new PasswordUpdateRequestDto())

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    @Unroll
    def "test - updateUserRole/Status: User not found [op: #op]"() {
        given: "A missing userVO ID for role/status change"
        Integer userId = 99

        when: "The target method executed"
        if (op == "role") userImpl.updateUserRole(userId, "ROLE")
        else userImpl.toggleUserStatus(userId, true)

        then: "The expected calls are made"
        1 * userRepo.findById(userId) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)

        where:
        op << ["role", "status"]
    }

    def "test - getAllUsers: Should return all users"() {
        given: "A list of users"
        String username1 = "user1"
        String username2 = "user2"
        UserVO userVO1 = new UserVO(username: username1, role: Role.ADMIN)
        UserVO userVO2 = new UserVO(username: username2, role: Role.MEMBER)
        List<UserVO> userList = [userVO1, userVO2]

        UserResponseDto dto1 = UserResponseDto.builder().username(username1).role(Role.ADMIN.name()).build()
        UserResponseDto dto2 = UserResponseDto.builder().username(username2).role(Role.MEMBER.name()).build()
        List<UserResponseDto> dtoList = [dto1, dto2]

        ResponseEntity<GlobalResponse<List<UserResponseDto>>> response

        when: "The target method executed"
        response = userImpl.getAllUsers()

        then: "The expected calls are made"
        1 * userRepo.findAll() >> userList
        1 * dtoTransformer.transformList(userList) >> dtoList
        0 * _

        and: "The expected result"
        response.statusCode == HttpStatus.OK
        response.body.responseData.size() == 2
        response.body.responseData[0].username == username1
        response.body.responseData[1].username == username2
        noExceptionThrown()
    }

    @Unroll
    def "test - adminUserExists: Should check for admin existence [exists: #exists]"() {
        given: "A repository state"
        JpaSpecification capturedSpec

        when: "The target method executed"
        boolean result = userImpl.adminUserExists()

        then: "The expected calls are made"
        1 * userRepo.exists(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; exists }
        0 * _

        and: "The expected result"
        result == exists
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([FieldConstants.ROLE])
        inspection.values.containsAll([Role.ADMIN])

        where:
        exists << [true, false]
    }
}