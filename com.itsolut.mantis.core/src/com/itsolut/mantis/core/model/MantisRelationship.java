package com.itsolut.mantis.core.model;

public class MantisRelationship {
	private int id;

	private int targetId;
	
	private RelationType type;
	
	public enum RelationType {
		RELATED, PARENT, CHILD, DUPLICATE, HAS_DUPLICATE;
		
		@Override
		public String toString() {
			switch(this) {
			case RELATED:
				return "related to";
			case PARENT:
				return "parent of";
			case CHILD:
				return "child of";
			case DUPLICATE:
				return "duplicate of";
			case HAS_DUPLICATE:
				return "has duplicate";
			default:
				return "";
			}
		}
		
		public static RelationType fromRelation(String relation) {
			if("duplicate of".equals(relation))
				return DUPLICATE;
			else if("has duplicate".equals(relation))
				return HAS_DUPLICATE;
			else if("related to".equals(relation))
				return RELATED;
			else if("parent of".equals(relation))
				return PARENT;
			else if("child of".equals(relation))
				return CHILD;
			return null;
		}
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setTargetId(int targetId) {
		this.targetId = targetId;
	}

	public int getTargetId() {
		return targetId;
	}

	public void setType(RelationType type) {
		this.type = type;
	}

	public RelationType getType() {
		return type;
	}
	
}
