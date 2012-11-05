// Copyright © 2009-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.nestedjunit;

import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.*;

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
public class NestedJUnit extends BlockJUnit4ClassRunner {

    private final List<LeafFixture> level2 = new ArrayList<LeafFixture>();

    public NestedJUnit(Class<?> testClass) throws InitializationError {
        super(testClass);

        for (Class<?> nestedClass : testClass.getClasses()) {
            level2.add(new LeafFixture(nestedClass, new SurroundingFixture(testClass)));
        }
    }

    // TODO: Override computeTestMethods() instead and require at least one level 1 or 2 test
    @Override
    protected void validateInstanceMethods(List<Throwable> errors) {
        // disable; don't fail if the top level has no test methods
    }

    @Override
    protected Statement childrenInvoker(final RunNotifier notifier) {
        final Statement level1 = super.childrenInvoker(notifier);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                level1.evaluate();
                for (LeafFixture leafFixture : level2) {
                    leafFixture.run(notifier);
                }
            }
        };
    }

    @Override
    public Description getDescription() {
        Description description = super.getDescription();
        for (LeafFixture leafFixture : level2) {
            description.addChild(leafFixture.getDescription());
        }
        return description;
    }


    private static class LeafFixture extends BlockJUnit4ClassRunner {

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
            // disable; don't fail if the top level has no test methods
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
