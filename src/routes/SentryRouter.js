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

const { Signale } = require('signale');
const crypto = require('crypto');
const e = require('express');

const logger = new Signale({ scope: 'Webhooks' });
const router = e.Router();
logger.config({ displayBadge: true, displayTimestamp: true });

const verify = (req) => {
  const hmac = crypto.createHmac('sha256', config.sentrySignature);
  hmac.update(JSON.stringify(req.body), 'utf8');

  const digest = hmac.digest('hex');
  return digest === req.headers['Sentry-Hook-Signature'];
};

router.get('/', (_, res) => res.status(200).json({ apple: 'dot com' }));
router.post('/', (req, res) => {
  console.log('a');
  if (!config.sentryAccessToken || !config.sentrySignature) return res.status(503).json({ message: 'Sentry webhooks aren\'t enabled.' });
  //if (!verify(req)) return res.status(204).send();

  logger.debug(req.headers);
  logger.info(`Received event "${req.headers['Sentry-Hook-Resource']}"`);
});

module.exports = { path: '/sentry', core: router };

// time to test in prod
/*
{
  "Content-Type": "application/json",
  "Request-ID": "<request_uuid>",
  "Sentry-Hook-Resource": "<resource>",
  "Sentry-Hook-Timestamp": "<timestamp>",
  "Sentry-Hook-Signature": "<generated_signature>"
}

{
    "action": "created",
    "actor": {
        "id": 1,
        "name": "Meredith Heller",
        "type": "user",
    },
    "data": {
        "installation": {
            "status": "pending",
            "organization": {
                "slug": "test-org"
            },
            "app": {
                "uuid": "2ebf071f-28df-4989-aca9-c37c763b278f",
                "slug": "webhooks-galore"
            },
            "code": "f3c71b491e3949b6b033ae45312a4fcb",
            "uuid": "a8e5d37a-696c-4c54-adb5-b3f28d64c7de"
        }
    },
    "installation": {
        "uuid": "a8e5d37a-696c-4c54-adb5-b3f28d64c7de"
    }
}
*/
