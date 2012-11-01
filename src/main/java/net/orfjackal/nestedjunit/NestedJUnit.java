// Copyright © 2009-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.nestedjunit;

import org.junit.*;
import org.junit.internal.runners.statements.*;
import org.junit.runner.*;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.*;
import org.junit.runners.model.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Allows organizing JUnit 4 tests into member classes. This makes it possible
 * to write tests in a more Behaviour-Driven Development (BDD) style:
 * <pre><code>
 * &#64;RunWith(NestedJUnit.class)
 * public class WerewolfTest extends Assert {
 *     public class Given_the_moon_is_full {
 *         &#64;Before public void When_you_walk_in_the_woods() {
 *             ...
 *         }
 *         &#64;Test public void Then_you_can_hear_werewolves_howling() {
 *             ...
 *         }
 *         &#64;Test public void Then_you_wish_you_had_a_silver_bullet() {
 *             ...
 *         }
 *     }
 *     public class Given_the_moon_is_not_full {
 *         &#64;Before public void When_you_walk_in_the_woods() {
 *             ...
 *         }
 *         &#64;Test public void Then_you_do_not_hear_any_werewolves() {
 *             ...
 *         }
 *         &#64;Test public void Then_you_are_not_afraid() {
 *             ...
 *         }
 *     }
 * }
 * </code></pre>
 *
 * @author Esko Luontola
 */
public class NestedJUnit extends ParentRunner<Runner> {

    private final TestClass parentTestClass;
    private final List<Runner> children = new ArrayList<Runner>();
    private final BlockJUnit4ClassRunner parent;

    public NestedJUnit(Class<?> testClass) throws InitializationError {
        super(testClass);
        parentTestClass = new TestClass(testClass);
        parent = new BlockJUnit4ClassRunner(testClass) {
            @Override
            protected void validateInstanceMethods(List<Throwable> errors) {
                // disable; don't fail if has no test methods
            }
        };
        addToChildrenAllNestedClassesWithTests(testClass);

        // If there are no children, then IntelliJ IDEA's test runner will get confused
        // and report the tests as still being executed and the progress bar will be buggy.
        if (children.size() == 0) {
            children.add(new NoTestsRunner(testClass));
        }
    }

    private void addToChildrenAllNestedClassesWithTests(Class<?> testClass) throws InitializationError {
        for (Class<?> child : testClass.getDeclaredClasses()) {
            if (containsTests(child)) {
                children.add(new NestedJUnit4ClassRunner(child));
            }
        }
    }

    private boolean containsTests(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (method.getAnnotation(Test.class) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run(RunNotifier notifier) {
        // XXX: these use different Description instances, so IDEA doesn't nest the tests nicely when mixing level 1 and 2 tests
        parent.run(notifier);
        super.run(notifier);
    }

    @Override
    protected List<Runner> getChildren() {
        return children;
    }

    @Override
    protected Description describeChild(Runner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(Runner child, RunNotifier notifier) {
        child.run(notifier);
    }


    private class NestedJUnit4ClassRunner extends BlockJUnit4ClassRunner {

        private Object parentOfCurrentTest;

        public NestedJUnit4ClassRunner(Class<?> childClass) throws InitializationError {
            super(childClass);
        }

        @Override
        protected void validateNoNonStaticInnerClass(List<Throwable> errors) {
            // disable default validation; our inner classes are non-static
        }

        @Override
        protected void validateConstructor(List<Throwable> errors) {
            validateOnlyOneConstructor(errors);
            validateNonStaticInnerClassWithDefaultConstructor(errors);
        }

        private void validateNonStaticInnerClassWithDefaultConstructor(List<Throwable> errors) {
            try {
                getTestClass().getJavaClass().getConstructor(parentTestClass.getJavaClass());
            } catch (NoSuchMethodException e) {
                String gripe = "Nested test classes should be non-static and have a public zero-argument constructor";
                errors.add(new Exception(gripe));
            }
        }

        @Override
        protected Object createTest() throws Exception {
            parentOfCurrentTest = parentTestClass.getJavaClass().newInstance();
            return getTestClass().getOnlyConstructor().newInstance(parentOfCurrentTest);
        }

        @Override
        protected Statement methodBlock(FrameworkMethod method) {
            Statement statement = super.methodBlock(method);
            statement = withParentBefores(statement);
            statement = withParentAfters(statement);
            return statement;
        }

        private Statement withParentBefores(Statement statement) {
            List<FrameworkMethod> befores = parentTestClass.getAnnotatedMethods(Before.class);
            return befores.isEmpty() ? statement : new RunBefores(statement, befores, parentOfCurrentTest);
        }

        private Statement withParentAfters(Statement statement) {
            List<FrameworkMethod> afters = parentTestClass.getAnnotatedMethods(After.class);
            return afters.isEmpty() ? statement : new RunAfters(statement, afters, parentOfCurrentTest);
        }
    }

    private static class NoTestsRunner extends Runner {
        private final Description description;

        public NoTestsRunner(Class<?> testClass) {
            description = Description.createTestDescription(testClass, "<no tests>");
        }

        @Override
        public Description getDescription() {
            return description;
        }

        @Override
        public void run(RunNotifier notifier) {
            notifier.fireTestIgnored(description);
        }
    }
}
