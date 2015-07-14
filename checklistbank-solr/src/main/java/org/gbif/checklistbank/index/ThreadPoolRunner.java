package org.gbif.checklistbank.index;


import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class was implemented to be executed only one time, and probably be executed by a job scheduler (CRON for example).
 * The behavior of the pool of threads can be customized using the properties file, using this file
 * * is possible define the values for: poolSize, maxPoolSize and keepAliveTime; and additionally for the
 * e-mail configuration (an email will be sent at the end of the process):
 *  smtp.server,email.to,email.from,email.subject,email.body,email.cc
 * The thread pool is implemented using an {@link ExecutorCompletionService} and a {@link ThreadPoolExecutor} because
 * some results can be return for each job: errors during the process or any other information.
 */
public abstract class ThreadPoolRunner<T> {


  private static class DefaultRejectedExecutionHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable job, ThreadPoolExecutor executor) {
      ThreadPoolRunner.LOG.debug("Job has been rejected by the executor, putting on queue");
      try {
        executor.getQueue().put(job);
      } catch (InterruptedException e1) {
        LOG.error("Work discarded, thread was interrupted while waiting for space to schedule: {}", job);
      }
    }
  }


  /**
   * the number of threads to keep in the pool, even if they are idle.
   */
  protected int poolSize = 0;

  /**
   * Executes each submitted task (agent synchronization) using one of possibly several pooled threads
   */
  private ThreadPoolExecutor threadPool = null;

  /**
   * Decouples the production of new asynchronous tasks from the consumption of the results of completed tasks
   */
  private CompletionService<T> completionService;

  /**
   * Queue of elements that will be executed by the {@link ThreadPoolExecutor}
   */
  private final BlockingQueue<Runnable> queue = new SynchronousQueue<Runnable>();

  /**
   * Logger for the {@link ThreadPoolRunner} class
   */
  private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolRunner.class);

  /**
   * @param poolSize the number of threads to keep in the pool, even if they are idle.
   */
  public ThreadPoolRunner(int poolSize) {
    this.poolSize = poolSize;
  }

  protected abstract Callable<T> newJob();

  /**
   * Initialize the {@link ThreadPoolExecutor} and {@link ExecutorCompletionService}
   */
  private void initThreadPool() {
    threadPool = new ThreadPoolExecutor(
      poolSize, poolSize, 0, TimeUnit.SECONDS, queue, new DefaultRejectedExecutionHandler()
    );
    completionService = new ExecutorCompletionService<T>(threadPool);
  }


  /**
   * This method runs the synchronization for all the agent of the same type,
   * creates a thread that will synchronized # of agents (defined by the agentsPerThread field)
   */
  public int run() {
    // localTasksCount contains the number of threads created, this count will be used for shutting down the service

    initThreadPool();

    int localTasksCount = 0;
    try {
      // gets the agents with service type: this.serviceTypeCode
      Callable<T> job = newJob();
      while (job != null) {
        try {
          LOG.debug("Submitting job {}, tasks count={}", job, localTasksCount);
          completionService.submit(job);
          localTasksCount += 1;
        } catch (Exception e) {
          LOG.error("Error when submiting job: {}", localTasksCount);
        }
        job = newJob();
      }
      LOG.info("All {} jobs submitted succesfully!", localTasksCount);
      shutdownService(localTasksCount);
    } catch (Exception e) {
      LOG.error("Error when submiting jobs: {}", e);
    }

    return localTasksCount;
  }

  /**
   * Performs the shutdown activities: waits each thread to complete the process and collects the results.
   */
  protected void shutdownService(int tasksCount) {
    try {
      LOG.info("Initiating shutting down of the service");
      while (tasksCount > 0) {
        // waits for each task to return the results
        Future<T> result = completionService.take();
        tasksCount--;
        try {
          T taskResponse = result.get();
          LOG.info("Job completed, {} still running", tasksCount);
          taskResponseHook(taskResponse);

        } catch (ExecutionException e) {
          LOG.error("Error waiting a job to return results. {} still running jobs", tasksCount, e);
        }
        LOG.debug("Waiting for {} jobs to shutdown.", tasksCount);
      }
      List<Runnable> failedTasks = threadPool.shutdownNow();
      LOG.debug("{} Tasks failed or forcibly removed from the thread pool", failedTasks.size());
    } catch (Exception ignored) {
      LOG.error("Thread pool prematurely terminated", ignored);
    }
    LOG.info("Shutdown complete");
  }

  /**
   * Override this method if the main runner needs to consume the individual job results.
   * @param taskResponse the finished job result
   */
  protected void taskResponseHook (T taskResponse) {

  }
}
