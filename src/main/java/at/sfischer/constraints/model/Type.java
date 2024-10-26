package at.sfischer.constraints.model;

public interface Type {

    /**
     * Returns if this type can be assigned to the target type (target = this).
     *
     * @param target
     * @return
     */
    boolean canAssignTo(Type target);

}
