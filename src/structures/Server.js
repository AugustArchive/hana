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

const RoutingManager = require('./managers/RoutingManager');
const { Signale } = require('signale');
const { version } = require('../util/Constants');
const express = require('express');
const cors = require('cors');

/**
 * Represents the bare-minimum API server, which handles everything
 * under the hood
 * 
 * - hi yes i am bad at explaining :(
 */
module.exports = class Server {
  /**
   * Creates a new [Server] instance
   * @param {EnvConfig} config The configuration from [dotenv.parse]
   */
  constructor(config) {
    /**
     * The configuration as an object
     * @type {Config}
     */
    this.config = {
      sentrySignature: config.SENTRY_SIGNATURE,
      sentryAccessToken: config.SENTRY_ACCESS_TOKEN,
      environment: config.NODE_ENV,
      accessToken: config.GH_ACCESS_TOKEN,
      port: config.PORT
    };

    /**
     * Handles all of the routing of the backend API
     * @type {RoutingManager}
     */
    this.routes = new RoutingManager(this);
  
    /**
     * Logger instance
     * @type {import('signale').Signale}
     */
    this.logger = new Signale({ scope: 'Server' });

    /**
     * Actual Fastify instance
     * @type {import('express').Express}
     */
    this.app = express();

    this.logger.config({ displayBadge: true, displayTimestamp: true });
  }

  /**
   * Adds the needed middleware for Express
   */
  addMiddleware() {
    // Override the powered by header
    this.app.use((_, res, next) => {
      res.setHeader('X-Powered-By', `auguwu tehc (v${version}, https://github.com/auguwu/API)`);
      next();
    });

    // Add CORS functionality
    this.app.use(cors());
  }

  /**
   * Boots up the server
   */
  async start() {
    this.logger.info('Booting up server...');
    this.addMiddleware();

    await this.routes.load();
    global.config = this.config; // this is bad practice but i can give 2 shits lol

    /**
     * The http service itself (shouldn't be accessable & used)
     * @type {import('http').Server}
     */
    this._service = this.app.listen(this.config.port, () => this.logger.info(`Now listening at http://localhost:${this.config.port}`));
  }

  /**
   * Disposes this instance
   */
  dispose() {
    this._service.close();
    this.logger.warn('Server has been disposed successfully');
  }
};

/**
 * @typedef {object} EnvConfig
 * @prop {string} gh_access_token The access token
 * @prop {'development' | 'production'} node_env The state of the environment
 * @prop {number} port The port of the API
 * 
 * @typedef {object} Config
 * @prop {'development' | 'production'} environment The environment of the API
 * @prop {string} accessToken The access token from GitHub
 * @prop {number} port The port of the API
 */
