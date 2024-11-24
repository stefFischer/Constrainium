package at.sfischer.constraints.model.operators;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.Type;

import java.util.List;

public interface Operator extends Node {

    List<Type> operandTypes();
}
