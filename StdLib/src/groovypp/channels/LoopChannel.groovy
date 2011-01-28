/*
 * Copyright 2009-2011 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovypp.channels

@Typed abstract class LoopChannel<O extends SupervisedChannel> extends SupervisedChannel<O> {
    protected volatile boolean stopped
    protected volatile Thread  currentThread

    protected abstract boolean doLoopAction ()

    protected void doStartup() {
        executor.execute {
            currentThread = Thread.currentThread()
            try {
                while (!stopped) {
                    if (!doLoopAction()) break
                }
            }
            catch(Throwable t) {
                if(!stopped) {
                  stopped = true
                  crash(t)
                }
            }
        }
    }

    protected void doShutdown() {
        stopped = true
        currentThread?.interrupt()
    }
}
