package org.sofumar.portal.testsupport

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import spock.lang.Specification
import org.springframework.data.jpa.domain.Specification as JpaSpecification

class BaseSpecification extends Specification {

    Map<String, List> inspectSpecification(JpaSpecification jpaSpec) {
        def cb = Mock(CriteriaBuilder)
        def root = Mock(Root)
        def path = Mock(Path)
        def expression = Mock(Expression)

        def fields = []
        def values = []

        // Recursive path handling for nested properties (e.g. member.memberID)
        // This ensures root.get() returns path, and path.get() also returns path (capturing both fields)
        root.get(_ as String) >> { String f -> fields << f; path }
        path.get(_ as String) >> { String f -> fields << f; path }

        // Handle case-insensitive queries (cb.lower)
        cb.lower(path) >> expression
        cb.lower(expression) >> expression // Just in case

        // Capture values from equal() calls - matched against both 'path' (direct) and 'expression' (lower)
        cb.equal(path, _) >> { expr, val -> values << val; null }
        cb.equal(expression, _) >> { expr, val -> values << val; null }
        
        // Capture values from like() calls
        cb.like(path, _) >> { expr, val -> values << val; null }
        cb.like(expression, _) >> { expr, val -> values << val; null }
        
        // Handle common aggregations
        cb.and(_ as Predicate[]) >> null
        cb.or(_ as Predicate[]) >> null
        cb.between(path, _, _) >> { expr, val1, val2 -> values << val1; values << val2; null }

        // Execute capture
        jpaSpec.toPredicate(root, Mock(CriteriaQuery), cb)

        return [filters: fields, values: values]
    }

}