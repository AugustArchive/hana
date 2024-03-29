/*
 * 🥀 hana: API to proxy different APIs like GitHub Sponsors, source code for api.floofy.dev
 * Copyright (c) 2020-2022 Noel <cutie@floofy.dev>
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

package gay.floof.hana.routing.endpoints

import gay.floof.hana.routing.AbstractEndpoint
import gay.floof.hana.routing.endpoints.api.v2.*
import gay.floof.hana.routing.endpoints.api.v3.*
import org.koin.dsl.bind
import org.koin.dsl.module

val routingModule = module {
    // api/v3 (default)
    single { DefaultFetchSponsorsEndpoint(get(), get()) } bind AbstractEndpoint::class
    single { FetchSponsorsV3Endpoint(get(), get()) } bind AbstractEndpoint::class
    single { DefaultSponsorsEndpoint() } bind AbstractEndpoint::class
//    single { KadiV3ImageEndpoint() } bind AbstractEndpoint::class
//    single { WahsV3ImageEndpoint() } bind AbstractEndpoint::class
//    single { YiffV3ImageEndpoint() } bind AbstractEndpoint::class
    single { SponsorsV3Endpoint() } bind AbstractEndpoint::class
    single { KadiImageEndpoint() } bind AbstractEndpoint::class
    single { YiffImageEndpoint() } bind AbstractEndpoint::class
    single { WahsImageEndpoint() } bind AbstractEndpoint::class
//    single { YiffV3Endpoint() } bind AbstractEndpoint::class
    single { ApiV3Endpoint() } bind AbstractEndpoint::class
    single { YiffEndpoint() } bind AbstractEndpoint::class
    single { WahsEndpoint() } bind AbstractEndpoint::class
    single { KadiEndpoint() } bind AbstractEndpoint::class

    // api/v2
    single { FetchSponsorV2Endpoint(get(), get()) } bind AbstractEndpoint::class
    single { KadiImageV2Endpoint(get(), get()) } bind AbstractEndpoint::class
    single { YiffImageV2Endpoint(get(), get()) } bind AbstractEndpoint::class
    single { KadiV2Endpoint(get(), get()) } bind AbstractEndpoint::class
    single { YiffV2Endpoint(get(), get()) } bind AbstractEndpoint::class
    single { SponsorV2Endpoint() } bind AbstractEndpoint::class
    single { ApiV2Endpoint() } bind AbstractEndpoint::class

    // api/v1
    single { ParamApiV1Endpoint() } bind AbstractEndpoint::class
    single { ApiV1Endpoint() } bind AbstractEndpoint::class

    // main endpoints
    single { MetricsEndpoint(get()) } bind AbstractEndpoint::class
    single { HealthEndpoint() } bind AbstractEndpoint::class
    single { MainEndpoint() } bind AbstractEndpoint::class
}
