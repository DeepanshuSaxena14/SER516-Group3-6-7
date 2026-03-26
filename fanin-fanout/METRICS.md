# Metric Definitions

## Class-Level Fan-Out

**Definition**: The number of **unique** classes (within the project) that a specific class references. High fan-out suggests a class has many dependencies and may be brittle.

### Computation Rules

1.  **Unit of Analysis**:
    *   Top-level `class`, `interface`, or `enum`.
    *   Inner/nested classes are treated as part of the containing top-level class. Dependencies *within* the same file (e.g., inner class referencing outer class) do not count.

2.  **Dependencies (What counts)**:
    A class `A` has a dependency on class `B` if `A` explicitly references `B` in any of the following ways:
    *   **Inheritance**: `extends B` or `implements B`.
    *   **Fields**: Declaring a member variable of type `B` (e.g., `private B myField;`).
    *   **Methods**:
        *   Method return type is `B`.
        *   Method parameter type is `B`.
        *   Method throws exception `B`.
    *   **Local Variables**: Declaring a variable of type `B` inside a method.
    *   **Instantiations**: Calling `new B()`.
    *   **Static Access**: Accessing `B.staticMethod()` or `B.STATIC_FIELD`.
    *   **Type Casting**: Casting an object to type `B`.
    *   **Annotations**: Using `@B` (if `B` is a custom annotation defined in the project).
    *   **Generics**: Using `B` as a type argument (e.g., `List<B>`).

3.  **Exclusions (What to ignore)**:
    *   **Standard Library**: All types under `java.*`, `javax.*`, `jdk.*`, `sun.*` are **ignored**. We are measuring *internal* project coupling.
    *   **Primitives**: `int`, `boolean`, `char`, `void`, etc., are **ignored**.
    *   **Self-Reference**: A class referring to itself is **ignored**.
    *   **Third-Party Libraries**: References to external libraries (e.g., `org.junit.*`, `com.google.*`) are **ignored** unless explicitly stated otherwise. Only classes defined *within the project source* count.

4.  **Counting**:
    *   The metric is a count of **unique** types.
    *   *Example*: If class `A` has two fields of type `B` and one method returning `B`, the Fan-Out contribution from `B` is **1**.

## Class-Level Fan-In

**Definition**: The number of other classes (within the project) that reference the target class. High fan-in suggests a class is a common utility or core component; changing it may break many other classes.

*Derived strictly by reversing the Fan-Out relationships defined above.*
