// Copyright © 2009-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.nestedjunit;

import org.junit.*;
import org.junit.internal.TextListener;
import org.junit.runner.*;

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


    // TODO: level 3 (arbitrary) nesting
}
