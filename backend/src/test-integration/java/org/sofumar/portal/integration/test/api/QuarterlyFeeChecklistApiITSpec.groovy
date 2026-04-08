package org.sofumar.portal.integration.test.api

import org.sofumar.portal.constants.QuarterCellStatus
import org.sofumar.portal.constants.ReferenceConstants
import org.sofumar.portal.constants.SystemSettingConstants
import org.sofumar.portal.core.repo.MemberRepository
import org.sofumar.portal.core.repo.PaymentRepository
import org.sofumar.portal.core.repo.SystemSettingRepository
import org.sofumar.portal.core.vo.MemberVO
import org.sofumar.portal.core.vo.PaymentVO
import org.sofumar.portal.core.vo.SystemSettingsVO
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.integration.base.BaseIntegrationSpecification
import org.sofumar.portal.integration.constants.ApiEndpoints
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

import java.time.LocalDate

class QuarterlyFeeChecklistApiITSpec extends BaseIntegrationSpecification {

    @Autowired
    MemberRepository memberRepository

    @Autowired
    PaymentRepository paymentRepository

    @Autowired
    SystemSettingRepository systemSettingRepository

    def setup() {
        login()
    }

    def cleanup() {
        paymentRepository.deleteAll()
        memberRepository.deleteAll()
    }

    // ---------------------------------------------------------------------------
    // Test 1: Core computation — past year with mixed payment scenarios
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - Past year with mixed payments verifies per-row and summary calculations"() {
        given: "seed test members for a past year where all 4 quarters are assessable"
        int testYear = LocalDate.now().getYear() - 1

        MemberVO m1 = memberRepository.save(new MemberVO(
                firstName: "Alice", lastName: "Adams",
                phone: "612-555-0001", email: "alice@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(testYear - 1, 1, 1),
                state: "MN"
        ))
        MemberVO m2 = memberRepository.save(new MemberVO(
                firstName: "Bob", lastName: "Baker",
                phone: "612-555-0002", email: "bob@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(testYear - 1, 6, 1),
                state: "MN"
        ))

        and: "Alice paid all 4 quarters (60 each)"
        (1..4).each { q ->
            paymentRepository.save(new PaymentVO(
                    member: m1, feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                    amount: new BigDecimal("60.00"),
                    dateReceived: LocalDate.of(testYear, q * 3, 1),
                    year: testYear, quarter: q,
                    methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
            ))
        }

        and: "Bob paid Q1 and Q3 only (missing Q2 and Q4)"
        [1, 3].each { q ->
            paymentRepository.save(new PaymentVO(
                    member: m2, feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                    amount: new BigDecimal("60.00"),
                    dateReceived: LocalDate.of(testYear, q * 3, 1),
                    year: testYear, quarter: q,
                    methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
            ))
        }

        when: "authenticated POST /checklist/quarterly-fee"
        Map<String, Object> searchRequest = [year: testYear, page: 0, size: 10]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "response is 200 OK"
        response.statusCode == HttpStatus.OK
        response.body.responseData != null

        and: "checklist metadata is correct"
        Map data = response.body.responseData as Map
        data.year == testYear
        (data.quarterlyFeeAmount as Number).doubleValue() == 60.0d
        data.currentQuarter == 4   // past year => all 4 assessable

        and: "rows contain our seeded members"
        List<Map> rows = data.rows as List<Map>
        rows.size() >= 2

        // Locate Alice and Bob rows by name
        Map aliceRow = rows.find { it.memberName == "Alice Adams" }
        Map bobRow = rows.find { it.memberName == "Bob Baker" }
        aliceRow != null
        bobRow != null

        and: "Alice — paid all 4 quarters: totalPaid=240, balance=0"
        (aliceRow.totalPaid as Number).doubleValue() == 240.0d
        (aliceRow.balance as Number).doubleValue() == 0.0d
        List<Map> aliceQuarters = aliceRow.quarters as List<Map>
        aliceQuarters.size() == 4
        aliceQuarters.every { it.status == QuarterCellStatus.PAID.name() }

