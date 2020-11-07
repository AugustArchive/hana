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
package dev.floofy.api.modules

import dev.floofy.api.endpoints.v1.SponsorsEndpoint as SponsorsV1Endpoint
import dev.floofy.api.endpoints.v1.MainEndpoint as MainV1Endpoint
import dev.floofy.api.endpoints.v2.*
import dev.floofy.api.endpoints.KadiEndpoint
import dev.floofy.api.core.Endpoint
import org.koin.dsl.bind
import org.koin.dsl.module

val endpointsModule = module {
    // v2 endpoints
    //single { SponsorsWebhookEndpoint() } bind Endpoint::class
    //single { GitHubWebhooksEndpoint() } bind Endpoint::class
    //single { SentryWebhookEndpoint() } bind Endpoint::class
    //single { ListSponsorsEndpoint() } bind Endpoint::class
    //single { SponsorsEndpoint() } bind Endpoint::class
    //single { WebhooksEndpoint() } bind Endpoint::class
    //single { MainEndpoint() } bind Endpoint::class
    //single { YiffEndpoint() } bind Endpoint::class

    // v1 endpoints
    //single { SponsorsV1Endpoint() } bind Endpoint::class
    //single { MainV1Endpoint() } bind Endpoint::class

    // Other that don't require an version number
    // i.e: API (data) didn't change
    single { KadiEndpoint() } bind Endpoint::class
}
