package com.itsolut.mantis.core.model;

/**
 * @author Robert Munteanu
 *
 */
public class MantisCustomField {
	
	private String name;
	
	private MantisCustomFieldType type;
	
	public String getName() {
		return name;
	}
	
	public MantisCustomFieldType getType() {
		return type;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setType(MantisCustomFieldType type) {
		this.type = type;
	}
	

}
