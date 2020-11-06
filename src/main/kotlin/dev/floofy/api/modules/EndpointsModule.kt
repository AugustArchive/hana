package dev.floofy.api.modules

import dev.floofy.api.endpoints.*
import dev.floofy.api.core.Endpoint
import org.koin.dsl.bind
import org.koin.dsl.module

val endpointsModule = module {
    single { SponsorsWebhookEndpoint() } bind Endpoint::class
    single { GitHubWebhooksEndpoint() } bind Endpoint::class
    single { SentryWebhookEndpoint() } bind Endpoint::class
    single { ListSponsorsEndpoint() } bind Endpoint::class
    single { SponsorsEndpoint() } bind Endpoint::class
    single { WebhooksEndpoint() } bind Endpoint::class
    single { MainEndpoint() } bind Endpoint::class
    single { KadiEndpoint() } bind Endpoint::class
    single { YiffEndpoint() } bind Endpoint::class
}
