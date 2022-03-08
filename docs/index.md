# **花** - "hana"
**hana** is an API service made for [Noel](https://floofy.dev)'s projects and proxy services. It contains the following:

- Image Endpoints (red pandas, polar bears, and much more!)
- Image Manipulation (making an image round, cropping an image, and more!)
- Proxy Services
  - **GitHub Sponsors** - Grab any user's sponsors from a simple API endpoint without grabbing it from GitHub's GraphQL API!

## Changelog
### 1.x
This is the initial release of **api.floofy.dev**, originally under the domain **api.augu.dev**! This was written
in pure JavaScript to include a Sentry webhook (to create issues then posts it in a channel on Discord). **hana** (originally **api.augu.dev**),
was only made to be a GitHub sponsors proxy to show a list of my sponsors!

### 2.x
**hana** v2 was the "brand new" improved version of **hana**, still under **api.augu.dev** but swiftly moved to Kotlin with the
Vert.x framework, and introduced new images endpoints, that's pretty much it.

Currently, **v3** is the only supported API version, but **v2** will deprecate and will be retired in December 12th, 2022.

### 3.x
**hana** v3 was a very new improvement towards the **hana** core, only being a proxy service for GitHub sponsors and image endpoints.
But, the image endpoints were no longer handled by the disk, it was handled by Amazon **S3**!

### 4.x
This is the current version that you are seeing (as of **03/03/22**) and brings image manipulation endpoints, this API page
you see, and API keys. You can register an API key in the [Noelware](https://noelware.org/discord) Discord server under the **#commands**
channel using the `/api-key create` command!

Since some image endpoints are **NSFW**, by default, the NSFW endpoints are LOCKED AND MUST BE EXPLICITLY ENABLED BY THE USER REGISTERING!

I am not held responsible if a minor has access to this API since it has been discussed that it is 18+ only since I do not want to bring
in a system to verify users' API keys by their IDs, passports, etc since that is ***very insecure*** and dangerous to give out in the first place.
