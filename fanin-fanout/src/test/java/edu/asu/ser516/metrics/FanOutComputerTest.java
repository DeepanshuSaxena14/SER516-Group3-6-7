package edu.asu.ser516.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FanOutComputerTest {

    private static ClassReference ref(String src, String tgt) {
        return new ClassReference(src, tgt);
    }

    @Nested
    @DisplayName("1. Empty and boundary inputs")
    class EmptyAndBoundaryInputs {

        @Test
        @DisplayName("Empty list returns empty map")
        void emptyListReturnsEmptyMap() {
            Map<String, Integer> result = FanOutComputer.computeFanOut(List.of());
            assertTrue(result.isEmpty(), "Expected empty map for empty input");
        }

        @Test
        @DisplayName("Single self-reference produces empty map")
        void singleSelfReferenceOnlyProducesEmptyMap() {
            Map<String, Integer> result = FanOutComputer.computeFanOut(
                    List.of(ref("A", "A"))
            );
            assertTrue(result.isEmpty(),
                    "Pure self-reference should contribute nothing to fan-out");
        }

        @Test
        @DisplayName("All self-references produces empty map")
        void allSelfReferencesProduceEmptyMap() {
            List<ClassReference> refs = List.of(
                    ref("A", "A"),
                    ref("B", "B"),
                    ref("C", "C")
            );
            Map<String, Integer> result = FanOutComputer.computeFanOut(refs);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("2. Self-reference exclusion")
    class SelfReferenceExclusion {

        @Test
        @DisplayName("Self-reference mixed with real reference is excluded")
        void selfReferenceIsExcluded() {
            Map<String, Integer> result = FanOutComputer.computeFanOut(
                    List.of(ref("A", "A"), ref("A", "B"))
            );
            assertEquals(1, result.get("A"),
                    "Self-reference must not count; only B should be counted");
        }

        @Test
        @DisplayName("Multiple self-references do not inflate fan-out")
        void multipleSelfReferencesDoNotInflate() {
            Map<String, Integer> result = FanOutComputer.computeFanOut(
                    List.of(ref("A", "A"), ref("A", "A"), ref("A", "B"))
            );
            assertEquals(1, result.get("A"));
        }
    }

    @Nested
    @DisplayName("3. Duplicate reference deduplication")
    class DuplicateDeduplication {

        @Test
        @DisplayName("Identical references count as one")
        void identicalReferencesCountAsOne() {
            Map<String, Integer> result = FanOutComputer.computeFanOut(
                    List.of(ref("A", "B"), ref("A", "B"), ref("A", "B"))
            );
            assertEquals(1, result.get("A"),
                    "Three identical references should collapse to fan-out=1");
        }

        @Test
        @DisplayName("Two distinct targets with duplicates counts correctly")
        void twoDistinctTargetsWithDuplicates() {
            Map<String, Integer> result = FanOutComputer.computeFanOut(
                    List.of(ref("A", "B"), ref("A", "C"), ref("A", "B"))
            );
            assertEquals(2, result.get("A"),
                    "Duplicate A→B should not inflate; expected 2 distinct targets");
        }
    }

    @Nested
    @DisplayName("4. Single class fan-out values")
    class SingleClassFanOut {

        @Test
        @DisplayName("Fan-out of 1 when referencing exactly one other class")
        void fanOutOfOne() {
            Map<String, Integer> result = FanOutComputer.computeFanOut(
                    List.of(ref("Service", "Repository"))
            );
            assertEquals(1, result.get("Service"));
        }

        @Test
        @DisplayName("Fan-out grows with each distinct target")
        void fanOutGrowsWithDistinctTargets() {
            List<ClassReference> refs = List.of(
                    ref("Service", "Repository"),
                    ref("Service", "Model"),
                    ref("Service", "Util"),
                    ref("Service", "Validator")
            );
            Map<String, Integer> result = FanOutComputer.computeFanOut(refs);
            assertEquals(4, result.get("Service"));
        }
    }

    @Nested
    @DisplayName("5. Multiple classes computed independently")
    class MultipleClassFanOut {

        @Test
        @DisplayName("Two classes have their own fan-out counts")
        void twoClassesHaveIndependentCounts() {
            List<ClassReference> refs = List.of(
                    ref("Service", "Repository"),
                    ref("Service", "Model"),
                    ref("Service", "Util"),
                    ref("Controller", "Service"),
                    ref("Controller", "Model")
            );
            Map<String, Integer> result = FanOutComputer.computeFanOut(refs);

            assertEquals(3, result.get("Service"), "Service should have fan-out 3");
            assertEquals(2, result.get("Controller"), "Controller should have fan-out 2");
            assertEquals(2, result.size(), "Only source classes should appear in the map");
        }

        @Test
        @DisplayName("Target-only class is absent from result map")
        void targetOnlyClassAbsentFromResult() {
            Map<String, Integer> result = FanOutComputer.computeFanOut(
                    List.of(ref("A", "B"))
            );
            assertTrue(result.containsKey("A"), "Source class A must be in the map");
            assertFalse(result.containsKey("B"),
                    "Target-only class B must not appear as a key");
        }

        @Test
        @DisplayName("Three classes each with distinct fan-out")
        void threeClassesWithDifferentFanOut() {
            List<ClassReference> refs = List.of(
                    ref("A", "X"),
                    ref("B", "X"), ref("B", "Y"),
                    ref("C", "X"), ref("C", "Y"), ref("C", "Z")
            );
            Map<String, Integer> result = FanOutComputer.computeFanOut(refs);

            assertEquals(1, result.get("A"));
            assertEquals(2, result.get("B"));
            assertEquals(3, result.get("C"));
        }
    }

    @Nested
    @DisplayName("6. Zero-fan-out classes not present in map")
    class ZeroFanOutAbsence {

        @Test
        @DisplayName("Class only referenced by others has no entry")
        void referencedOnlyClassHasNoEntry() {
            // Leaf class "Repository" is referenced by Service but never references anyone.
            List<ClassReference> refs = List.of(
                    ref("Service", "Repository"),
                    ref("Controller", "Repository")
            );
            Map<String, Integer> result = FanOutComputer.computeFanOut(refs);

            assertFalse(result.containsKey("Repository"));
            assertTrue(result.containsKey("Service"));
            assertTrue(result.containsKey("Controller"));
        }
    }

    @Nested
    @DisplayName("7. Fully-qualified class name handling")
    class FullyQualifiedClassNames {

        @Test
        @DisplayName("FQCN source and target treated as distinct strings")
        void fullyQualifiedNamesAreTreatedLiterally() {
            List<ClassReference> refs = List.of(
                    ref("com.example.ServiceA", "com.example.Repository"),
                    ref("com.example.ServiceA", "com.example.Model"),
                    ref("com.example.ServiceA", "com.example.ServiceA") // self-ref by FQCN
            );
            Map<String, Integer> result = FanOutComputer.computeFanOut(refs);

            assertEquals(2, result.get("com.example.ServiceA"),
                    "FQCN self-reference must be excluded; remaining fan-out is 2");
        }

        @Test
        @DisplayName("Simple and FQCN versions of the same name are treated as different keys")
        void simpleAndFqcnTreatedAsDifferentKeys() {
            // computeFanOut is string-based; "Service" ≠ "com.example.Service"
            List<ClassReference> refs = List.of(
                    ref("Service", "Model"),
                    ref("com.example.Service", "Model")
            );
            Map<String, Integer> result = FanOutComputer.computeFanOut(refs);

            assertEquals(2, result.size(),
                    "Simple name and FQCN should be tracked as separate source classes");
            assertEquals(1, result.get("Service"));
            assertEquals(1, result.get("com.example.Service"));
        }
    }

    @Nested
    @DisplayName("8. Large-input correctness")
    class LargeInput {

        @Test
        @DisplayName("1 000 identical references collapse to fan-out 1")
        void manyDuplicatesCollapse() {
            List<ClassReference> refs = new ArrayList<>();
            for (int i = 0; i < 1_000; i++) {
                refs.add(ref("A", "B"));
            }
            Map<String, Integer> result = FanOutComputer.computeFanOut(refs);
            assertEquals(1, result.get("A"));
        }

        @Test
        @DisplayName("100 distinct targets produce fan-out 100")
        void manyDistinctTargets() {
            List<ClassReference> refs = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                refs.add(ref("Hub", "Class" + i));
            }
            Map<String, Integer> result = FanOutComputer.computeFanOut(refs);
            assertEquals(100, result.get("Hub"));
        }

        @Test
        @DisplayName("100 source classes each with fan-out 5 are all computed correctly")
        void manySourceClassesEachWithFanOutFive() {
            List<ClassReference> refs = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                String src = "Class" + i;
                for (int j = 0; j < 5; j++) {
                    refs.add(ref(src, "Dep" + j)); // Dep0..Dep4 shared across all
                }
            }
            Map<String, Integer> result = FanOutComputer.computeFanOut(refs);
            assertEquals(100, result.size());
            result.forEach((cls, fanOut) ->
                    assertEquals(5, fanOut, cls + " should have fan-out 5"));
        }
    }

    @Nested
    @DisplayName("9. Returned map independence")
    class ReturnMapIndependence {

        @Test
        @DisplayName("Mutating the input list after the call does not affect the result")
        void mutatingInputDoesNotAffectResult() {
            List<ClassReference> refs = new ArrayList<>(List.of(ref("A", "B")));
            Map<String, Integer> result = FanOutComputer.computeFanOut(refs);

            refs.add(ref("A", "C")); // modify input after computation

            assertEquals(1, result.get("A"),
                    "Fan-out should reflect state at call time, not after mutation");
        }
    }

    @Nested
    @DisplayName("10. ClassReference value semantics")
    class ClassReferenceValueSemantics {

        @Test
        @DisplayName("Two ClassReferences with same fields are equal")
        void equalWhenSameFields() {
            ClassReference r1 = ref("A", "B");
            ClassReference r2 = ref("A", "B");
            assertEquals(r1, r2);
        }

        @Test
        @DisplayName("Equal ClassReferences have the same hashCode")
        void sameHashCodeWhenEqual() {
            ClassReference r1 = ref("A", "B");
            ClassReference r2 = ref("A", "B");
            assertEquals(r1.hashCode(), r2.hashCode());
        }

        @Test
        @DisplayName("ClassReferences with different source are not equal")
        void notEqualWhenSourceDiffers() {
            assertNotEquals(ref("A", "B"), ref("X", "B"));
        }

        @Test
        @DisplayName("ClassReferences with different target are not equal")
        void notEqualWhenTargetDiffers() {
            assertNotEquals(ref("A", "B"), ref("A", "X"));
        }

        @Test
        @DisplayName("sourceClass() returns the value passed at construction")
        void sourceClassAccessor() {
            assertEquals("MySource", ref("MySource", "MyTarget").sourceClass());
        }

        @Test
        @DisplayName("targetClass() returns the value passed at construction")
        void targetClassAccessor() {
            assertEquals("MyTarget", ref("MySource", "MyTarget").targetClass());
        }

        @Test
        @DisplayName("ClassReference rejects null sourceClass")
        void nullSourceClassThrows() {
            assertThrows(NullPointerException.class, () -> new ClassReference(null, "B"));
        }

        @Test
        @DisplayName("ClassReference rejects null targetClass")
        void nullTargetClassThrows() {
            assertThrows(NullPointerException.class, () -> new ClassReference("A", null));
        }

        @Test
        @DisplayName("ClassReference is not equal to null")
        void notEqualToNull() {
            assertNotEquals(null, ref("A", "B"));
        }

        @Test
        @DisplayName("ClassReference is not equal to an object of a different type")
        void notEqualToDifferentType() {
            assertNotEquals("A->B", ref("A", "B"));
        }

        @Test
        @DisplayName("ClassReference is reflexively equal to itself")
        void reflexiveEquality() {
            ClassReference r = ref("A", "B");
            assertEquals(r, r);
        }
    }

    @Test
    void computesDistinctFanOutPerClass() {
        List<ClassReference> refs = List.of(
                new ClassReference("A", "B"),
                new ClassReference("A", "C"),
                new ClassReference("A", "B")  // duplicate
        );

        Map<String, Integer> fanOut = FanOutComputer.computeFanOut(refs);

        assertEquals(1, fanOut.size());
        assertEquals(2, fanOut.get("A"));  // B and C, distinct
    }

    @Test
    void excludesSelfReferences() {
        List<ClassReference> refs = List.of(
                new ClassReference("A", "A"),
                new ClassReference("A", "B")
        );

        Map<String, Integer> fanOut = FanOutComputer.computeFanOut(refs);

        assertEquals(1, fanOut.get("A"));  // only B
    }

    @Test
    void emptyInputReturnsEmptyMap() {
        Map<String, Integer> fanOut = FanOutComputer.computeFanOut(List.of());

        assertTrue(fanOut.isEmpty());
    }

    @Test
    void multipleClassesWithDifferentFanOut() {
        List<ClassReference> refs = List.of(
                new ClassReference("Service", "Repository"),
                new ClassReference("Service", "Model"),
                new ClassReference("Service", "Util"),
                new ClassReference("Controller", "Service"),
                new ClassReference("Controller", "Model")
        );

        Map<String, Integer> fanOut = FanOutComputer.computeFanOut(refs);

        assertEquals(3, fanOut.get("Service"));
        assertEquals(2, fanOut.get("Controller"));
    }

    @Test
    void classWithNoOutgoingReferencesNotInResult() {
        List<ClassReference> refs = List.of(
                new ClassReference("A", "B")
        );

        Map<String, Integer> fanOut = FanOutComputer.computeFanOut(refs);

        assertTrue(fanOut.containsKey("A"));
        assertFalse(fanOut.containsKey("B"));  // B has no outgoing refs
    }
}
