package org.sofumar.portal.core.businesslogic.impl

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.constants.Role
import org.sofumar.portal.core.repo.UserRepository
import org.sofumar.portal.core.vo.UserVO
import org.sofumar.portal.data.dto.UserDto
import org.sofumar.portal.data.dto.request.PasswordUpdateRequestDto
import org.sofumar.portal.data.dto.response.UserProfileDto
import org.sofumar.portal.data.dto.response.UserResponseDto
import org.sofumar.portal.data.transformer.UserResponseDtoTransformer
import org.sofumar.portal.data.transformer.UserVOTransformer
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.framework.exception.ValidationException
import org.sofumar.portal.framework.service.RefreshTokenService
import org.sofumar.portal.framework.service.TokenBlacklistService
import org.sofumar.portal.framework.util.MySQLConstraintResolver
import org.sofumar.portal.security.JwtService
import org.sofumar.portal.security.SofumarUserDetails
import org.sofumar.portal.service.validation.UserValidator
import org.sofumar.portal.testbase.BaseSpecification
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

    private UserVO createUserVO(Integer id = null) {
        def vo = new UserVO(userID: id, username: "user", email: "user@test.com", role: Role.MEMBER, active: true, password: "encodedPassword")
        vo.postLoad()
        return vo
    }

    def "test - register: Success"() {
        given: "A valid registration request"
        UserDto request = new UserDto(username: 'user', password: 'password', email: 'email@example.com')
        UserVO vo = new UserVO(username: 'user', password: 'password', email: 'email@example.com')
        UserVO savedVo = null

        when: "The target method executed"
        userImpl.register(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(request) >> vo
        1 * validator.validatePassword(vo)
        1 * encoder.encode('password') >> 'encodedPassword'
        1 * validator.validate(vo)
        2 * userRepo.exists(_ as JpaSpecification) >> false
        1 * userRepo.save(vo) >> {
            savedVo = it[0] as UserVO
            savedVo.userID = 1
            savedVo.postLoad()
            return savedVo
        }
        0 * _

        and: "The expected result"
        savedVo.password == 'encodedPassword'
        noExceptionThrown()
    }

    def "test - register: Validation failure (Username exists)"() {
        given: "A registration request with existing username"
        UserDto request = new UserDto(username: 'user', password: 'password', email: 'email@example.com')
        UserVO vo = new UserVO(username: 'user', password: 'password', email: 'email@example.com')

        when: "The target method executed"
        userImpl.register(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(request) >> vo
        1 * validator.validatePassword(vo)
        1 * encoder.encode('password') >> 'encodedPassword'
        1 * validator.validate(vo)
        2 * userRepo.exists(_ as JpaSpecification) >> { JpaSpecification spec -> true }
        0 * _

        and: "VO has validation errors"
        vo.hasErrors()
        thrown(ValidationException)
    }

    def "test - logout: Success (Revokes tokens if present)"() {
        given: "Active tokens"
        String accessToken = "access"
        String refreshToken = "refresh"

        when: "The target method executed"
        userImpl.logout(accessToken, refreshToken)

        then: "The expected calls are made"
        1 * jwtService.getRemainingExpirationSeconds(accessToken) >> 300L
        1 * blacklistService.revokeToken(accessToken, 300L)
        1 * refreshTokenService.deleteRefreshToken(refreshToken)
        0 * _
    }

    def "test - logout: Success (Handles null tokens)"() {
        when: "The target method executed with nulls"
        userImpl.logout(null, null)

        then: "No revokes called"
        0 * _
    }

    def "test - refreshToken: Success"() {
        given: "A valid refresh token"
        String token = "validRefresh"
        String rotatedToken = "rotatedToken"
        String username = "testuser"
        UserVO userVO = new UserVO(username: username, active: true, role: Role.MEMBER)
        String newAccess = "newAccess"

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> response = userImpl.refreshToken(token)

        then: "The expected calls are made"
        1 * refreshTokenService.rotateRefreshToken(token) >> rotatedToken
        1 * refreshTokenService.validateRefreshToken(rotatedToken) >> Optional.of(username)
        1 * userRepo.findOne(_ as JpaSpecification) >> Optional.of(userVO)
        1 * jwtService.generateAccessToken(_ as SofumarUserDetails) >> newAccess
        0 * _

        and: "Response contains new tokens in the map"
        response.statusCode == HttpStatus.OK
        response.body.map.token == newAccess
        response.body.map.refreshToken == rotatedToken
    }

    @Unroll
    def "test - refreshToken: Failure scenarios [reason: #reason]"() {
        given: "A refresh token"
        String token = "refreshToken"

        when: "The target method executed"
        ResponseEntity<?> response = userImpl.refreshToken(token)

        then: "The expected calls"
        if (reason == "Invalid") {
            1 * refreshTokenService.rotateRefreshToken(token) >> { throw new IllegalArgumentException() }
        } else if (reason == "NotFound") {
            1 * refreshTokenService.rotateRefreshToken(token) >> "rotated"
            1 * refreshTokenService.validateRefreshToken("rotated") >> Optional.of("u")
            1 * userRepo.findOne(_) >> Optional.empty()
        } else if (reason == "Inactive") {
            1 * refreshTokenService.rotateRefreshToken(token) >> "rotated"
            1 * refreshTokenService.validateRefreshToken("rotated") >> Optional.of("u")
            1 * userRepo.findOne(_) >> Optional.of(new UserVO(active: false, role: Role.MEMBER))
            1 * refreshTokenService.deleteRefreshToken("rotated")
        } else if (reason == "Locked") {
            1 * refreshTokenService.rotateRefreshToken(token) >> "rotated"
            1 * refreshTokenService.validateRefreshToken("rotated") >> Optional.of("u")
            UserVO lockedVO = new UserVO(active: true, role: Role.MEMBER, lockoutTime: LocalDateTime.now().plusHours(1))
            1 * userRepo.findOne(_) >> Optional.of(lockedVO)
            1 * refreshTokenService.deleteRefreshToken("rotated")
        }

        and: "UNAUTHORIZED response"
        response.statusCode == HttpStatus.UNAUTHORIZED

        where:
        reason << ["Invalid", "NotFound", "Inactive", "Locked"]

    }

    def "test - getProfile: Success"() {
        given: "A valid username"
        String username = "u"
        UserVO vo = new UserVO(username: username, role: Role.ADMIN, firstName: "F", lastName: "L", email: "E")

        when: "The target method executed"
        ResponseEntity<GlobalResponse<UserProfileDto>> response = userImpl.getProfile(username)

        then: "The expected calls are made"
        1 * userRepo.findOne(_ as JpaSpecification) >> Optional.of(vo)
        0 * _

        and: "Profile data is correct"
        response.body.responseData.username == username
        response.body.responseData.role == "ADMIN"
    }

    def "test - getProfile: Not Found"() {
        given: "An unknown username"
        String username = "u"

        when: "The target method executed"
        ResponseEntity<GlobalResponse<UserProfileDto>> response = userImpl.getProfile(username)

        then: "The expected calls"
        1 * userRepo.findOne(_) >> Optional.empty()
        0 * _

        and: "UNAUTHORIZED response"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "test - updatePassword: Success"() {
        given: "A valid request"
        String username = "u"; String token = "t"
        PasswordUpdateRequestDto request = new PasswordUpdateRequestDto(oldPassword: 'o', newPassword: 'n', confirmPassword: 'n')
        UserVO vo = createUserVO(1)
        vo.username = username
        vo.password = 'encodedOld'
        vo.postLoad()

        when: "The target method executed"
        userImpl.updatePassword(username, token, request)

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.of(vo)
        1 * encoder.matches('o', 'encodedOld') >> true
        1 * validator.validatePassword(vo)
        1 * encoder.encode('n') >> 'encodedNew'
        1 * userRepo.findById(1) >> Optional.of(vo)
        1 * validator.validate(vo)
        2 * userRepo.exists(_ as JpaSpecification) >> false
        1 * userRepo.save(vo) >> vo
        1 * blacklistService.revokeToken(token, 3600L)
        0 * _
    }

    @Unroll
    def "test - updatePassword: Validation failure [case: #failCase]"() {
        given: "A request"
        PasswordUpdateRequestDto request = new PasswordUpdateRequestDto(oldPassword: 'o', newPassword: 'n', confirmPassword: confirm)
        UserVO vo = new UserVO(userID: 1, password: 'encodedOld')

        when: "The target method executed"
        ResponseEntity<GlobalResponse<Void>> response = userImpl.updatePassword("u", "t", request)

        then: "The expected calls"
        1 * userRepo.findOne(_) >> Optional.of(vo)
        1 * encoder.matches('o', 'encodedOld') >> (failCase != "WrongOld")
        0 * _

        and: "Bad Request"
        response.statusCode == HttpStatus.BAD_REQUEST

        where:
        failCase   | confirm
        "WrongOld" | 'n'
        "Mismatch" | 'x'
    }

    def "test - getAllUsers: Success"() {
        given: "Users in repo"
        List<UserVO> users = [new UserVO(username: 'u1')]
        List<UserResponseDto> dtos = [UserResponseDto.builder().username('u1').build()]

        when: "The target method executed"
        ResponseEntity<GlobalResponse<List<UserResponseDto>>> response = userImpl.getAllUsers()

        then: "The expected calls"
        1 * userRepo.findAll() >> users
        1 * dtoTransformer.transformList(users) >> dtos
        0 * _

        and: "Result is correct"
        response.body.responseData == dtos
    }

    @Unroll
    def "test - updateUserRole: Success [from: #from, to: #to]"() {
        given: "A user VO"
        Integer userId = 1
        UserVO vo = createUserVO(userId)
        vo.role = from

        when: "Role is updated"
        userImpl.updateUserRole(userId, to.name())

        then: "The expected calls are made"
        1 * userRepo.findById(userId) >> Optional.of(vo)
        1 * userRepo.findById(userId) >> Optional.of(new UserVO(userID: userId, role: from, active: true))
        if (from == Role.ADMIN && to != Role.ADMIN) {
            1 * userRepo.count(_ as JpaSpecification) >> 2
        } else {
            0 * userRepo.count(_)
        }
        1 * validator.validate(vo)
        2 * userRepo.exists(_ as JpaSpecification) >> false
        1 * userRepo.save(vo) >> vo
        0 * _

        and: "Role is changed"
        vo.role == to

        where:
        from        | to
        Role.ADMIN  | Role.ADMIN
        Role.MEMBER | Role.ADMIN
        Role.ADMIN  | Role.MEMBER
    }

    def "test - updateUserRole: Invalid Role"() {
        given: "A user and an invalid role string"
        Integer userId = 1
        UserVO vo = createUserVO(userId)

        when: "updateUserRole is called with invalid role"
        userImpl.updateUserRole(userId, "INVALID_ROLE")

        then: "Initial find successfully returns VO, but update fails due to invalid role"
        1 * userRepo.findById(userId) >> Optional.of(vo)
        thrown(ValidationException)
        0 * _

        and: "VO has validation error"
        vo.hasErrors()
        vo.fieldMessages.containsKey(FieldConstants.ROLE)
    }

    def "test - updateUserRole: Last Admin Protection"() {
        given: "The last active admin"
        Integer userId = 1
        UserVO vo = createUserVO(userId)
        vo.role = Role.ADMIN

        when: "Trying to demote"
        userImpl.updateUserRole(userId, Role.MEMBER.name())

        then: "ValidationException is thrown"
        1 * userRepo.findById(userId) >> Optional.of(vo)
        1 * userRepo.findById(userId) >> Optional.of(new UserVO(userID: userId, role: Role.ADMIN, active: true))
        1 * userRepo.count(_ as JpaSpecification) >> 1
        1 * validator.validate(vo)
        2 * userRepo.exists(_ as JpaSpecification) >> false
        thrown(ValidationException)
        0 * _

        and: "VO has validation errors"
        vo.hasErrors()
    }

    @Unroll
    def "test - toggleUserStatus: Success [active: #active -> #target]"() {
        given: "A user VO"
        Integer userId = 1
        UserVO vo = createUserVO(userId)
        vo.role = Role.ADMIN
        vo.active = active

        when: "Status is toggled"
        userImpl.toggleUserStatus(userId, target)

        then: "The expected calls are made"
        1 * userRepo.findById(userId) >> Optional.of(vo)
        1 * userRepo.findById(userId) >> Optional.of(new UserVO(userID: userId, role: Role.ADMIN, active: active))
        if (active && !target) {
            1 * userRepo.count(_ as JpaSpecification) >> 2
        } else {
            0 * userRepo.count(_)
        }
        1 * validator.validate(vo)
        2 * userRepo.exists(_ as JpaSpecification) >> false
        1 * userRepo.save(vo) >> vo
        0 * _

        and: "Status is changed"
        vo.active == target

        where:
        active | target
        false  | true
        true   | false
    }

    def "test - toggleUserStatus: Last Admin Deactivation Protection"() {
        given: "The last active admin"
        Integer userId = 1
        UserVO vo = createUserVO(userId)
        vo.role = Role.ADMIN
        vo.active = true

        when: "Trying to deactivate"
        userImpl.toggleUserStatus(userId, false)

        then: "ValidationException is thrown"
        1 * userRepo.findById(userId) >> Optional.of(vo)
        1 * userRepo.findById(userId) >> Optional.of(new UserVO(userID: userId, role: Role.ADMIN, active: true))
        1 * userRepo.count(_ as JpaSpecification) >> 1
        1 * validator.validate(vo)
        2 * userRepo.exists(_ as JpaSpecification) >> false
        thrown(ValidationException)
        0 * _

        and: "VO has validation errors"
        vo.hasErrors()
    }

    def "test - adminUserExists: Should check repo"() {
        when: "The target method executed"
        userImpl.adminUserExists()

        then: "The expected calls"
        1 * userRepo.exists(_ as JpaSpecification) >> true
        0 * _
    }

    def "test - findUserForAuthentication: Success"() {
        given: "A username"
        UserVO vo = new UserVO(username: "u")

        when: "Searching"
        UserVO result = userImpl.findUserForAuthentication("u")

        then: "The expected calls"
        1 * userRepo.findOne(_ as JpaSpecification) >> Optional.of(vo)
        0 * _

        and: "User is returned"
        result == vo
    }

    @Unroll
    def "test - onLoginSuccess: Resets state if needed [failures: #fail, locked: #locked]"() {
        given: "A user with some failed state"
        UserVO vo = createUserVO(1)
        vo.failedLoginAttempts = fail
        vo.lockoutTime = locked ? LocalDateTime.now() : null

        when: "Login succeeds"
        userImpl.onLoginSuccess("u")

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.of(vo)
        if (fail > 0 || locked) {
            1 * userRepo.findById(1) >> Optional.of(new UserVO(userID: 1, username: "u", email: "e", role: Role.MEMBER, active: true))
            1 * validator.validate(vo)
            2 * userRepo.exists(_ as JpaSpecification) >> false
            1 * userRepo.save(vo) >> vo
        }
        0 * _

        and: "State is reset"
        vo.failedLoginAttempts == 0
        vo.lockoutTime == null

        where:
        fail | locked
        3    | true
        0    | true
        3    | false
        0    | false
    }

    @Unroll
    def "test - onLoginFailure: State update [failCount: #count -> locked: #isLocked]"() {
        given: "A user"
        UserVO vo = createUserVO(1)
        vo.failedLoginAttempts = count

        when: "Login fails"
        userImpl.onLoginFailure("u")

        then: "The expected calls are made"
        1 * userRepo.findOne(_) >> Optional.of(vo)
        1 * userRepo.findById(1) >> Optional.of(new UserVO(userID: 1, username: "u", email: "e", role: Role.MEMBER, active: true))
        1 * validator.validate(vo)
        2 * userRepo.exists(_ as JpaSpecification) >> false
        1 * userRepo.save(vo) >> vo
        0 * _

        and: "State updated correctly"
        vo.failedLoginAttempts == count + 1
        (vo.lockoutTime != null) == isLocked

        where:
        count | isLocked
        0     | false
        4     | true
    }

    def "test - beforeUpdate: Password hashing"() {
        given: "A user VO with a new password"
        UserVO vo = new UserVO(password: "newPass", persistedPassword: "old")

        when: "Reflectively calling beforeUpdate"
        userImpl.beforeUpdate(vo)

        then: "The expected calls"
        1 * encoder.encode("newPass") >> "hashed"
        0 * _

        and: "Password is hashed and update time set"
        vo.password == "hashed"
        vo.passwordUpdatedAt != null
    }

}