const { promises: fs, createReadStream } = require('fs');
const { createHash } = require('crypto');
const { join } = require('path');

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

async function main() {
  const files = await readdir('D:\\Cache\\Yiff', ['videos']);
  const obj = {};

  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    const hash = await getHash(file);

    obj[file] = hash;
    console.log(`[${i + 1}/${files.length}] ${file} -> ${hash}`);
  }

  await fs.writeFile('./src/main/resources/yiff/hashes.json', JSON.stringify(obj, null, 4));
}

main();
