package io.github.qishr.cascara.common.data;

import java.util.Map;

/// Data that can represent a row within a table
public interface TableData {
    Object[] getValues();
    Map<String,Object> getValuesMap();
    Object get(String key);
}
