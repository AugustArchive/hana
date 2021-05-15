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

import { Component, Inject, ComponentOrServiceHooks } from '@augu/lilith';
import { readFile, writeFile } from 'fs/promises';
import { randomBytes } from 'crypto';
import { load, dump } from 'js-yaml';
import { existsSync } from 'fs';
import { Logger } from 'tslog';
import { join } from 'path';

type ConfigKeyedAsDotNotation = {
  [P in keyof Configuration]: Configuration[P];
} & {
  [P in keyof S3Config as `s3.${P}`]: S3Config[P];
};

interface Configuration {
  githubSecret: string;
  secret: string;
  host?: string;
  s3: S3Config;
}

interface S3Config {
  secretKey: string;
  accessKey: string;
  provider: 'amazon' | 'wasabi';
  region: string;
  bucket: string;
}

const NotFoundSymbol = Symbol.for('$hana::config::value::404');

@Component({
  priority: 0,
  name: 'config'
})
export default class Config implements ComponentOrServiceHooks {
  @Inject
  private readonly logger!: Logger;
  #config!: Configuration;

  async load() {
    const path = join(process.cwd(), '..', 'config.yml');
    if (!existsSync(path)) {
      const config: Configuration = {
        githubSecret: '',
        secret: randomBytes(16).toString('hex'),
        s3: {
          secretKey: '',
          accessKey: '',
          provider: 'amazon',
          region: 'us-east-1',
          bucket: 'hana'
        }
      };

      const contents = dump(config, {
        noArrayIndent: false,
        skipInvalid: true,
        sortKeys: true,
        quotingType: "'" // eslint-disable-line quotes
      });

      await writeFile(path, contents);
      throw new SyntaxError(`Well well, that seems... obviously weird... No config path was found in "${path}"? Why why do you got to make me do this? Well, I created one for you anyway, please edit it to your liking...`);
    }

    this.logger.info('attempting to load config...');
    const contents = await readFile(path, { encoding: 'utf-8' });
    const config = load(contents) as unknown as Configuration;

    this.logger.info(`found config at path "${path}"`);
    this.#config = config;
  }

  getPropertyOrNull<K extends keyof ConfigKeyedAsDotNotation>(key: K): ConfigKeyedAsDotNotation[K] | null {
    const nodes = key.split('.');
    let value: any = this.#config;

    for (let i = 0; i < nodes.length; i++) {
      try {
        value = value[nodes[i]];
      } catch {
        value = NotFoundSymbol;
      }
    }

    return value === NotFoundSymbol ? null : value;
  }

  getProperty<K extends keyof ConfigKeyedAsDotNotation>(key: K) {
    const value = this.getPropertyOrNull(key);
    if (value === null)
      throw new TypeError(`Node \`${key}\` was not found in config.`);

    return value;
  }
}
