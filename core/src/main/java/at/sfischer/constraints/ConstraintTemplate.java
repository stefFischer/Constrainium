package at.sfischer.constraints;

import at.sfischer.constraints.miner.ConstraintPolicy;
import at.sfischer.constraints.model.Node;

import java.util.List;

public class ConstraintTemplate implements ConstraintConstruct {

    private final String name;

    private final Node term;

    private final ConstraintPolicy retentionPolicy;

    public ConstraintTemplate(String name, Node term, ConstraintPolicy retentionPolicy) {
        this.name = name;
        this.term = term;
        this.retentionPolicy = retentionPolicy;
    }

    @Override
    public String getName() {
        return name;
    }

    public Node getTerm() {
        return term;
    }

    @Override
    public List<Node> getTerms() {
        return List.of(this.term);
    }

    @Override
    public ConstraintPolicy getRetentionPolicy() {
        return retentionPolicy;
    }
}
