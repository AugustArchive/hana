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

import { Component, Inject, PendingInjectDefinition } from '@augu/lilith';
import { HttpServer as Server, middleware, Router } from '@augu/http';
import { RouteDefinition, ROUTE_METAKEY } from '../decorators';
import { readdir, Ctor, firstUpper } from '@augu/utils';
import ThreadPool from '../threading/WorkerPool';
import { Logger } from 'tslog';
import { join } from 'path';
import Config from './Config';
import app from '../container';

@Component({
  priority: 1,
  name: 'http'
})
export default class HttpServer {
  protected _rehydrateYiffCache?: NodeJS.Timer;
  private threadPool!: ThreadPool;

  #server!: Server;

  @Inject
  private logger!: Logger;

  @Inject
  private config!: Config;

  async load() {
    this.threadPool = new ThreadPool(2);
    this.#server = new Server({
      middleware: [middleware.headers()],
      port: this.config.getProperty('PORT')
    });

    this.logger.info('now loading routes...');
    const files = await readdir(join(process.cwd(), 'endpoints'));
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const ctor: Ctor<Router> = await import(file);

      // TODO(auguwu): make this as a function in @augu/lilith
      const injections = Reflect.getMetadata<PendingInjectDefinition[]>('$lilith::api::injections::pending', global) ?? [];
      for (const inject of injections) app.inject(inject);

      const router = new ctor.default!();
      const routes = Reflect.getMetadata<RouteDefinition[]>(ROUTE_METAKEY, router) ?? [];
      this.logger.info(`found ${routes.length} routes to load`);

      for (const route of routes) {
        router[route.method](route.path, async (req, res) => {
          try {
            await route.run.call(router, req, res);
          } catch(ex) {
            this.logger.error(`Unable to run route "${route.method.toUpperCase()} ${route.path}"`, ex);
            return res.status(500).json({
              message: `An unexpected error has occured while running "${route.method.toUpperCase()} ${route.path}". Contact August#5820 in #support under the API category at discord.gg/ATmjFH9kMH if this continues.`
            });
          }
        });
      }

      this.logger.info(`✔ init ${router.prefix} with ${routes.length} route(s)`);
      this.#server.router(router);
    }

    this.logger.info('now rehydrating yiff cache...');
    await this.rehydrateCache();

    this._rehydrateYiffCache = setInterval(async () => {
      this.logger.info('it has been a day, rehydrating...');
      await this.rehydrateCache();
    }, 86400000);

    this.#server.on('listening', (networks) =>
      this.logger.info('API service is now listening on the following URLs:\n', networks.map(network => `• ${firstUpper(network.type)} | ${network.host}`).join('\n'))
    );

    this.#server.on('request', (props) =>
      this.logger.debug(`API: ${props.method} ${props.path} (${props.status}) | ${props.time}ms`)
    );

    this.#server.on('error', error => this.logger.fatal(error));
    return this.#server.start();
  }

  dispose() {
    return this.#server.close();
  }

  private async rehydrateCache() {
    await this.threadPool.run(join(process.cwd(), '..', 'scripts', 'e621.js'), {
      hydrate: true
    }, {
      YIFF_CACHE: this.config.getProperty('YIFF_PATH')
    });
  }
}
