/**
 * Copyright (c) 2020-2021 Noel
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

const { promises: fs, createReadStream } = require('fs');
const { isMainThread, parentPort } = require('worker_threads');
const { createHash } = require('crypto');
const { HttpClient } = require('@augu/orchid');
const { hostname } = require('os');
const { readdir } = require('@augu/utils');
const { version } = require('../package.json');
const { Logger } = require('tslog');

const http = new HttpClient({
  userAgent: `hana / v${version}`,
});

const logger = new Logger({
  displayFunctionName: true,
  exposeErrorCodeFrame: true,
  displayInstanceName: true,
  displayFilePath: 'hideNodeModulesOnly',
  dateTimePattern: '[ day-month-year / hour:minute:second ]',
  displayTypes: false,
  instanceName: hostname(),
  name: '花 ("hana") ~ scripts/e621',
});

if (isMainThread) {
  logger.error('Do not call this with `node`!');
  process.exit(1);
}

const hashPath = (path) => {
  const hash = createHash('md5');
  const stream = createReadStream(path);

  return new Promise((resolve, reject) => {
    stream.on('error', reject);
    hash.once('readable', () => resolve(hash.read().toString('hex')));

    stream.pipe(hash);
  });
};

const rehydrate = async () => {
  logger.info('Running e621 script...');

  const sources = {};
  const hashes = {};
  const tags = {};
  const files = await readdir('E:\\Images\\Yiff', { exclude: ['videos', 'comics'] });
  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    const hash = await hashPath(file);
    const res = await http.request({
      method: 'GET',
      url: `https://e621.net/posts.json?md5=${hash}`,
    });

    const data = res.json();
    logger.info(`[ ${i + 1} / ${files.length} ] ${res.status} ~> ${hash}`);
    hashes[file] = hash;

    if (res.statusCode === 200) {
      sources[file] = data.post.sources;
      tags[file] = {
        characters: data.post.tags.character,
        copyright: data.post.tags.copyright,
        artists: data.post.artist.filter((owo) => owo !== 'conditional_dnp'),
      };
    }
  }

  logger.info(`Found ${Object.entries(sources).length} sources and ${Object.entries(tags).length} tags`);
  await fs.writeFile(
    '../assets/yiff.json',
    JSON.stringify(
      {
        version: 2,
        sources,
        hashes,
        tags,
      },
      null,
      '\t'
    )
  );

  async function cleanup() {
    const { sources, tags, hashes, version } = require('../assets/yiff.json');
    const cleanS = {};
    const cleanT = {};

    for (const key of Object.keys(sources)) {
      const path = key.split('\\');
      const p = path[path.length - 1];

      cleanS[p] = sources[key];
    }

    for (const key of Object.keys(tags)) {
      const path = key.split('\\');
      const p = path[path.length - 1];

      cleanT[p] = tags[key];
    }

    await fs.writeFile(
      '../assets/yiff.json',
      JSON.stringify(
        {
          version,
          sources,
          hashes,
          tags,
        },
        null,
        '\t'
      )
    );
  }

  await cleanup();
  logger.info('Re-hydrated yiff cache');
  parentPort.postMessage({ done: true });
};

parentPort.once('message', async (data) => {
  if (data.hydrate === true) await rehydrate();
});