        and: "Bob — paid Q1,Q3: totalPaid=120, balance=120"
        (bobRow.totalPaid as Number).doubleValue() == 120.0d
        (bobRow.balance as Number).doubleValue() == 120.0d
        List<Map> bobQuarters = bobRow.quarters as List<Map>
        bobQuarters[0].status == QuarterCellStatus.PAID.name()     // Q1 paid
        bobQuarters[1].status == QuarterCellStatus.UNPAID.name()   // Q2 unpaid
        bobQuarters[2].status == QuarterCellStatus.PAID.name()     // Q3 paid
        bobQuarters[3].status == QuarterCellStatus.UNPAID.name()   // Q4 unpaid

        and: "global summary is correct"
        Map summary = data.summary as Map
        (summary.totalPaid as Number).doubleValue() == 360.0d     // 240 + 120
        (summary.totalBalance as Number).doubleValue() == 120.0d  // 0 + 120

        List<Map> qs = summary.quarterSummaries as List<Map>
        qs.size() == 4
        // Q1: both paid
        (qs[0].paidCount as Number).intValue() == 2
        (qs[0].unpaidCount as Number).intValue() == 0
        qs[0].future == false
        // Q2: Alice paid, Bob unpaid
        (qs[1].paidCount as Number).intValue() == 1
        (qs[1].unpaidCount as Number).intValue() == 1
        // Q3: both paid
        (qs[2].paidCount as Number).intValue() == 2
        (qs[2].unpaidCount as Number).intValue() == 0
        // Q4: Alice paid, Bob unpaid
        (qs[3].paidCount as Number).intValue() == 1
        (qs[3].unpaidCount as Number).intValue() == 1
        qs[3].future == false
    }

    // ---------------------------------------------------------------------------
    // Test 2: Member joined mid-year — Q1 should be NOT_APPLICABLE
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - Member joined mid-year has NOT_APPLICABLE for pre-join quarters"() {
        given: "a member who joined in Q2 of the test year"
        int testYear = LocalDate.now().getYear() - 1

        MemberVO m = memberRepository.save(new MemberVO(
                firstName: "Charlie", lastName: "Clark",
                phone: "612-555-0003", email: "charlie@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(testYear, 4, 15),   // Q2
                state: "MN"
        ))

        and: "Charlie paid Q2 only"
        paymentRepository.save(new PaymentVO(
                member: m, feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("60.00"),
                dateReceived: LocalDate.of(testYear, 6, 1),
                year: testYear, quarter: 2,
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
        ))

        when: "authenticated POST /checklist/quarterly-fee"
        Map<String, Object> searchRequest = [year: testYear, page: 0, size: 10]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "response is 200 OK"
        response.statusCode == HttpStatus.OK

        and: "Charlie's quarter statuses reflect his mid-year join"
        Map data = response.body.responseData as Map
        List<Map> rows = data.rows as List<Map>
        Map charlieRow = rows.find { it.memberName == "Charlie Clark" }
        charlieRow != null

        List<Map> quarters = charlieRow.quarters as List<Map>
        quarters[0].status == QuarterCellStatus.NOT_APPLICABLE.name()  // Q1 - before join
        quarters[1].status == QuarterCellStatus.PAID.name()            // Q2 - paid
        quarters[2].status == QuarterCellStatus.UNPAID.name()          // Q3 - unpaid
        quarters[3].status == QuarterCellStatus.UNPAID.name()          // Q4 - unpaid

        and: "balance covers only eligible quarters (Q2-Q4 = 3 x 60 = 180, paid 60 → balance 120)"
        (charlieRow.totalPaid as Number).doubleValue() == 60.0d
        (charlieRow.balance as Number).doubleValue() == 120.0d

        and: "global summary reflects NOT_APPLICABLE correctly"
        Map summary = data.summary as Map
        List<Map> qs = summary.quarterSummaries as List<Map>
        (qs[0].paidCount as Number).intValue() == 0    // Q1: not applicable, not counted
        (qs[0].unpaidCount as Number).intValue() == 0
    }

    // ---------------------------------------------------------------------------
    // Test 3: Future year — all quarters marked FUTURE, totals are zero
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - Future year returns all FUTURE quarters with zero totals"() {
        given: "a seeded member"
        int futureYear = LocalDate.now().getYear() + 1

        memberRepository.save(new MemberVO(
                firstName: "Diana", lastName: "Davis",
                phone: "612-555-0004", email: "diana@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(futureYear - 2, 1, 1),
                state: "MN"
        ))

        when: "requesting the checklist for a future year"
        Map<String, Object> searchRequest = [year: futureYear, page: 0, size: 10]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "response is 200 OK"
        response.statusCode == HttpStatus.OK

        and: "all quarters are FUTURE"
        Map data = response.body.responseData as Map
        data.currentQuarter == 0   // future year => assessableQuarter = 0

        List<Map> rows = data.rows as List<Map>
        Map dianaRow = rows.find { it.memberName == "Diana Davis" }
        dianaRow != null

        List<Map> quarters = dianaRow.quarters as List<Map>
        quarters.every { it.status == QuarterCellStatus.FUTURE.name() }

        and: "totalPaid and balance are zero"
        (dianaRow.totalPaid as Number).doubleValue() == 0.0d
        (dianaRow.balance as Number).doubleValue() == 0.0d

        and: "global summary quarter counts are all zero and marked future"
        Map summary = data.summary as Map
        List<Map> qs = summary.quarterSummaries as List<Map>
        qs.every { (it.paidCount as Number).intValue() == 0 }
        qs.every { (it.unpaidCount as Number).intValue() == 0 }
        qs.every { it.future == true }
        (summary.totalPaid as Number).doubleValue() == 0.0d
        (summary.totalBalance as Number).doubleValue() == 0.0d
    }

    // ---------------------------------------------------------------------------
    // Test 4: Pagination — verifies page/size params and meta
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - Pagination returns correct page meta and row count"() {
        given: "5 seeded members"
        int testYear = LocalDate.now().getYear() - 1
        (1..5).each { i ->
            memberRepository.save(new MemberVO(
                    firstName: "Pager${i}", lastName: "Test",
                    phone: "612-555-100${i}", email: "pager${i}@test.com",
                    status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                    joinDate: LocalDate.of(testYear - 1, 1, 1),
                    state: "MN"
            ))
        }

        when: "requesting page 0 with size 2"
        Map<String, Object> searchRequest = [year: testYear, page: 0, size: 2]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "response is 200 OK"
        response.statusCode == HttpStatus.OK

        and: "pagination meta is correct"
        Map meta = response.body.meta as Map
        (meta.page as Number).intValue() == 0
        (meta.pageSize as Number).intValue() == 2
        (meta.totalRecords as Number).longValue() >= 5
        (meta.totalPages as Number).intValue() >= 3

        and: "page contains at most 2 rows"
        Map data = response.body.responseData as Map
        List<Map> rows = data.rows as List<Map>
        rows.size() == 2

        when: "requesting the last page"
        int lastPage = (meta.totalPages as Number).intValue() - 1
        Map<String, Object> lastPageRequest = [year: testYear, page: lastPage, size: 2]
        ResponseEntity<GlobalResponse> lastPageResponse = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(lastPageRequest, authHeaders),
                GlobalResponse
        )

        then: "last page returns remaining rows"
        lastPageResponse.statusCode == HttpStatus.OK
        Map lastData = lastPageResponse.body.responseData as Map
        List<Map> lastRows = lastData.rows as List<Map>
        lastRows.size() >= 1
    }

    // ---------------------------------------------------------------------------
    // Test 5: No active members — empty rows with zero summary
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - No active members returns empty rows and zero summary"() {
        given: "no members in the system (cleanup already ran)"
        int testYear = LocalDate.now().getYear() - 1

        when: "requesting the checklist"
        Map<String, Object> searchRequest = [year: testYear, page: 0, size: 10]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "response is 200 OK with empty rows"
        response.statusCode == HttpStatus.OK
        Map data = response.body.responseData as Map
        List<Map> rows = data.rows as List<Map>
        rows.isEmpty()

        and: "summary totals are zero"
        Map summary = data.summary as Map
        (summary.totalPaid as Number).doubleValue() == 0.0d
        (summary.totalBalance as Number).doubleValue() == 0.0d
    }

    // ---------------------------------------------------------------------------
    // Test 6: Overpayment — balance clamped to zero (no negative balance)
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - Overpayment clamps balance to zero"() {
        given: "a member who overpaid Q1"
        int testYear = LocalDate.now().getYear() - 1

        MemberVO m = memberRepository.save(new MemberVO(
                firstName: "Eve", lastName: "Evans",
                phone: "612-555-0005", email: "eve@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(testYear - 1, 1, 1),
                state: "MN"
        ))

        and: "Eve paid 500 for Q1 (way more than 60)"
        paymentRepository.save(new PaymentVO(
                member: m, feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("500.00"),
                dateReceived: LocalDate.of(testYear, 1, 15),
                year: testYear, quarter: 1,
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
        ))

        when: "requesting the checklist"
        Map<String, Object> searchRequest = [year: testYear, page: 0, size: 10]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "response is 200 OK"
        response.statusCode == HttpStatus.OK

        and: "Eve's Q1 = PAID (overpaid), remaining quarters = UNPAID"
        Map data = response.body.responseData as Map
        List<Map> rows = data.rows as List<Map>
        Map eveRow = rows.find { it.memberName == "Eve Evans" }
        eveRow != null

        List<Map> quarters = eveRow.quarters as List<Map>
        quarters[0].status == QuarterCellStatus.PAID.name()
        quarters[1].status == QuarterCellStatus.UNPAID.name()
        quarters[2].status == QuarterCellStatus.UNPAID.name()
        quarters[3].status == QuarterCellStatus.UNPAID.name()

        and: "totalPaid=500, but balance is clamped — max(4x60 - 500, 0) = max(-260, 0) = 0"
        (eveRow.totalPaid as Number).doubleValue() == 500.0d
        (eveRow.balance as Number).doubleValue() == 0.0d
    }

    // ---------------------------------------------------------------------------
    // Test 7: Default year — null year defaults to current year
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - Null year defaults to current year"() {
        given: "a seeded member"
        memberRepository.save(new MemberVO(
                firstName: "Frank", lastName: "Fisher",
                phone: "612-555-0006", email: "frank@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(2020, 1, 1),
                state: "MN"
        ))

        when: "requesting the checklist without specifying a year"
        Map<String, Object> searchRequest = [page: 0, size: 10]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "response is 200 OK and year defaults to the current year"
        response.statusCode == HttpStatus.OK
        Map data = response.body.responseData as Map
        (data.year as Number).intValue() == LocalDate.now().getYear()
    }

    // ---------------------------------------------------------------------------
    // Test 8: Current year — future quarters correctly marked FUTURE
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - Current year marks future quarters as FUTURE"() {
        given: "a member who joined long ago"
        int currentYear = LocalDate.now().getYear()
        int currentQuarter = ((LocalDate.now().getMonthValue() - 1) / 3 + 1) as int

        memberRepository.save(new MemberVO(
                firstName: "Grace", lastName: "Green",
                phone: "612-555-0007", email: "grace@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(2020, 1, 1),
                state: "MN"
        ))

        when: "requesting the checklist for the current year"
        Map<String, Object> searchRequest = [year: currentYear, page: 0, size: 10]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "response is 200 OK"
        response.statusCode == HttpStatus.OK

        and: "assessable quarter matches the current quarter"
        Map data = response.body.responseData as Map
        (data.currentQuarter as Number).intValue() == currentQuarter

        and: "past/current quarters are UNPAID or PAID; future quarters are FUTURE"
        List<Map> rows = data.rows as List<Map>
        Map graceRow = rows.find { it.memberName == "Grace Green" }
        graceRow != null

        List<Map> quarters = graceRow.quarters as List<Map>
        (1..4).each { q ->
            if (q > currentQuarter) {
                assert quarters[q - 1].status == QuarterCellStatus.FUTURE.name()
            } else {
                assert quarters[q - 1].status != QuarterCellStatus.FUTURE.name()
                assert quarters[q - 1].status != QuarterCellStatus.NOT_APPLICABLE.name()
            }
        }

        and: "global summary marks future quarters correctly"
        Map summary = data.summary as Map
        List<Map> qs = summary.quarterSummaries as List<Map>
        (1..4).each { q ->
            assert qs[q - 1].future == (q > currentQuarter)
        }
    }

    // ---------------------------------------------------------------------------
    // Test 9: Dynamic fee amount — verifies the centralized SystemSetting accessor
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - Reads dynamic fee amount from system settings"() {
        given: "the seeded MEMBERSHIP_FEE is 60 (from systemsettings-data.json)"
        int testYear = LocalDate.now().getYear() - 1

        MemberVO m = memberRepository.save(new MemberVO(
                firstName: "Helen", lastName: "Hill",
                phone: "612-555-0008", email: "helen@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(testYear - 1, 1, 1),
                state: "MN"
        ))

        and: "Helen paid 60 for Q1"
        paymentRepository.save(new PaymentVO(
                member: m, feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("60.00"),
                dateReceived: LocalDate.of(testYear, 2, 1),
                year: testYear, quarter: 1,
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
        ))

        when: "requesting the checklist"
        Map<String, Object> searchRequest = [year: testYear, page: 0, size: 10]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "the checklist uses the dynamic fee amount of 60"
        response.statusCode == HttpStatus.OK
        Map data = response.body.responseData as Map
        (data.quarterlyFeeAmount as Number).doubleValue() == 60.0d

        and: "Helen: totalPaid=60, balance = 4x60 - 60 = 180"
        List<Map> rows = data.rows as List<Map>
        Map helenRow = rows.find { it.memberName == "Helen Hill" }
        (helenRow.totalPaid as Number).doubleValue() == 60.0d
        (helenRow.balance as Number).doubleValue() == 180.0d
    }

    // ---------------------------------------------------------------------------
    // Test 10: Missing MEMBERSHIP_FEE setting — fails fast with 503
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - Missing MEMBERSHIP_FEE returns 503 Service Unavailable"() {
        given: "backup and delete the MEMBERSHIP_FEE setting"
        SystemSettingsVO feeSetting = systemSettingRepository.findAll().find {
            it.settingKey == SystemSettingConstants.FEE.MEMBERSHIP_FEE
        }
        String originalValue = feeSetting?.settingValue
        if (feeSetting) {
            systemSettingRepository.delete(feeSetting)
            systemSettingRepository.flush()
        }

        when: "requesting the checklist without a MEMBERSHIP_FEE in the system"
        Map<String, Object> searchRequest = [year: LocalDate.now().getYear() - 1, page: 0, size: 10]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "response is 503 Service Unavailable (fail-fast)"
        response.statusCode == HttpStatus.SERVICE_UNAVAILABLE

        cleanup: "restore the MEMBERSHIP_FEE setting"
        if (feeSetting) {
            systemSettingRepository.save(new SystemSettingsVO(
                    settingName: feeSetting.settingName,
                    settingKey: feeSetting.settingKey,
                    settingValue: originalValue,
                    effectiveDate: feeSetting.effectiveDate,
                    active: feeSetting.active
            ))
            systemSettingRepository.flush()
        }
    }

    // ---------------------------------------------------------------------------
    // Test 11: Unauthenticated access returns 401
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee without auth returns 401"() {
        when: "anonymous access"
        ResponseEntity<GlobalResponse> response = restTemplate.postForEntity(
                ApiEndpoints.Checklist.QUARTERLY_FEE, [year: 2026], GlobalResponse
        )

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    // ---------------------------------------------------------------------------
    // Test 12: Partial payment — amount < fee marks quarter as UNPAID
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - Partial payment marks quarter as UNPAID"() {
        given: "a member with a partial Q1 payment"
        int testYear = LocalDate.now().getYear() - 1

        MemberVO m = memberRepository.save(new MemberVO(
                firstName: "Ivan", lastName: "Ivanov",
                phone: "612-555-0009", email: "ivan@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(testYear - 1, 1, 1),
                state: "MN"
        ))

        and: "Ivan paid 30 for Q1 (less than 60 fee)"
        paymentRepository.save(new PaymentVO(
                member: m, feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("30.00"),
                dateReceived: LocalDate.of(testYear, 1, 15),
                year: testYear, quarter: 1,
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
        ))

        when: "requesting the checklist"
        Map<String, Object> searchRequest = [year: testYear, page: 0, size: 10]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "response is 200 OK"
        response.statusCode == HttpStatus.OK

        and: "Q1 is UNPAID because 30 < 60 fee"
        Map data = response.body.responseData as Map
        List<Map> rows = data.rows as List<Map>
        Map ivanRow = rows.find { it.memberName == "Ivan Ivanov" }
        ivanRow != null

        List<Map> quarters = ivanRow.quarters as List<Map>
        quarters[0].status == QuarterCellStatus.UNPAID.name()
        (quarters[0].amountPaid as Number).doubleValue() == 30.0d

        and: "balance = 4x60 - 30 = 210"
        (ivanRow.totalPaid as Number).doubleValue() == 30.0d
        (ivanRow.balance as Number).doubleValue() == 210.0d
    }

    // ---------------------------------------------------------------------------
    // Test 13: Member joined in current year — pre-join quarters NOT_APPLICABLE
    // ---------------------------------------------------------------------------
    def "POST /checklist/quarterly-fee - Member joined in Q3 of requested year has Q1/Q2 as NOT_APPLICABLE"() {
        given: "a member who joined in Q3"
        int testYear = LocalDate.now().getYear() - 1

        MemberVO m = memberRepository.save(new MemberVO(
                firstName: "Julia", lastName: "Jones",
                phone: "612-555-0010", email: "julia@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(testYear, 7, 1),   // Q3
                state: "MN"
        ))

        when: "requesting the checklist"
        Map<String, Object> searchRequest = [year: testYear, page: 0, size: 10]
        ResponseEntity<GlobalResponse> response = restTemplate.exchange(
                ApiEndpoints.Checklist.QUARTERLY_FEE,
                HttpMethod.POST,
                new HttpEntity<>(searchRequest, authHeaders),
                GlobalResponse
        )

        then: "response is 200 OK"
        response.statusCode == HttpStatus.OK

        and: "Q1 and Q2 are NOT_APPLICABLE, Q3 and Q4 are UNPAID"
        Map data = response.body.responseData as Map
        List<Map> rows = data.rows as List<Map>
        Map juliaRow = rows.find { it.memberName == "Julia Jones" }
        juliaRow != null

        List<Map> quarters = juliaRow.quarters as List<Map>
        quarters[0].status == QuarterCellStatus.NOT_APPLICABLE.name()  // Q1
        quarters[1].status == QuarterCellStatus.NOT_APPLICABLE.name()  // Q2
        quarters[2].status == QuarterCellStatus.UNPAID.name()          // Q3
        quarters[3].status == QuarterCellStatus.UNPAID.name()          // Q4

        and: "balance covers only eligible quarters: 2 x 60 = 120"
        (juliaRow.totalPaid as Number).doubleValue() == 0.0d
        (juliaRow.balance as Number).doubleValue() == 120.0d
    }
}