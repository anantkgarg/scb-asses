package com.scb.assessment.engine;

import javolution.util.FastMap;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class DeadlineEngineImpl implements DeadlineEngine{

    private Map<Long, Map> scheduledTasksMap;

    public DeadlineEngineImpl(Map<Long, Map> scheduledTasksMap) {
        this.scheduledTasksMap = scheduledTasksMap;
    }

    public DeadlineEngineImpl(int size) {
        this(new FastMap(size).shared());
    }

    @Override
    public long schedule(long deadlineMs) {
        if(deadlineMs <= 0)
            throw new RuntimeException("Invalid Scheduling Time " + deadlineMs);

        long mainIndex = getMainIndex(deadlineMs);

        Map tasks = scheduledTasksMap.get(mainIndex);
        if (tasks == null) {
            tasks = Collections.synchronizedMap(new FastMap<>());
            scheduledTasksMap.put(mainIndex, tasks);
        }

        long taskIdentifier = Long.MIN_VALUE;
        synchronized (tasks) {
            taskIdentifier = getScheduleId(deadlineMs, tasks.size());

            tasks.put(taskIdentifier, deadlineMs);
        }

        return taskIdentifier;
    }

    @Override
    public boolean cancel(long requestId) {
        long mainIndex = requestId / 100;
        Map tasks = scheduledTasksMap.get(mainIndex);
        if (tasks != null) {
            synchronized (tasks) {
                Object task = tasks.remove(requestId);
                if (task != null) {
                    if(tasks.isEmpty()) {
                        scheduledTasksMap.remove(mainIndex);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int poll(long nowMs, Consumer<Long> handler, int maxPoll) {
        if(nowMs <= 0)
            throw new RuntimeException("Invalid Scheduling Time " + nowMs);

        if(handler == null)
            throw new NullPointerException("Consumer cannot be null");

        if(maxPoll <= 0)
            return 0;

        Collection<Long> triggeredIds = new ArrayList();
        long mainIndex = getMainIndex(nowMs);
        List<Long> eligibleIndices = scheduledTasksMap.keySet().stream().filter(index -> index <= mainIndex).collect(Collectors.toList());
        for(Long index : eligibleIndices) {
            Map tasksMap = scheduledTasksMap.get(index);
            Set tasksKeys = tasksMap.keySet();
            synchronized (tasksMap) {
                Iterator<Long> iterator = tasksKeys.iterator();

                while (iterator.hasNext() && triggeredIds.size() < maxPoll) {
                    Long triggerId = iterator.next();
                    triggeredIds.add(triggerId);
                    handler.accept(triggerId);
                    iterator.remove();
                }

                if (tasksMap.isEmpty()) {
                    scheduledTasksMap.remove(index);
                }
            }
        }
        return triggeredIds.size();
    }

    @Override
    public int size() {
        return scheduledTasksMap.values().stream().mapToInt(tasks -> tasks.size()).sum();
    }

    private long getMainIndex(long time) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(time);

        //Format this time to YYYYMMDDHHmmSSsss --> This is main index that we will use to store our tasks in cache.
        String longFormat = format("%4d%02d%02d%02d%02d%02d%03d", calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), calendar.get(Calendar.MILLISECOND));

        return Long.parseLong(longFormat);
    }

    private long getScheduleId(long time, int size) {
        long mainKey = getMainIndex(time);

        //Format this to MainIndex+XX.
        String longFormat = format("%17d%02d", mainKey, size);
        return Long.parseLong(longFormat);
    }
}
