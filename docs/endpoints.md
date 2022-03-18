# 花 - Endpoints
This is a list of curated endpoints that you can use today! This doesn't discuss the topics of authentication,
ratelimiting, and such, please read the [reference](/reference) guide!

## GET /
> *Returns the main response, which isn't nothing fancy.*

### JSON Response
> 200: OK

```json
{
  "success": true,
  "data": {
    "message": "Hello, world!",
    "docsUri": "https://api.floofy.dev/docs/"
  }
}
```

## GET /v{version}
> *Returns the main response for any versioned endpoint*

### JSON Response [v2]
> 200: OK

```json
{
  "message": "Hello, world!",
  "docsUri": "https://api.floofy.dev/docs/"
}
```

### JSON Response [v3]
> 200: OK

```json
{
  "success": true,
  "data": {
    "message": "Hello, world!",
    "docsUri": "https://api.floofy.dev/docs/"
  }
}
```

## GET /metrics
> *Returns the metrics scraped from the main service*

### Response
Since the metrics service is enabled only on the main instance (used for monitoring), it will return a 404 Not Found status
code or a 200 OK status code with all the metrics scraped.

## GET /health
> *Returns a simple response of "OK" if the server is healthy*

### Response
> 200 OK:

```
OK
```

## GET /api/sponsors/{login}
> *Returns a list of sponsors for the given "login" that you provide.*

### Request Parameters
| Name        | Type                          | Required?   | Description                                                   |
|-------------|-------------------------------|-------------|---------------------------------------------------------------|
| `{login}`   | **String**                    | True        | The user on GitHub to retrieve sponsors.                      |
| `?pricing=` | **Enum** ("dollars", "cents") | False       | Returns what pricing we should use when the data is fetched.  |
| `?private=` | **Boolean**                   | False       | If private sponsors should be included.                       |

### Response
> 200 OK:

```js
{
  "success": Boolean,
  "data": GitHubSponsorMetadata[]
}
```
