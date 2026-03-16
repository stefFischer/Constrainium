package at.sfischer.testing;

import at.sfischer.constraints.Constraint;
import at.sfischer.constraints.data.DataSchemaEntry;
import at.sfischer.constraints.model.Node;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaAssertions {

    public static void assertPotentialConstraintNumber(DataSchemaEntry<?> entry, int constraintNumber) {
        assertEquals(constraintNumber, entry.potentialConstraints.size());
    }

    public static void assertPotentialConstraintNumber(DataSchemaEntry<?> entry, int constraintNumber, String message) {
        assertEquals(constraintNumber, entry.potentialConstraints.size(), message);
    }

    public static void assertNoPotentialConstraintType(DataSchemaEntry<?> entry, Class<?> nodeType) {
        assertNoPotentialConstraintType(entry, nodeType, "");
    }

    public static void assertNoPotentialConstraintType(DataSchemaEntry<?> entry, Class<?> nodeType, String message) {
        assertFalse(containsPotentialConstraintType(entry, nodeType), message);
    }

    public static void assertPotentialConstraintType(DataSchemaEntry<?> entry, Class<?> nodeType) {
        assertPotentialConstraintType(entry, nodeType, "");
    }

    public static void assertPotentialConstraintType(DataSchemaEntry<?> entry, Class<?> nodeType, String message) {
        assertTrue(containsPotentialConstraintType(entry, nodeType), message);
    }

    public static void assertPotentialConstraint(DataSchemaEntry<?> entry, Node term) {
        assertPotentialConstraint(entry, term, "");
    }

    public static void assertPotentialConstraint(DataSchemaEntry<?> entry, Node term, String message) {
        assertTrue(containsPotentialConstraint(entry, term), message);
    }

    public static void assertNoPotentialConstraint(DataSchemaEntry<?> entry, Node term) {
        assertNoPotentialConstraint(entry, term, "");
    }

    public static void assertNoPotentialConstraint(DataSchemaEntry<?> entry, Node term, String message) {
        assertFalse(containsPotentialConstraint(entry, term), message);
    }

    public static void assertConstraintNumber(DataSchemaEntry<?> entry, int constraintNumber) {
        assertEquals(constraintNumber, entry.constraints.size());
    }

    public static void assertConstraintNumber(DataSchemaEntry<?> entry, int constraintNumber, String message) {
        assertEquals(constraintNumber, entry.constraints.size(), message);
    }

    public static void assertNoConstraintType(DataSchemaEntry<?> entry, Class<?> nodeType) {
        assertNoConstraintType(entry, nodeType, "");
    }

    public static void assertNoConstraintType(DataSchemaEntry<?> entry, Class<?> nodeType, String message) {
        assertFalse(containsConstraintType(entry, nodeType), message);
    }

    public static void assertConstraintType(DataSchemaEntry<?> entry, Class<?> nodeType) {
        assertConstraintType(entry, nodeType, "");
    }

    public static void assertConstraintType(DataSchemaEntry<?> entry, Class<?> nodeType, String message) {
        assertTrue(containsConstraintType(entry, nodeType), message);
    }

    public static void assertConstraint(DataSchemaEntry<?> entry, Node term) {
        assertConstraint(entry, term, "");
    }

    public static void assertConstraint(DataSchemaEntry<?> entry, Node term, String message) {
        assertTrue(containsConstraint(entry, term), message);
    }

    public static void assertNoConstraint(DataSchemaEntry<?> entry, Node term) {
        assertNoConstraint(entry, term, "");
    }

    public static void assertNoConstraint(DataSchemaEntry<?> entry, Node term, String message) {
        assertFalse(containsConstraint(entry, term), message);
    }


    private static boolean containsPotentialConstraintType(DataSchemaEntry<?> entry, Class<?> nodeType) {
        return containsConstraintType(entry.potentialConstraints, nodeType);
    }

    private static boolean containsPotentialConstraint(DataSchemaEntry<?> entry, Node term) {
        return containsConstraint(entry.potentialConstraints, term);
    }

    private static boolean containsConstraintType(DataSchemaEntry<?> entry, Class<?> nodeType) {
        return containsConstraintType(entry.constraints, nodeType);
    }

    private static boolean containsConstraint(DataSchemaEntry<?> entry, Node term) {
        return containsConstraint(entry.constraints, term);
    }

    private static boolean containsConstraintType(Set<Constraint> constraints, Class<?> nodeType) {
        return constraints.stream()
                .anyMatch(c -> nodeType.isInstance(c.term()));
    }

    private static boolean containsConstraint(Set<Constraint> constraints, Node term) {
        return constraints.stream()
                .anyMatch(c -> term.equals(c.term()));
    }
}
