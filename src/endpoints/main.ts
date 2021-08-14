/**
 * Copyright (c) 2020-2021 Noel
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

import type { FastifyReply, FastifyRequest } from 'fastify';
import type { ISizeCalculationResult } from 'image-size/dist/types/interface';
import { HttpClient } from '@augu/orchid';
import GitHubService from '../services/GitHubService';
import ImageService from '../services/ImageService';
import { Inject } from '@augu/lilith';
import imageSize from 'image-size';
import { Get } from '../decorators';

export default class MainRouter {
  @Inject
  private readonly github!: GitHubService;

  @Inject
  private readonly images!: ImageService;

  @Inject
  private readonly http!: HttpClient;

  @Get('/')
  main(_: FastifyRequest, res: FastifyReply) {
    return res.status(200).send({ hello: 'world' });
  }

  @Get('/yiff')
  async yiffJson(_: FastifyRequest, reply: FastifyReply) {
    const url = this.images.random('yiff');
    const res = await this.http.request({ url, method: 'GET' });
    const raw = res.buffer();
    let size!: ISizeCalculationResult;

    try {
      size = imageSize(raw);
    } catch {
      size = { width: 0, height: 0 };
    }

    return reply.type('application/json').status(200).send({
      height: size.height,
      width: size.width,
      url,
    });
  }

  @Get('/yiff/random')
  async yiffBuffer(_: FastifyRequest, reply: FastifyReply) {
    const url = this.images.random('yiff');
    const res = await this.http.request({ url, method: 'GET' });
    const raw = res.buffer();
    let size!: ISizeCalculationResult;

    try {
      size = imageSize(raw);
    } catch {
      size = { width: 0, height: 0 };
    }

    return reply.type(`image/${size.type ?? url.split('.')[1]}`).send(raw);
  }

  @Get('/sponsors')
  sponsorRedirect(_: FastifyRequest, reply: FastifyReply) {
    return reply.status(200).send({
      message: 'Missing :login params.',
    });
  }

  @Get('/sponsors/:login')
  async getSponsor(
    req: FastifyRequest<{
      Params: { login: string };
      Querystring: { private?: boolean; pricing?: 'dollars' | 'cents' };
    }>,
    reply: FastifyReply
  ) {
    if (req.query?.pricing !== undefined && !['dollars', 'cents'].includes(req.query?.pricing))
      return reply.type('application/json').status(400).send({
        message: '?pricing must be the following: `dollars` or `cents`.',
      });

    const data = await this.github.getSponsorships(
      req.params.login,
      req.query?.pricing ?? 'cents',
      req.query?.private !== undefined && req.query.private! === true
    );
    return reply
      .type('application/json; charset=utf-8')
      .status(data.hasOwnProperty('errors') ? 500 : 200)
      .send(data);
  }

  @Get('/kadi')
  async kadiJson(_: FastifyRequest, reply: FastifyReply) {
    const url = this.images.random('kadi');
    const res = await this.http.request({ url, method: 'GET' });
    const raw = res.buffer();
    let size!: ISizeCalculationResult;

    try {
      size = imageSize(raw);
    } catch {
      size = { width: 0, height: 0 };
    }

    return reply.type('application/json').status(200).send({
      height: size.height,
      width: size.width,
      url,
    });
  }

  @Get('/kadi/random')
  async kadiBuffer(_: FastifyRequest, reply: FastifyReply) {
    const url = this.images.random('kadi');
    const res = await this.http.request({ url, method: 'GET' });
    const raw = res.buffer();
    let size!: ISizeCalculationResult;

    try {
      size = imageSize(raw);
    } catch {
      size = { width: 0, height: 0 };
    }

    return reply.type(`image/${size.type ?? url.split('.')[1]}`).send(raw);
  }
}
