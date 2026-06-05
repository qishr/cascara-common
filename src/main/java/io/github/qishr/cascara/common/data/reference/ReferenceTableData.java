package io.github.qishr.cascara.common.data.reference;

import java.util.HashMap;
import java.util.Map;

import io.github.qishr.cascara.common.data.TableData;

/// A reference implementation of TableData
public class ReferenceTableData implements TableData {

    private Map<String, Object> valuesMap = new HashMap<>();

    public ReferenceTableData() {
        // Nothing to see here
    }

    @Override
    public final Object[] getValues() {
        Object[] r = new Object[valuesMap.size()];
        int i = 0;
        for (Object value : valuesMap.values()) {
            r[i] = value;
            i++;
        }
        return r;
    }

    @Override
	public Map<String, Object> getValuesMap() {
        return valuesMap;
	}

    public ReferenceTableData put(String key, String value) {
        valuesMap.put(key, value);
        return this;
    }

	@Override
	public Object get(String key) {
        return valuesMap.get(key);
	}

}
