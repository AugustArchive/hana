const { promises: fs, createReadStream } = require('fs');
const { createHash } = require('crypto');
const { join } = require('path');
const https = require('https');

/**
 * Asynchronouslly read a directory's contents and returns a list of files
 * @param {string} path The path directory to get files from
 * @returns {Promise<string[]>} Returns a list of files available
 */
const readdir = async (path, skip = []) => {
  let results = [];
  const files = await fs.readdir(path);

  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    const stats = await fs.lstat(join(path, file));

    if (stats.isDirectory()) {
      if (skip.includes(file)) continue;

      const r = await readdir(join(path, file));
      results = results.concat(r);
    } else {
      results.push(join(path, file));
    }
  }

  return results;
};

const getHash = (path) => {
  const hash = createHash('md5');
  const stream = createReadStream(path);

  return new Promise((resolve, reject) => {
    stream.on('error', reject);
    hash.once('readable', () => resolve(hash.read().toString('hex')));

    stream.pipe(hash);
  });  
};

const request = (hash) => {
  return new Promise((resolve, reject) => {
    let body = Buffer.alloc(0);
    const url = new URL(`https://e621.net/posts.json?md5=${hash}`);

    const r = https.request({
      protocol: url.protocol,
      method: 'GET',
      path: `${url.pathname}${url.search}`,
      port: url.port,
      host: url.hostname,
      headers: {
        'user-agent': 'api.floofy.dev/2.4.0'
      }
    }, (res) => {
      res.on('error', reject);
      res.on('data', chunk => (body = Buffer.concat([body, chunk])));
      res.on('end', () => {
        let payload;
        try {
          payload = JSON.parse(body.toString());
        } catch(ex) {
          return reject(new Error('Unable to parse JSON'));
        }

        return resolve({ status: res.statusCode, data: payload });
      });
    });

    r.on('error', console.error);
    r.end();
  });
};

const hashes = {};
const sources = {};
const tags = {};

async function main() {
  const files = await readdir('D:\\Cache\\Yiff', ['videos']);

  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    const hash = await getHash(file);
    let data;

    try {
      data = await request(hash);
    } catch(ex) {
      console.error(`[${i + 1}/${files.length}] Unable to get source from hash ${hash}\n`, ex);
      continue;
    }

    console.log(`[${i + 1}/${files.length} | ${data.status}] ${file} -> ${hash}`);
    hashes[file] = hash;
    
    if (data.status === 200) {
      sources[file] = data.data.post.sources;
      tags[file] = {
        characters: data.data.post.tags.character,
        copyright: data.data.post.tags.copyright,
        artists: data.data.post.tags.artist.filter(r => r !== 'conditional_dnp')
      };
    }
  }

  console.log(`Found ${Object.keys(sources).length} sources and tags.`);
  await fs.writeFile('./src/main/resources/yiff/hashes.json', JSON.stringify(hashes, null, 4));
  await fs.writeFile('./src/main/resources/yiff/sources.json', JSON.stringify(sources, null, 4));
  await fs.writeFile('./src/main/resources/yiff/tags.json', JSON.stringify(tags, null, 4));

  cleanup();
}

async function cleanup() {
  const sources = require('../src/main/resources/yiff/sources.json');
  const tags = require('../src/main/resources/yiff/tags.json');

  const cleanS = {};
  const cleanT = {};

  for (const key of Object.keys(sources)) {
    const paths = key.split('\\');
    const p = paths[paths.length - 1];

    cleanS[p] = sources[key];
  }

  for (const key of Object.keys(tags)) {
    const paths = key.split('\\');
    const p = paths[paths.length - 1];

    cleanT[p] = tags[key];
  }

  await fs.writeFile('./src/main/resources/yiff/sources.json', JSON.stringify(cleanS, null, 4));
  await fs.writeFile('./src/main/resources/yiff/tags.json', JSON.stringify(cleanT, null, 4));
}

main();
