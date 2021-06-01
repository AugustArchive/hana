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
import fastify, { FastifyReply, FastifyRequest } from 'fastify';
import { MetadataKeys, RouteDefinition } from '../types';
import { Logger } from 'tslog';
import container from '../container';
import rateLimit from 'fastify-rate-limit';
import { join } from 'path';
import Config from './Config';
import { STATUS_CODES } from 'http';
import { calculateHRTime } from '@augu/utils';

@Component({
  priority: 0,
  children: join(process.cwd(), 'endpoints'),
  name: 'http:server'
})
export default class HttpServer implements ComponentOrServiceHooks<any> {
  @Inject
  private readonly logger!: Logger;

  @Inject
  private readonly config!: Config;

  #lastPing!: [number, number];
  #pings: number[] = [];
  #app!: ReturnType<typeof fastify>;

  async load() {
    this.logger.info('attempting to connect to the world...');

    const fastifyLogger = this.logger.getChildLogger({ name: 'hana: fastify' });
    this.#app = fastify();
    this
      .#app
      .register(require('fastify-cors'))
      .register(require('fastify-no-icon'))
      .register(rateLimit, {
        timeWindow: '10s',
        max: 1000
      })
      .setErrorHandler((error, _, reply) => {
        fastifyLogger.fatal('unable to fulfill request', error);

        const send: Record<string, any> = {
          message: 'Unable to fulfill request',
          error: `[${error.name}:${error.code}]: ${error.message}`
        };

        if (error.validation !== undefined)
          send.validate = error.validation.map(v => ({
            key: v.keyword,
            message: v.params
          }));

        return reply.status(500).send(send);
      })
      .setNotFoundHandler((req, reply) => {
        fastifyLogger.warn(`Path "${req.method.toUpperCase()} ${req.url}" was not found.`);
        return reply.status(404).send({
          message: `Route "${req.method.toUpperCase()} ${req.url}" was not found.`
        });
      })
      .addHook('onRequest', (_, res, done) => {
        res.headers({
          'Cache-Control': 'public, max-age=7776000',
          'X-Powered-By': 'A cute furry doing cute things :3 (https://github.com/auguwu/hana)'
        });

        this.#lastPing = process.hrtime();
        done();
      })
      .addHook('onResponse', (req, res, done) => {
        const duration = calculateHRTime(this.#lastPing);
        this.#pings.push(duration);

        const avg = this.#pings.reduce((acc, curr) => acc + curr, 0) / this.#pings.length;
        fastifyLogger.info(`\n[${req.ip === '::1' ? 'localhost' : req.ip}] ${res.statusCode} (${STATUS_CODES[res.statusCode]}): ${req.method.toUpperCase()} ${req.url} (~${duration.toFixed(2)}ms; avg: ~${avg.toFixed(2)}ms)`);
        done();
      });

    const host = this.config.getPropertyOrNull('host');
    return this.#app.listen({
      port: 4010,
      host: host === null || host === undefined ? undefined : host
    }, (err, address) => {
      if (err) {
        this.logger.fatal('unable to listen to :4010\n', err);
        process.exit(1);
      }

      this.logger.info(`now listening at ${address}`);
    });
  }

  onChildLoad(endpoint: any) {
    console.log(endpoint);

    const routes = Reflect.getMetadata<RouteDefinition[]>(MetadataKeys.APIRoute, endpoint);
    if (routes.length === 0) {
      this.logger.warn(`endpoint class ${endpoint.constructor.name} has no routes attached.`);
      return;
    }

    const v2PrefixRoutes = routes.map(route => ({
      method: route.method,
      path: `/v2${route.path}`,
      run: route.run
    }));

    for (let i = 0; i < routes.length; i++) {
      const route = routes[i];
      this.logger.info(`init: "${route.method.toUpperCase()} ${route.path}"`);

      this.#app[route.method](route.path, async (req: FastifyRequest, reply: FastifyReply) => {
        try {
          await route.run.call(endpoint, req, reply);
        } catch(ex) {
          this.logger.fatal(`unable to run "${route.method.toUpperCase()} ${route.path}"`, ex);
          reply.status(500).send({
            message: 'Unable to run route',
            error: `[${ex.name}] ${ex.message}`
          });
        }
      });
    }

    for (let i = 0; i < v2PrefixRoutes.length; i++) {
      const route = v2PrefixRoutes[i];
      this.logger.info(`init: "${route.method.toUpperCase()} ${route.path}"`);

      this.#app[route.method](route.path, async (req: FastifyRequest, reply: FastifyReply) => {
        try {
          await route.run.call(endpoint, req, reply);
        } catch(ex) {
          this.logger.fatal(`unable to run "${route.method.toUpperCase()} ${route.path}"`, ex);
          reply.status(500).send({
            message: 'Unable to run route',
            error: `[${ex.name}] ${ex.message}`
          });
        }
      });
    }
  }
}
