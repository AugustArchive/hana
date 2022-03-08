# 花 / Reference
This is a reference sheet on how to use hana with your application, if you want to! **hana** is only consisted with one
HTTP layer, which is the REST layer.

## Versions
**hana** has a lot of growth during 2019, when the project was first created, and the data structure
of any API request has changed from that period forward, hana is split into many API versions that are pretty
much deprecated instead of the default (v3):

- ❤️ **indicates that this API version is no longer supported.**
- 💛 **indicates that this API version is deprecated and will be removed in a major release.**
- 💚 **indicates that this API version is healthy and is receiving updates.**

| API Version  | Supported |
|--------------|-----------|
| `/api/v1`    | ❤️        |
| `/api/v2`    | 💛        |
| `/api/v3`    | 💚        |

### REST Endpoints
```http
https://api.floofy.dev
```

### CDN Endpoints
```http
https://cdn.floofy.dev
```

## Authentication
Some routes require authentication to execute them, you will need to join the [Noelware](https://discord.gg/ATmjFH9kMH) Discord server to register
one to link it your Discord account! You can execute the `/create-key` slash command that is available:

![example: registering an api key](https://i-am.floof.gay/images/8c31df69.png)

You are only required to have an application name to uniquely identify one.
At this moment of time, you are only allowed to create one application.
Once you register an API key, you will get a success message:

![success message](https://i-am.floof.gay/images/a83940e7.png)

You will get a few warnings if `nsfw_enabled` or `im_enabled` was set to true:

![warning messages - nsfw](https://i-am.floof.gay/images/8e2fe6ae.png)

![warning messages - image manipulation](https://i-am.floof.gay/images/8d07cd7c.png)

You will also receive an embed about the application you created:

![app info](https://i-am.floof.gay/images/ff3c2f9d.png)

Now we are all set to execute API requests to endpoints that have the "[Authorization]" suffix.

To use the JWT that **hana** generated, you can set it in the "Authorization" HTTP Header:

```http
Authorization: Bearer <token>
```

## Ratelimiting
**hana** is no different from any other applications, to handle the load of any request from a user,
**hana** has API ratelimiting implemented into the application itself. It will be handled **before** the request
call was ever executed internally. You will receive the following headers in any request you send:

```http
X-RateLimit-Limit: 1200
X-RateLimit-Reset: ...
X-RateLimit-Remaining: ...
X-RateLimit-Reset-Date: ...
```

The following headers detail about the request's ratelimit. This table will describe what
endpoints will be ratelimited:

| Endpoint                            | Limit           | Reasoning                                                                                                                  | IP/API Key?    |
|-------------------------------------|-----------------|----------------------------------------------------------------------------------------------------------------------------|----------------|
| non-im endpoints (without api key)  | 1200 per hour   | This is the default ratelimits for any non-image manipulation endpoints.                                                   | IP             |
| image manipulation                  | 250 per minute  | Since this can take up CPU or memory usage, this is ratelimited very **strictly**.                                         | API Key        |
| non-im endpoints (with api key)     | 2500 per hour   | Since you are probably most careful with any endpoints you execute with an API key, this has been tailored to be very lax. | API Key        |