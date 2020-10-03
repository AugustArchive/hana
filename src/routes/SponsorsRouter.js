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

const { queries, version } = require('../util/Constants');
const { HttpClient } = require('@augu/orchid');

const e = require('express');
const router = e.Router();

router.get('/', async(req, res) => {
  const http = new HttpClient({
    defaults: {
      headers: {
        'User-Agent': `api.augu.dev (v${version}, https://github.com/auguwu/API)`,
        'Authorization': `Bearer ${config.accessToken}`
      }
    }
  });

  if (!req.query.hasOwnProperty('login')) return res.status(406).json({
    statusCode: 406,
    message: 'Missing "?login" query'
  });

  if (req.query.hasOwnProperty('first')) {
    const first = Number(req.query.first);
    if (isNaN(first) && !Number.isSafeInteger(first)) return res.status(406).json({
      statusCode: 406,
      message: '`first` query param must be a number'
    });
  }

  const query = queries.Sponsors(req.query.login, req.query.hasOwnProperty('first') ? Number(req.query.first) : 5);
  try {
    const resp = await http.request({
      method: 'POST',
      url: 'https://api.github.com/graphql',
      data: {
        query
      }
    });

    const { data } = resp.json();
    const sponsorships = data.user.sponsorshipsAsMaintainer;

    return res.status(200).json({
      statusCode: 200,
      data: sponsorships.nodes.filter(sponsor => sponsor.privacyLevel === 'PUBLIC').map(sponsor => ({
        createdAt: sponsor.createdAt,
        sponsor: {
          profile: sponsor.sponsorEntity.url,
          avatar: sponsor.sponsorEntity.avatarUrl,
          login: sponsor.sponsorEntity.login,
          name: sponsor.sponsorEntity.name,
          bio: sponsor.sponsorEntity.bio === '' ? 'User is a mystery...' : sponsor.sponsorEntity.bio
        },
        tier: sponsor.tier
      }))
    });
  } catch(ex) {
    console.error(ex);
    return res.status(500).json({
      statusCode: 500,
      message: `Unable to fetch data from GitHub (${ex.message})`
    });
  }
});

module.exports = {
  path: '/sponsors',
  core: router
};
