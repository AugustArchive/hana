/**
 * Copyright (c) 2020-2021 August
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import AsyncPoolResource from './AsyncResource';
import type { Logger } from 'tslog';
import { Worker } from 'worker_threads';
import logger from '../singletons/logger';

const RESOURCE_INFO = Symbol('$hana::threading::resource-info');

/**
 * Represents a pool of threaded workers for parallel JavaScript execution
 */
export default class WorkerThreadPool {
  /**
   * Number of threads to take for this [WorkerThreadPool]
   */
  public numThreads: number;

  /**
   * Logger for logging messages
   */
  private logger: Logger;

  /**
   * List of workers available
   */
  public worker: Worker | null = null;

  /**
   * Creates a new [WorkerThreadPool] instance
   * @param filename The filename to run
   * @param numThreads Number of threads to take for this [WorkerThreadPool]
   */
  constructor(numThreads: number) {
    this.numThreads = numThreads;
    this.logger = logger.getChildLogger({
      name: '花 ("hana") ~ thread-pool'
    });
  }

  run<T = unknown, TTask extends Record<string, unknown> = {}>(filename: string, task: TTask, env?: { [x: string]: string }) {
    if (this.worker === null) {
      const worker = new Worker(filename, { stdout: true, stderr: true, env });
      worker.on('message', result => {
        worker[RESOURCE_INFO]?.done(null, result);
        worker[RESOURCE_INFO] = null;
      });

      worker.on('error', error => {
        worker[RESOURCE_INFO]?.done(error);
        this.logger.error(`Worker #${worker.threadId} was unable to resolve data`, error);
      });

      // Log messages from files
      worker.stdout.on('data', buf => console.info(buf.toString()));
      worker.stderr.on('data', buf => console.error(buf.toString()));

      this.worker = worker;
      this.logger.info(`Dispatched worker #${worker.threadId} to run file ${filename}`);
    } else {
      this.logger.info(`Worker #${this.worker.threadId} is gonna run file ${filename} again.`);
    }

    return new Promise<T>((resolve, reject) => {
      this.worker![RESOURCE_INFO] = new AsyncPoolResource(this.worker!, (w, error, result) => {
        if (error) return reject(error);
        if (this.worker!.threadId !== w.threadId) return reject(new Error(`Received data on thread ${w.threadId} when it was on thread ${this.worker!.threadId}`));

        return resolve(result as T);
      });

      this.worker!.postMessage(task);
    });
  }

  destroy() {
    this.logger.warn(`Terminated thread #${this.worker!.threadId}`);
    this.worker?.terminate();
    this.worker = null;
  }
}
