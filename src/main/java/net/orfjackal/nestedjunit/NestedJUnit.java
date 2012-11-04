// Copyright © 2009-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.nestedjunit;

import org.junit.Test;
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
public class NestedJUnit extends Runner {

    private final BlockJUnit4ClassRunner level1;
    private final NestedParentRunner level2;
    private final Description description;

    public NestedJUnit(Class<?> testClass) throws InitializationError {
        level1 = new BlockJUnit4ClassRunner(testClass) {
            @Override
            protected void validateInstanceMethods(List<Throwable> errors) {
                // disable; don't fail if has no test methods
            }
        };
        level2 = new NestedParentRunner(level1.getTestClass(), testClass);

        description = level1.getDescription();
        for (Description child : level2.getDescription().getChildren()) {
            description.addChild(child);
        }
    }

    @Override
    public void run(RunNotifier notifier) {
        level1.run(notifier);
        level2.run(notifier);
    }

    @Override
    public Description getDescription() {
        return description;
    }


    private class NestedParentRunner extends ParentRunner<Runner> {

        private final List<Runner> children = new ArrayList<Runner>();
        private final TestClass parent;

        public NestedParentRunner(TestClass parent, Class<?> testClass) throws InitializationError {
            super(testClass);
            this.parent = parent;
            addToChildrenAllNestedClassesWithTests(testClass);
        }

        private void addToChildrenAllNestedClassesWithTests(Class<?> testClass) throws InitializationError {
            for (Class<?> child : testClass.getDeclaredClasses()) {
                if (containsTests(child)) {
                    children.add(new LeafFixture(child, new SurroundingFixture(parent.getJavaClass())));
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
    }

    private class LeafFixture extends BlockJUnit4ClassRunner {

        private final SurroundingFixture parent;

        public LeafFixture(Class<?> testClass, SurroundingFixture parent) throws InitializationError {
            super(testClass);
            this.parent = parent;
        }

        @Override
        protected void validateNoNonStaticInnerClass(List<Throwable> errors) {
            // disable default validation; our inner classes are non-static
        }

        @Override
        protected void validateZeroArgConstructor(List<Throwable> errors) {
            // disable default validation; our inner classes take the outer class as parameter
        }

        @Override
        protected Object createTest() throws Exception {
            return getTestClass().getOnlyConstructor().newInstance(parent.createFreshTest());
        }

        @Override
        protected Statement methodBlock(FrameworkMethod method) {
            Statement statement = super.methodBlock(method);
            statement = parent.withParentFixture(method, statement);
            return statement;
        }
    }

    private static class SurroundingFixture extends BlockJUnit4ClassRunner {

        private Object currentTest;
        private Statement nestedFixture;

        public SurroundingFixture(Class<?> testClass) throws InitializationError {
            super(testClass);
        }

        @Override
        protected void validateInstanceMethods(List<Throwable> errors) {
            // disable; don't fail if has no test methods
        }

        // XXX: We override methods called by BlockJUnit4ClassRunner.methodBlock()
        // so that we can change it to surround another test fixture instead of being the leaf fixture itself.

        public Object createFreshTest() throws Exception {
            currentTest = super.createTest();
            return createTest();
        }

        @Override
        protected Object createTest() throws Exception {
            if (currentTest == null) {
                throw new IllegalStateException();
            }
            return currentTest;
        }

        public Statement withParentFixture(FrameworkMethod method, Statement next) {
            nestedFixture = next;
            try {
                return methodBlock(method);
            } finally {
                nestedFixture = null;
                currentTest = null;
            }
        }

        @Override
        protected Statement methodInvoker(FrameworkMethod method, Object test) {
            if (nestedFixture == null) {
                throw new IllegalStateException();
            }
            return nestedFixture;
        }

        @Override
        protected Statement possiblyExpectingExceptions(FrameworkMethod method, Object test, Statement next) {
            return next;
        }

        @Override
        protected Statement withPotentialTimeout(FrameworkMethod method, Object test, Statement next) {
            return next;
        }
    }
}
