package at.sfischer.constraints.miner;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.model.Node;

import java.util.Set;

public interface ConstraintMiner {
    Set<Constraint> getPossibleConstraints(Set<Node> terms);
}
