package org.sofumar.portal.service.validation

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.message.ValidationMessages
import org.sofumar.portal.constants.ReferenceConstants
import org.sofumar.portal.core.businesslogic.Reference
import org.sofumar.portal.framework.vo.ValueObject
import org.sofumar.portal.testbase.BaseSpecification
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