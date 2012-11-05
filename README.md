
NestedJUnit
===========

Enables you to organize JUnit tests in a hierarchy, so that the nested
test classes can reuse the context from the outermost test class. This
makes it easier to write [expressive tests][naming-tests].

NestedJUnit can be downloaded from [Maven Central][download] and is
licensed under Apache License 2.0.

As a usage example, have a look at [StackTest]:

```java
@RunWith(NestedJUnit.class)
public class StackTest {

    private Deque<String> stack;

    @Before
    public void createStack() {
        stack = new ArrayDeque<String>();
    }

    public class An_empty_stack {

        @Test
        public void is_empty() {
            assertTrue(stack.isEmpty());
        }

        @Test
        public void After_a_push_the_stack_is_no_longer_empty() {
            stack.push("a push");
            assertFalse(stack.isEmpty());
        }
    }

    public class When_objects_have_been_pushed_onto_a_stack {

        @Before
        public void pushObjects() {
            stack.push("pushed first");
            stack.push("pushed last");
        }

        @Test
        public void the_object_pushed_last_is_popped_first() {
            String poppedFirst = stack.pop();
            assertThat(poppedFirst, is("pushed last"));
        }

        @Test
        public void the_object_pushed_first_is_popped_last() {
            stack.pop();
            String poppedLast = stack.pop();
            assertThat(poppedLast, is("pushed first"));
        }

        @Test
        public void After_popping_all_objects_the_stack_is_empty() {
            stack.pop();
            stack.pop();
            assertTrue(stack.isEmpty());
        }
    }
}
```

[naming-tests]:  http://blog.orfjackal.net/2010/02/three-styles-of-naming-tests.html
[download]:      http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22net.orfjackal.nestedjunit%22
[StackTest]:     https://github.com/orfjackal/nestedjunit/blob/master/src/test/java/net/orfjackal/nestedjunit/StackTest.java


Version History
---------------

**1.0.0 (2012-11-05)**

- Runs `@Test` annotated test methods from two levels: the topmost test class (same as JUnit) and its non-static member classes
- Runs `@Before`, `@After` and `@Rule` from all levels
- Runs `@BeforeClass`, `@AfterClass` and `@ClassRule` from the topmost level
- The nested test classes have access to member variables from the topmost test class, and any side effects are isolated the same way as in JUnit

Known issues:

- Fails if a test class contains public member classes which do not have test methods
- Does not fail if there are no tests in a test class nor its nested test classes
