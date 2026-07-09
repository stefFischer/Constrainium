package at.sfischer.constraints;

import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.Node;

import java.util.List;

public interface ConstraintConstruct {
    String getName();

    ConstraintPolicy getRetentionPolicy();

    List<Node> getTerms();
}
