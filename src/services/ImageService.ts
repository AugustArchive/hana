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

import { CreateBucketCommand, ListBucketsCommand, ListObjectsCommand, S3Client } from '@aws-sdk/client-s3';
import { Service, Inject, ComponentOrServiceHooks } from '@augu/lilith';
import type { Provider, Credentials } from '@aws-sdk/types';
import { Logger } from 'tslog';
import Config from '../components/Config';

type Image = 'yiff' | 'kadi' | 'polarbois';
type ImageCache = {
  [P in Image]: string[];
}

@Service({
  priority: 1,
  name: 'images'
})
export default class ImageService implements ComponentOrServiceHooks {
  protected _refreshImageCache?: NodeJS.Timer;
  protected _imagePool!: ImageCache;
  private s3!: S3Client;

  @Inject
  private readonly logger!: Logger;

  @Inject
  private readonly config!: Config;

  async load() {
    this.logger.info('setting up image cache...');

    const config = this.config.getPropertyOrNull('s3');
    if (config === null)
      throw new SyntaxError('strange, there is no... S3 configuration.');

    this.s3 = new S3Client({
      credentialDefaultProvider: () => this.credentialsProvider.bind(this),
      endpoint: config.provider === 'wasabi' ? 'https://s3.wasabisys.com' : undefined,
      region: config.region ?? 'us-east-1'
    });

    this.logger.info(`created s3 client with provider ${config.provider} in region ${config.region ?? 'us-east-1'}.`);

    const result = await this.s3.send(new ListBucketsCommand({}));
    if (result.Buckets === undefined)
      throw new TypeError('result from s3 was corrupted?');

    const bucket = this.config.getProperty('s3.bucket');
    this.logger.debug(`got result back with ${result.Buckets.length} buckets\n`, result.Buckets.map(bucket => `- ${bucket.Name ?? '(unknown)'} @ ${bucket.CreationDate !== undefined ? new Date(bucket.CreationDate).toLocaleString('en-US') : '(unknown)'}`).join('\n'));
    this.logger.info(`using bucket ${bucket}...`);

    if (!result.Buckets.find(b => b.Name === bucket)) {
      this.logger.warn(`missing bucket ${bucket}! creating...`);

      const o = await this.s3.send(new CreateBucketCommand({ Bucket: bucket }));
      this.logger.info(`Made a bucket named "${bucket}" in location ${o.Location ?? '(unknown)'}`);
    }

    const listObj = await this.s3.send(new ListObjectsCommand({ Bucket: bucket }));
    if (!listObj.Contents)
      throw new SyntaxError('Missing contents!');

    this._imagePool = {
      polarbois: (listObj.Contents ?? []).filter(c => c.Key! !== 'polarbois/').filter(c => c.Key!.startsWith('polarbois/')).map(content => `https://cdn.floofy.dev/${content.Key}`),
      yiff: listObj.Contents!.filter(c => c.Key! !== 'yiff/').filter(c => c.Key!.startsWith('yiff/')).map(content => `https://cdn.floofy.dev/${content.Key}`),
      kadi: listObj.Contents!.filter(c => c.Key! !== 'kadi/').filter(c => c.Key!.startsWith('kadi/')).map(content => `https://cdn.floofy.dev/${content.Key}`)
    };

    this._refreshImageCache = setInterval(this._refreshImagePool.bind(this), 86400000);
    this.logger.info('successfully implemented image cache.');
  }

  dispose() {
    clearInterval(this._refreshImageCache!);
    this.s3.destroy();
  }

  private async _refreshImagePool() {
    this.logger.info('refreshing image cache...');
    const listObj = await this.s3.send(new ListObjectsCommand({ Bucket: this.config.getProperty('s3.bucket') }));
    if (!listObj.Contents) {
      this.logger.warn('no contents available :(');
      return;
    }

    this._imagePool = {
      polarbois: (listObj.Contents ?? []).filter(c => c.Key! !== 'polarbois/').filter(c => c.Key!.startsWith('polarbois/')).map(content => `https://cdn.floofy.dev/${content.Key}`),
      yiff: listObj.Contents!.filter(c => c.Key! !== 'yiff/').filter(c => c.Key!.startsWith('yiff/')).map(content => `https://cdn.floofy.dev/${content.Key}`),
      kadi: listObj.Contents!.filter(c => c.Key! !== 'kadi/').filter(c => c.Key!.startsWith('kadi/')).map(content => `https://cdn.floofy.dev/${content.Key}`)
    };
  }

  random(kind: Image) {
    const urls = this._imagePool[kind];
    return urls[Math.floor(Math.random() * urls.length)];
  }

  get credentialsProvider(): Provider<Credentials> {
    return () => new Promise<Credentials>(resolve => resolve({
      secretAccessKey: this.config.getProperty('s3.secretKey'),
      accessKeyId: this.config.getProperty('s3.accessKey')
    }));
  }
}
