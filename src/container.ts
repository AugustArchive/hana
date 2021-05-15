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

import { Container } from '@augu/lilith';
import { join } from 'path';
import Logger from './singletons/logger';
import Http from './singletons/http';

const logger = Logger.getChildLogger({
  name: 'hana: lilith'
});

const container = new Container({
  componentsDir: join(__dirname, 'components'),
  servicesDir: join(__dirname, 'services'),
  singletons: [Logger, Http]
});

container.on('onBeforeInit', cls => logger.info(`Initializing ${cls.type} ${cls.name}`));
container.on('onAfterInit', cls  => logger.info(`✔ Initialized ${cls.type} ${cls.name}`));
container.on('initError', (cls, error) => logger.error(`Unable to initalize ${cls.type} ${cls.name}`, error));

export default container;
