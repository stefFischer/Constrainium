package at.sfischer.constraints.miner;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.DataCollection;
import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;
import at.sfischer.constraints.model.Variable;
import at.sfischer.constraints.model.operators.Operator;

import java.util.*;

public interface ConstraintMiner {
    Set<Constraint> getPossibleConstraints(Set<Node> terms);
}
