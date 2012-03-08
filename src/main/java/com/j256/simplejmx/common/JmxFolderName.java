package com.j256.simplejmx.common;

import javax.management.ObjectName;

/**
 * Wrapper around a folder-name that is used in {@link JmxSelfNaming#getJmxFolderNames()} and
 * {@link ObjectNameUtil#makeObjectName(JmxResource, JmxSelfNaming)}. Either it can be a field/value pair which turns
 * into "field=value" in the {@link ObjectName} or it can be just a value in which case a numerical field prefix is
 * auto-generated.
 * 
 * @author graywatson
 */
public class JmxFolderName {

	private final String field;
	private final String value;

	public JmxFolderName(String value) {
		this.field = null;
		this.value = value;
	}

	public JmxFolderName(String field, String value) {
		this.field = field;
		this.value = value;
	}

	public String getField() {
		return field;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		if (field == null) {
			return "(null-field)=" + value;
		} else {
			return field + "=" + value;
		}
	}
}
