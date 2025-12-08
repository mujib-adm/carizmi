package org.sofumar.portal.data.transformer;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public interface Transformer<Input, Output> {
    Output transform(Input input);

    default List<Output> transformList(List<Input> inputs) {
        return inputs == null ? List.of() :
                inputs.stream()
                        .filter(Objects::nonNull)
                        .map(this::transform)
                        .collect(Collectors.toList());
    }

    default Set<Output> transformSet(Set<Input> inputs) {
        return inputs == null ? Set.of() :
                inputs.stream()
                        .filter(Objects::nonNull)
                        .map(this::transform)
                        .collect(Collectors.toSet());
    }
}
