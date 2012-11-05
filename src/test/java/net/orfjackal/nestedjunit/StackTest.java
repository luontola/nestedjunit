// Copyright © 2009-2012 Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.nestedjunit;

import org.junit.*;
import org.junit.runner.RunWith;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

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
