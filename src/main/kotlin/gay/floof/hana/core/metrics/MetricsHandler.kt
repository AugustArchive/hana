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

package gay.floof.hana.core.metrics

import gay.floof.hana.data.HanaConfig
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import io.prometheus.client.hotspot.DefaultExports

class MetricsHandler(config: HanaConfig) {
    val requestLatency: Histogram?
    val requestsCount: Counter?
    val registry: CollectorRegistry?

    val enabled = config.metrics

    init {
        if (enabled) {
            registry = CollectorRegistry()

            DefaultExports.register(registry)
            requestLatency = Histogram.build()
                .name("hana_request_latency")
                .help("Returns the average latency on all API requests.")
                .register(registry)

            requestsCount = Counter.build()
                .name("hana_request_count")
                .help("Returns how many requests by endpoint + method have been executed.")
                .labelNames("endpoint", "method")
                .register(registry)
        } else {
            requestLatency = null
            requestsCount = null
            registry = null
        }
    }
}
