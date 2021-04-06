if (threading.isMainThread) {
  console.log('[main] Running under the main thread');

  const pool = new WorkerThreadPool(__filename, 2);
  pool.on('free', worker => console.log(`[main | thread #${worker.threadId}] Worker is free!`));

  pool.run({ result: 'uwu' }).then(({ worker, result }) => {
    console.log(`[main | thread #${worker.threadId}] Received '${result}'`);

    pool.end();
    process.exit(0);
  }).catch(error => {
    console.error('[main] Received a error from worker', error);
    process.exit(1);
  });
} else {
  console.log(`[thread ${threading.threadId}] Spawned as a worker`);
  threading.parentPort?.on('message', (data) => {
    if (data.result === 'uwu') threading.parentPort?.postMessage('owo');
  });
}
