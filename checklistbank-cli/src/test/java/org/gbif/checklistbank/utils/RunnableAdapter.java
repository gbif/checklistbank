package org.gbif.checklistbank.utils;

import java.util.concurrent.Callable;

/**
 * A callable that runs given task and returns given result.
 * Copied from java8 sources.
 */
public class RunnableAdapter<T> implements Callable<T> {
    final Runnable task;
    final T result;

    public RunnableAdapter(Runnable task, T result) {
        this.task = task;
        this.result = result;
    }

    @Override
    public T call() {
        task.run();
        return result;
    }
}