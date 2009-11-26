package com.itsolut.mantis.core.model;

/**
 * @author Robert Munteanu
 * 
 */
public enum MantisCustomFieldType {

	STRING(0), NUMERIC(1), FLOAT(2), ENUM(3), EMAIL(4), CHECKBOX(5), LIST(6), MULTILIST(
			7), DATE(8), RADIO(9);

	public static MantisCustomFieldType fromMantisConstant(int mantisConstant) {

		for (MantisCustomFieldType customFieldType : MantisCustomFieldType
				.values())
			if (customFieldType.mantisConstant == mantisConstant)
				return customFieldType;

		return null;
	}

	private final int mantisConstant;

	private MantisCustomFieldType(int mantisConstant) {

		this.mantisConstant = mantisConstant;
	}

}
