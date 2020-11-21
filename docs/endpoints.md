# API · Endpoints
List of endpoints available with the request and response types of v2.

## Constants
- API Version: v2
- Base URL:    **api.floofy.dev**
- CDN URL:     **cdn.floofy.dev**

## GET /
Returns the default homepage

### Request
```sh
$ curl -X GET https://api.floofy.dev
```

### Response
```js
{
  "hello": String,
  "version": String,
  "commit": String
}
```

## GET /sponsors/:login
Fetches a list of user sponsors available and who they are sponsoring

### Query Parameters
- `show_private`: Boolean | If we should include private sponsors
- `pricing`: "dollars" or "cents" | If we should show the pricing of each sponsor/sponsoree
- `first`: Int | Fetches the first list of sponsors, default: 5

### Request
```sh
curl -X GET https://api.floofy.dev/sponsors/auguwu?first=5&pricing=dollars&show_private=true
```

### Response
```js
{
    "sponsors": {
        "total_count": Int,
        "data": Array<Sponsorship>
    },
    "user_sponsors": {
        "total_count": Int,
        "data": Array<Sponsorship>
    }
}
```

## GET /kadi
Returns an image of Kadi, my red panda plush. This endpoint returns as a JSON response, not an image.

### Request
```sh
$ curl -X GET https://api.floofy.dev/kadi
```

### Response
```js
{
    "data": String
}
```

## GET /kadi/random
Returns an image of Kadi, my red panda plush. This endpoint returns as an image buffer, not a JSON response.

### Request
```sh
$ curl -X GET https://api.floofy.dev/kadi/random
```

### Response
![cute wah](https://cdn.floofy.dev/kadi/kadi.png)

# Types
List of types from the different request(s) above.

## Sponsorship
```js
{
    "created_at": String,
    "tier": SponsorTier,
    "user": Sponsor
}
```

## SponsorTier
```js
{
    // these 2 are determined by the `pricing` query param
    "monthlyPriceInDollars"?: Int,
    "monthlyPriceInCents"?: Int,
    "name": String
}
```

## Sponsor
```js
{
    "website_url"?: String,
    "avatar_url": String,
    "following": Int,
    "followers": Int,
    "company"?: String,
    "login": String,
    "name"?: String,
    "bio"?: String,
    "url": String
}
```
