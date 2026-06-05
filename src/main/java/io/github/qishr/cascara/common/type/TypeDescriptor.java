package io.github.qishr.cascara.common.type;

import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.service.ServiceProvider;

public interface TypeDescriptor extends ServiceProvider {

    void toSchema(MapAstNode<?,?> node);

    Object toType(String string);
    String toText(Object object);

}
