# API · Ratelimiting
Showcase on how ratelimiting works in the API.

## Table
|Scope|Endpoint|Time|
|-----|--------|----|
|`global`|`/kadi/...`|500 requests per minute|
|`v1`|`/sponsors`|1000 requests per minute|
|`v2`|`/yiff/...`|500 requests per minute|
|`v2`|`/sponsors`|300 requests per minute|

## Lifting Ratelimits
Due to some restrictions you might have, you might want an API key to lift the ratelimits with even relaxed times. If you do interact
a problem with it, contact August on [Telegram](https://t.me/auguwu) or on Discord (**August#5820**), and we can set up an API key on
the restrictions availed.
