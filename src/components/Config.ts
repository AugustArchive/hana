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

import { Component, Inject } from '@augu/lilith';
import { parse } from '@augu/dotenv';
import { Logger } from 'tslog';
import { join } from 'path';

interface ConfigDetails {
  KADI_IMAGE_PATH: string;
  YIFF_IMAGE_PATH: string;
  PORT: number;
}

export default class Config implements Component {
  public priority: number = 0;
  private config!: ConfigDetails;
  public name: string = 'config';

  @Inject
  private logger!: Logger;

  load() {
    this.logger.info('loaded config');
    this.config = parse<ConfigDetails>({
      populate: false,
      file: join(__dirname, '..', '..', '.env'),
      schema: {
        KADI_IMAGE_PATH: 'string',
        YIFF_IMAGE_PATH: 'string',
        PORT: 'int'
      }
    });
  }

  get<K extends keyof ConfigDetails>(key: K): ConfigDetails[K] {
    return this.config[key]!;
  }
}
