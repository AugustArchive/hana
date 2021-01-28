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
package dev.floofy.api_old.modules

import dev.floofy.api_old.core.Endpoint
import dev.floofy.api_old.endpoints.KadiEndpoint
import dev.floofy.api_old.endpoints.NotFoundEndpoint
import dev.floofy.api_old.endpoints.RandomKadiEndpoint
import dev.floofy.api_old.endpoints.v1.MainEndpoint as MainV1Endpoint
import dev.floofy.api_old.endpoints.v1.SponsorsEndpoint as SponsorsV1Endpoint
import dev.floofy.api_old.endpoints.v2.*
import org.koin.dsl.bind
import org.koin.dsl.module

val endpointsModule = module {
    // v2 endpoints
    single { ListSponsorsEndpoint(get(), get()) } bind Endpoint::class
    single { YiffImageStatsEndpoint(get()) } bind Endpoint::class
    single { RandomYiffEndpoint(get()) } bind Endpoint::class
    single { YiffStatsEndpoint(get()) } bind Endpoint::class
    single { YiffEndpoint(get()) } bind Endpoint::class
    single { SponsorsEndpoint() } bind Endpoint::class
    single { MainEndpoint() } bind Endpoint::class

    // v1 endpoints
    single { SponsorsV1Endpoint(get(), get()) } bind Endpoint::class
    single { MainV1Endpoint() } bind Endpoint::class

    // Other that don't require an version number
    // i.e: API (data) didn't change
    single { RandomKadiEndpoint() } bind Endpoint::class
    single { NotFoundEndpoint() } bind Endpoint::class
    single { KadiEndpoint() } bind Endpoint::class
}
