
package com.itsolut.mantis.core.model;

import java.math.BigInteger;

public class MantisRelationship {

    private int          id;

    private int          targetId;

    private RelationType type;

    public enum RelationType {
        RELATED(1, "related to"), PARENT(2, "parent of"), CHILD(3, "child of"), DUPLICATE(0, "duplicate of"), HAS_DUPLICATE(
                4, "has duplicate"),
        /**
         * Since matching is string-based, renaming relationship types has ill side-efects
         * 
         * <p>
         * This is a catch-all specifically added for
         * https://sourceforge.net/tracker/index.php?func=detail&aid=1956462&group_id=189858&atid=931013
         * </p>
         */
        UNKNOWN(-1, "unknown");

        public static RelationType fromRelationId(BigInteger relationId) {

            for (RelationType relationType : RelationType.values())
                if (relationType.mantisConstant == relationId.intValue())
                    return relationType;

            return UNKNOWN;

        }

        public static RelationType fromRelation(String relation) {

            for (RelationType relationType : RelationType.values())
                if (relationType.defaultMantisName.equals(relation))
                    return relationType;

            return UNKNOWN;
        }

        private final int    mantisConstant;

        private final String defaultMantisName;

        private RelationType(int mantisConstant, String defaultMantisName) {

            this.mantisConstant = mantisConstant;
            this.defaultMantisName = defaultMantisName;

        }

        public int getMantisConstant() {

            return mantisConstant;
        }

        @Override
        public String toString() {

            return defaultMantisName;
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
