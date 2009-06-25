package com.itsolut.mantis.core.model;

/**
 * @author Robert Munteanu
 */
public enum MantisCustomFieldType {

	STRING(0), NUMERIC(1), FLOAT(2), ENUMERATION(3), EMAIL(4), CHECKBOX(5), LIST(
			6), MULTISELECTION_LIST(7), DATE(8), RADIO(9);

	private static final long serialVersionUID = -2029433772857020154L;
	private final int remoteId;

	private MantisCustomFieldType(int remoteId) {

		this.remoteId = remoteId;
	}

	public static MantisCustomFieldType getByRemoteId(int remoteId) {

		for (MantisCustomFieldType type : values())
			if (type.remoteId == remoteId)
				return type;
		
		return null;
	}

}
