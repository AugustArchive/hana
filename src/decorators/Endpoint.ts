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

import type { Request, Response } from 'express';

const ENDPOINT_METADATA_KEY = '$hana.endpoint';
const ROUTE_METADATA_KEY    = '$hana.route';

interface EndpointReference {
  version: 'global' | 1 | 2;
  prefix: string;
}

interface RouteReference {
  methodName: string;
  path: string;
}

export const getEndpointRef   = (target: any): EndpointReference | undefined => Reflect.getMetadata(ENDPOINT_METADATA_KEY, target);
export const getRouteRefs     = (target: any): RouteReference[] => Reflect.getMetadata(ROUTE_METADATA_KEY, target) ?? [];

export const Endpoint = ({ version, prefix }: Pick<EndpointReference, 'version' | 'prefix'>): ClassDecorator =>
  (target) => Reflect.defineMetadata(ENDPOINT_METADATA_KEY, { version, prefix }, target);

export const Route = (path: string): MethodDecorator => {
  return (target: any, prop, descriptor: TypedPropertyDescriptor<any>) => {
    // todo: this
  };
};
