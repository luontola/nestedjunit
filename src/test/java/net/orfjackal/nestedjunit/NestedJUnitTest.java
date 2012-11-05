// Copyright © 2009-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.nestedjunit;

import org.junit.*;
import org.junit.internal.TextListener;
import org.junit.rules.TestRule;
import org.junit.runner.*;
import org.junit.runners.model.Statement;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class NestedJUnitTest {

    private static final List<String> spy = new ArrayList<String>();

    private final JUnitCore junit = new JUnitCore();

    {
        junit.addListener(new TextListener(System.out));
    }

    @Before
    public void resetGlobals() {
        spy.clear();
    }


    @Test
    public void level_1_nesting() throws Exception {
        Result result = junit.run(Level1Nesting.class);

        assertThat("success", result.wasSuccessful(), is(true));
        assertThat(spy, is(Arrays.asList("L1 before", "L1 test", "L1 after")));
    }

    @RunWith(NestedJUnit.class)
    public static class Level1Nesting {

        @Before
        public void before() {
            spy.add("L1 before");
        }

        @After
        public void after() {
            spy.add("L1 after");
        }

        @Test
        public void foo() {
            spy.add("L1 test");
        }
    }


    @Test
    public void level_2_nesting() throws Exception {
        Result result = junit.run(Level2Nesting.class);

        assertThat("success", result.wasSuccessful(), is(true));
        assertThat(spy, is(Arrays.asList("L1 before 1", "L2 before 2", "L2 test 3", "L2 after 4", "L1 after 5")));
    }

    @RunWith(NestedJUnit.class)
    public static class Level2Nesting {

        private int shared = 0;

        @Before
        public void before() {
            shared++;
            spy.add("L1 before " + shared);
        }

        @After
        public void after() {
            shared++;
            spy.add("L1 after " + shared);
        }

        public class Foo {

            @Before
            public void before() {
                shared++;
                spy.add("L2 before " + shared);
            }

            @After
            public void after() {
                shared++;
                spy.add("L2 after " + shared);
            }

            @Test
            public void foo() {
                shared++;
                spy.add("L2 test " + shared);
            }
        }
    }


    @Test
    public void descriptions_of_both_level_1_and_2_tests_are_in_the_same_tree() throws Exception {
        NestedJUnit runner = new NestedJUnit(MultipleLevels.class);

        Description level1 = runner.getDescription();

        assertThat(level1.getDisplayName(), is(MultipleLevels.class.getName()));
        assertThat(level1.getChildren(), containsInAnyOrder(
                Description.createTestDescription(MultipleLevels.class, "level_1_test_1"),
                Description.createTestDescription(MultipleLevels.class, "level_1_test_2"),
                Description.createSuiteDescription(MultipleLevels.Level2.class))
        );

        Description level2 = findChild(level1, MultipleLevels.Level2.class);
        assertThat(level2.getChildren(), is(containsInAnyOrder(
                Description.createTestDescription(MultipleLevels.Level2.class, "level_2_test_1"),
                Description.createTestDescription(MultipleLevels.Level2.class, "level_2_test_2")
        )));
    }

    private static Description findChild(Description haystack, Class<MultipleLevels.Level2> needle) {
        for (Description desc : haystack.getChildren()) {
            if (desc.getTestClass().equals(needle)) {
                return desc;
            }
        }
        throw new AssertionError("Did not find " + needle + " in " + haystack);
    }

    @RunWith(NestedJUnit.class)
    public static class MultipleLevels {

        @Test
        public void level_1_test_1() {
        }

        @Test
        public void level_1_test_2() {
        }

        public class Level2 {

            @Test
            public void level_2_test_1() {
            }

            @Test
            public void level_2_test_2() {
            }
        }
    }


    @Test
    public void evaluates_rules_from_all_levels() {
        Result result = junit.run(Rules.class);

        assertThat("success", result.wasSuccessful(), is(true));
        assertThat(spy, is(Arrays.asList("R1 start", "R2 start", "L2 test", "R2 end", "R1 end")));
    }

    @RunWith(NestedJUnit.class)
    public static class Rules {

        @Rule
        public SpyRule rule1 = new SpyRule("R1 start", "R1 end");

        public class Foo {

            @Rule
            public SpyRule rule2 = new SpyRule("R2 start", "R2 end");

            @Test
            public void foo() {
                spy.add("L2 test");
            }
        }
    }

    private static class SpyRule implements TestRule {
        private final String startMessage;
        private final String endMessage;

        public SpyRule(String startMessage, String endMessage) {
            this.startMessage = startMessage;
            this.endMessage = endMessage;
        }

        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    spy.add(startMessage);
                    try {
                        base.evaluate();
                    } finally {
                        spy.add(endMessage);
                    }
                }
            };
        }
    }


    @Test
    public void evaluates_class_rules_from_top_level_around_all_levels() {
        Result result = junit.run(ClassRules.class);

        assertThat("success", result.wasSuccessful(), is(true));
        assertThat(spy, is(Arrays.asList("CR start", "before class", "L1 tests", "L2 tests", "after class", "CR end")));
    }

    @RunWith(NestedJUnit.class)
    public static class ClassRules {

        @ClassRule
        public static SpyRule rule = new SpyRule("CR start", "CR end");

        @BeforeClass
        public static void before() {
            spy.add("before class");
        }

        @AfterClass
        public static void afterClass() {
            spy.add("after class");
        }

        @Test
        public void foo() {
            spy.add("L1 tests");
        }

        @Test
        public void bar() {
        }

        public class Foo {

            @Test
            public void foo() {
                spy.add("L2 tests");
            }

            @Test
            public void bar() {
            }
        }
    }


    // FIXME: fails if the test class contains public (static or non-static) non-test member classes
    // TODO: level 3 (arbitrary) nesting
    // TODO: raise an exception if there are no tests in any of the levels
    // TODO: static member classes with tests? error, ignore, or run without parent fixture?
}
