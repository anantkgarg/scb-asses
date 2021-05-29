package com.scb.assessment.engine;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DeadlineEngineImplTest {

    DeadlineEngine deadlineEngine;
    Calendar calendar;

    @Before
    public void setUp() {
        calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.set(2021, Calendar.MAY, 28, 7, 15, 1);
        calendar.set(Calendar.MILLISECOND, 0);

        deadlineEngine = new DeadlineEngineImpl(4);
    }

    @Test(expected = RuntimeException.class)
    public void testScheduleException() {
        deadlineEngine.schedule(-10);
    }

    @Test
    public void testSchedule() {
        long expectedId_1 = 2021052807150100000L;
        long expectedId_2 = 2021052807150100001L;

        assertEquals("TaskID don't match - 1", expectedId_1, deadlineEngine.schedule(calendar.getTimeInMillis()));
        assertEquals("TaskID don't match - 2", expectedId_2, deadlineEngine.schedule(calendar.getTimeInMillis()));
        calendar.set(Calendar.MILLISECOND, 1);
        assertEquals("TaskID don't match - 3", expectedId_1+100, deadlineEngine.schedule(calendar.getTimeInMillis()));
    }

    @Test
    public void testCancel() {
        long expectedId_1 = deadlineEngine.schedule(calendar.getTimeInMillis());
        long expectedId_2 = 2021052807150100001L;

        assertFalse("Job cancel failed - HOW ITS CANCELLED !!!", deadlineEngine.cancel(expectedId_2));
        assertTrue("Job cancel failed - Could not cancel", deadlineEngine.cancel(expectedId_1));
    }

    @Test
    public void testSize() {
        long expectedId_1 = deadlineEngine.schedule(calendar.getTimeInMillis());
        assertEquals("Size after after batch failed - 1", 1, deadlineEngine.size());

        deadlineEngine.schedule(calendar.getTimeInMillis());
        assertEquals("Size after after batch failed - 2", 2, deadlineEngine.size());

        calendar.set(Calendar.MILLISECOND, 10);
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        assertEquals("Size after after batch failed - 3", 5, deadlineEngine.size());

        calendar.set(Calendar.MILLISECOND, 11);
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        assertEquals("Size after after batch failed - 4", 9, deadlineEngine.size());

        calendar.set(Calendar.MILLISECOND, 310);
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());

        calendar.set(Calendar.MILLISECOND, 670);
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        assertEquals("Size after after batch failed - 5", 14, deadlineEngine.size());

        deadlineEngine.cancel(expectedId_1);
        assertEquals("Size after cancellation failed", 13, deadlineEngine.size());
    }

    @Test(expected = NullPointerException.class)
    public void testPollNPEException() {
        deadlineEngine.poll(calendar.getTimeInMillis(), null, 1);

        Consumer mock = mock(Consumer.class);
        deadlineEngine.poll(-1, mock, 1);
    }

    @Test(expected = RuntimeException.class)
    public void testPollException() {
        Consumer mock = mock(Consumer.class);
        deadlineEngine.poll(-1, mock, 1);
    }

    @Test
    public void testPoll() {
        deadlineEngine.schedule(calendar.getTimeInMillis());
        assertEquals("Size after after batch failed - 1", 1, deadlineEngine.size());

        deadlineEngine.schedule(calendar.getTimeInMillis());
        assertEquals("Size after after batch failed - 2", 2, deadlineEngine.size());

        calendar.set(Calendar.MILLISECOND, 10);
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        assertEquals("Size after after batch failed - 3", 5, deadlineEngine.size());

        calendar.set(Calendar.MILLISECOND, 11);
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        assertEquals("Size after after batch failed - 4", 9, deadlineEngine.size());

        calendar.set(Calendar.MILLISECOND, 310);
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());

        calendar.set(Calendar.MILLISECOND, 670);
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        deadlineEngine.schedule(calendar.getTimeInMillis());
        assertEquals("Size after after batch failed - 5", 14, deadlineEngine.size());

        Consumer mock = mock(Consumer.class);

        //We only have 9 schedules in first 100 ms, testing it out for that in batches 0+5+4
        calendar.set(Calendar.MILLISECOND, 100);
        assertEquals("Invocation 1 failed", 0, deadlineEngine.poll(calendar.getTimeInMillis(), mock, 0));
        verify(mock, times(0)).accept(any(Long.class));

        assertEquals("Invocation 2 failed", 5, deadlineEngine.poll(calendar.getTimeInMillis(), mock, 5));
        verify(mock, times(5)).accept(any(Long.class));

        //Max poll is 5, how ever we are only left with 4 schedules in this range, hence expectation is 4
        mock = mock(Consumer.class);
        assertEquals("Invocation 3 failed", 4, deadlineEngine.poll(calendar.getTimeInMillis(), mock, 5));
        verify(mock, times(4)).accept(any(Long.class));

        calendar.set(Calendar.MILLISECOND, 600);
        mock = mock(Consumer.class);
        assertEquals("Invocation 3 failed", 2, deadlineEngine.poll(calendar.getTimeInMillis(), mock, 5));
        verify(mock, times(2)).accept(any(Long.class));

        assertEquals("Size after polling", 3, deadlineEngine.size());
    }
}