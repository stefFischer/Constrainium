package at.sfischer.constraints;

import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.Node;

public class ConstraintTemplate {

    private final String name;

    private final Node term;

    private final ConstraintPolicy retentionPolicy;

    public ConstraintTemplate(String name, Node term, ConstraintPolicy retentionPolicy) {
        this.name = name;
        this.term = term;
        this.retentionPolicy = retentionPolicy;
    }

    public String getName() {
        return name;
    }

    public Node getTerm() {
        return term;
    }

    public ConstraintPolicy getRetentionPolicy() {
        return retentionPolicy;
    }
}
