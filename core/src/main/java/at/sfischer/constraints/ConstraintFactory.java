package at.sfischer.constraints;

import at.sfischer.constraints.model.Node;

public interface ConstraintFactory {

    IConstraint createConstraint(Node term);
}
