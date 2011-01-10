/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pivot.util.concurrent;

/**
 * Class that runs a group of tasks in parallel and notifies listeners
 * when all tasks are complete.
 */
public class ParallelTaskGroup extends TaskGroup {
    @Override
    @SuppressWarnings("unchecked")
    public synchronized Void execute() throws TaskExecutionException {
        TaskListener<Object> taskListener = new TaskListener<Object>() {
            @Override
            public void taskExecuted(Task<Object> task) {
                synchronized (ParallelTaskGroup.this) {
                    complete++;
                    ParallelTaskGroup.this.notify();
                }
            }
        };

        complete = 0;
        for (Task<?> task : tasks) {
            ((Task<Object>)task).execute(taskListener);
        }

        while (complete < tasks.size()) {
            try {
                wait();
            } catch (InterruptedException exception) {
                throw new TaskExecutionException(exception);
            }
        }

        return null;
    }
}