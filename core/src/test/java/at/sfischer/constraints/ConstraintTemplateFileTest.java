package at.sfischer.constraints;

import at.sfischer.constraints.model.Node;
import at.sfischer.constraints.model.validation.ValidationContext;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConstraintTemplateFileTest {

    // ---------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------

    private ConstraintTemplate mockValidConstraint() {
        ConstraintTemplate constraint = mock(ConstraintTemplate.class);
        Node term = mock(Node.class);

        doAnswer(invocation -> {
            ValidationContext ctx = invocation.getArgument(0);
            // no errors added
            return null;
        }).when(term).validate(any());

        when(constraint.getTerm()).thenReturn(term);
        return constraint;
    }

    private ConstraintTemplate mockInvalidConstraint() {
        ConstraintTemplate constraint = mock(ConstraintTemplate.class);
        Node term = mock(Node.class);

        doAnswer(invocation -> {
            ValidationContext ctx = invocation.getArgument(0);
            ctx.error(null, "Validation failed");
            return null;
        }).when(term).validate(any());

        when(constraint.getTerm()).thenReturn(term);
        return constraint;
    }

    // ---------------------------------------------------------
    // Test methods
    // ---------------------------------------------------------

    @Test
    void shouldKeepValidConstraints() {
        ConstraintTemplate valid = mockValidConstraint();
        ConstraintTemplateFile file = new ConstraintTemplateFile(
                Map.of(),
                new ArrayList<>(List.of(valid)),
                List.of()
        );

        Map<ConstraintTemplate, ValidationContext> removed = file.removeInvalidConstraints();

        assertTrue(removed.isEmpty());
        assertEquals(1, file.getConstraints().size());
        assertTrue(file.getConstraints().contains(valid));
    }

    @Test
    void shouldRemoveInvalidConstraints() {
        ConstraintTemplate invalid = mockInvalidConstraint();
        ConstraintTemplateFile file = new ConstraintTemplateFile(
                Map.of(),
                new ArrayList<>(List.of(invalid)),
                List.of()
        );

        Map<ConstraintTemplate, ValidationContext> removed = file.removeInvalidConstraints();

        assertEquals(1, removed.size());
        assertFalse(file.getConstraints().contains(invalid));
        assertTrue(removed.containsKey(invalid));
        assertFalse(removed.get(invalid).isValid());
    }

    @Test
    void shouldHandleMixedValidAndInvalidConstraints() {
        ConstraintTemplate valid = mockValidConstraint();
        ConstraintTemplate invalid = mockInvalidConstraint();
        ConstraintTemplateFile file = new ConstraintTemplateFile(
                Map.of(),
                new ArrayList<>(List.of(valid, invalid)),
                List.of()
        );

        Map<ConstraintTemplate, ValidationContext> removed = file.removeInvalidConstraints();

        assertEquals(1, removed.size());
        assertEquals(1, file.getConstraints().size());
        assertTrue(file.getConstraints().contains(valid));
        assertFalse(file.getConstraints().contains(invalid));
    }

    @Test
    void shouldValidateConstraintsInsideGroups() {
        ConstraintTemplate valid = mockValidConstraint();
        ConstraintTemplate invalid = mockInvalidConstraint();
        GroupDefinition group = mock(GroupDefinition.class);
        List<ConstraintTemplate> groupConstraints = new ArrayList<>(List.of(valid, invalid));
        when(group.getConstraints()).thenReturn(groupConstraints);

        ConstraintTemplateFile file = new ConstraintTemplateFile(
                Map.of(),
                new ArrayList<>(),
                List.of(group)
        );

        Map<ConstraintTemplate, ValidationContext> removed = file.removeInvalidConstraints();

        assertEquals(1, removed.size());
        assertEquals(1, groupConstraints.size());
        assertTrue(groupConstraints.contains(valid));
        assertFalse(groupConstraints.contains(invalid));
    }

    @Test
    void shouldHandleEmptyFile() {
        ConstraintTemplateFile file = new ConstraintTemplateFile(
                Map.of(),
                new ArrayList<>(),
                List.of()
        );

        Map<ConstraintTemplate, ValidationContext> removed = file.removeInvalidConstraints();

        assertTrue(removed.isEmpty());
        assertTrue(file.getConstraints().isEmpty());
    }
}
