package org.sofumar.portal.framework.bl;

import org.sofumar.portal.framework.vo.ValueObject;

import java.util.List;

public interface BusinessLogic<V extends ValueObject> {

    V add(V vo);

    V update(V vo);

    void delete(V vo);

    void delete(List<V> vo);
}