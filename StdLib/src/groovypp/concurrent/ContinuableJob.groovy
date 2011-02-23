/*
 * Copyright 2009-2011 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovypp.concurrent

import java.util.concurrent.Executor

import java.util.concurrent.Callable

@Typed abstract class ContinuableJob<V> extends BindLater<V> implements Runnable, Callable<V> {

  private Executor          executor
  private ContinuableJob    owner

  private volatile FList<ContinuableJob> subJobs = FList.emptyList

  private static ThreadLocal<ContinuableJob> currentJob = []

  static <S> ContinuableJob<S> job(Executor executor, ContinuableJob<S> job) {
    def cj = currentJob.get()
    if(cj) {
      cj.job job
    }
    else {
      if(job.executor)
        throw new IllegalStateException("Executor is already set for ContinuableJob")
      job.executor = executor

      executor.execute job
      job
    }
  }

  static <T> ContinuableJob<T> doneJob(T value) {
    ContinuableJob<T> result = { -> (T)null }
    result.set(value)
    result
  }

  final <S> ContinuableJob<S> job(ContinuableJob<S> job) {
      if(currentJob.get() != this)
        throw new IllegalStateException("Subjob can be attached only while job is running")

      if(job.executor)
        throw new IllegalStateException("Executor is already set for ContinuableJob")

      job.executor = executor

      for(;;) {
        def sj = subJobs
        if(subJobs.compareAndSet(sj, sj + job))
          break
      }
  }

  protected void whenChildJobReady(ContinuableJob subJob) {
  }

  protected void whenChildrenJobsReady() {
  }

  protected void done() {
    executor = null

    cancelSubJobs()

    super.done () // invoke listeners


  }

  final void run () {
    if (setRunningThread()) {
        currentJob.set(this)

        def value
        try {
            value = call ()
            currentJob.set(null)
        }
        catch (Throwable ex) {
            currentJob.set(null)
            throwException(ex)
            return
        }
    }
    else {
      throwException(new IllegalStateException("Job $this is started already"))
    }
  }

  private void throwException(Throwable ex) {
    cancelSubJobs()
    setException(ex)
  }

  private void cancelSubJobs() {
    for (;;) {
      def sj = subJobs
      if (subJobs.compareAndSet(sj, FList.emptyList)) {
        for (job in sj) {
          job.cancel(true)
        }
        break
      }
    }
  }
}
