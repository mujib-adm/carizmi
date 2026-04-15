package io.carizmi.domain.platform.validation

import io.carizmi.shared.constants.FieldConstants
import io.carizmi.shared.message.ValidationMessages
import io.carizmi.shared.constants.ReferenceConstants
import io.carizmi.domain.platform.service.Reference
import io.carizmi.framework.vo.ValueObject
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject
import spock.lang.Unroll

class ReferenceValidatorSpec extends BaseSpecification {

    Reference referenceService = Mock()

    @Subject
    ReferenceValidator referenceValidator = new ReferenceValidator(referenceService)

    @Unroll
    def "test - validate: Handling reference validation [code: #code, isValid: #isValid]"() {
        given: "A value object and validation parameters"
        ValueObject vo = new ValueObject() {
            @Override
            String getTableName() { "DUMMY" }
        }
        String fieldName = FieldConstants.FEE_TYPE
        String referenceName = ReferenceConstants.FEE_TYPE.NAME

        when: "The target method executed"
        referenceValidator.validate(vo, fieldName, referenceName, code)

        then: "The expected calls are made"
        if (code != null && !code.blank) {
            1 * referenceService.isValidReference(referenceName, code) >> isValid
        }
        0 * _

        and: "The expected result"
        if (code != null && !code.blank && !isValid) {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(fieldName)
            vo.getFieldMessages().get(fieldName).contains(ValidationMessages.INVALID_VALUE)
        } else {
            !vo.hasErrors()
        }
        noExceptionThrown()

        where:
        code                                         | isValid
        ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE   | true
        ReferenceConstants.FEE_TYPE.REGISTRATION_FEE | true
        "INVALID"                                    | false
        ""                                           | false
        null                                         | false
    }
}