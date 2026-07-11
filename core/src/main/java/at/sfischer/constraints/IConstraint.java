package at.sfischer.constraints;

import at.sfischer.constraints.data.DataObject;

public interface IConstraint {

    ConstraintConstruct derivedFrom();

    <T> void evaluate(DataObject dao, T dataEntry, ConstraintResults<T> constraintResults);
}
