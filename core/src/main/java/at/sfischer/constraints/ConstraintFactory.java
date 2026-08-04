package at.sfischer.constraints;

import at.sfischer.constraints.model.Node;

import java.util.Set;

public interface ConstraintFactory {

    Set<IConstraint> createConstraint(Node term);
}
