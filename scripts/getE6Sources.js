const { promises: fs, createReadStream } = require('fs');
const loadProperties = require('./loadProperties');
const { createHash } = require('crypto');
const { join } = require('path');
const https = require('https');

let metadata;

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
        'user-agent': `api.floofy.dev/${metadata['app.version']}`
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

const hashes = { yiff: [], bulge: [] };
const sources = {};
const tags = {};

async function main() {
  metadata = await loadProperties(join(__dirname, '..', 'src', 'main', 'resources', 'app.properties'));

  const yiff = await readdir('D:/Cache/Yiff/gay');
  const bulge = await readdir('D:/Cache/Yiff/bulge');

  console.log(`[info] Found ${yiff.length} gay and ${bulge.length} bulge images!`);

  for (let i = 0; i < yiff.length; i++) {
    const file = yiff[i];
    const hash = await getHash(file);
    let data;

    try {
      data = await request(hash);
    } catch(ex) {
      console.error(`[${i + 1}/${yiff.length} ~ yiff] Unable to fetch source from hash '${hash}'\n`, ex);
      continue;
    }

    console.log(`[${i + 1}/${yiff.length} ~ yiff ~ ${data.status}] ${file} | ${hash}`);
    hashes.yiff.push(hash);

    if (data.source === 200) {
      sources.yiff[file] = data.data.post.sources;
      tags.yiff[file] = {
        characters: data.data.post.tags.character,
        copyright: data.data.post.tags.copyright,
        artists: data.data.post.tags.artist.filter(r => r !== 'conditional_dnp')
      };
    }
  }

  for (let i = 0; i < bulge.length; i++) {
    const file = bulge[i];
    const hash = await getHash(file);
    let data;

    try {
      data = await request(hash);
    } catch(ex) {
      console.error(`[${i + 1}/${bulge.length} ~ bulge] Unable to fetch source from hash '${hash}'\n`, ex);
      continue;
    }

    console.log(`[${i + 1}/${bulge.length} ~ bulge ~ ${data.status}] ${file} | ${hash}`);
    hashes.bulge.push(hash);

    if (data.source === 200) {
      sources.bulge[file] = data.data.post.sources;
      tags.bulge[file] = {
        characters: data.data.post.tags.character,
        copyright: data.data.post.tags.copyright,
        artists: data.data.post.tags.artist.filter(r => r !== 'conditional_dnp')
      };
    }
  }

  console.log(`[info ~ yiff] Found ${Object.keys(sources.yiff).length} sources and ${Object.keys(sources.tags).length} tags.`);
  console.log(`[info ~ bulge] Found ${Object.keys(sources.yiff).length} sources and ${Object.keys(sources.tags).length} tags.`);

  await Promise.all(
    fs.writeFile('./src/main/resources/yiff/sources.json', JSON.stringify(sources, null, 4)),
    fs.writeFile('./src/main/resources/yiff/hashes.json', JSON.stringify(hashes, null, 4)),
    fs.writeFile('./src/main/resources/yiff/tags.json', JSON.stringify(tags, null, 4))
  );

  //cleanup();
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
