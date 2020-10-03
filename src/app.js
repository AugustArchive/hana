/**
 * Copyright (c) 2020 August
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

const { existsSync } = require('fs');
const { isNode10 } = require('./util');
const { Signale } = require('signale');
const { parse } = require('@augu/dotenv');
const { join } = require('path');
const Server = require('./structures/Server');

const logger = new Signale({ scope: 'Master' });
logger.config({ displayTimestamp: true, displayBadge: true });
if (!isNode10()) {
  logger.fatal(`Sorry but version ${process.version} is not an avaliable version to run the API. Please update your Node.js installation to v10 or higher.`);
  process.exit(1);
}

if (!existsSync(join(__dirname, '..', '.env'))) {
  logger.fatal('Missing .env directory in the root directory.');
  process.exit(1);
}

const config = parse({
  populate: false,
  file: join(__dirname, '..', '.env'),
  schema: {
    SENTRY_ACCESS_TOKEN: {
      type: 'string',
      default: undefined
    },
    SENTRY_SIGNATURE: {
      type: 'string',
      default: undefined
    },
    GH_ACCESS_TOKEN: {
      type: 'string',
      default: undefined
    },
    NODE_ENV: {
      type: 'string',
      oneOf: ['development', 'production'],
      default: 'development'
    },
    PORT: {
      type: 'int',
      default: 3939
    }
  }
});

const server = new Server(config);
server.start()
  .then(() => logger.info('API has successfully booted up!'))
  .catch(error => logger.error('Unable to load up API', error));

process.on('uncaughtException', (error) => logger.error('Received an uncaught exception:', error));
process.on('unhandledRejection', (error) => logger.error('Received an unhandled Promise rejection:', error));
process.on('SIGINT', () => {
  logger.warn('Disposing API...');
  server.dispose();

  process.exit(0);
});
