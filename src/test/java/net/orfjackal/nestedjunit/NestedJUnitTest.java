// Copyright © 2009-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.nestedjunit;

import org.junit.*;
import org.junit.internal.TextListener;
import org.junit.runner.*;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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


    @Ignore("not implemented") // TODO
    @Test
    public void level_1_nesting() throws Exception {
        Result result = junit.run(new NestedJUnit(Level1Nesting.class));

        assertThat("success", result.wasSuccessful(), is(true));
        assertThat(spy, is(Arrays.asList("L1 before", "L1 test", "L1 after")));
    }

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
        Result result = junit.run(new NestedJUnit(Level2Nesting.class));

        assertThat("success", result.wasSuccessful(), is(true));
        assertThat(spy, is(Arrays.asList("L1 before", "L2 before", "L2 test", "L2 after", "L1 after")));
    }

    public static class Level2Nesting {

        @Before
        public void before() {
            spy.add("L1 before");
        }

        @After
        public void after() {
            spy.add("L1 after");
        }

        public class Foo {

            @Before
            public void before() {
                spy.add("L2 before");
            }

            @After
            public void after() {
                spy.add("L2 after");
            }

            @Test
            public void foo() {
                spy.add("L2 test");
            }
        }
    }
}
