/**
 * Copyright (c) 2020-2021 August
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

/* eslint-disable camelcase */

import { Service, Inject } from '@augu/lilith';
import { HttpClient } from '@augu/orchid';
import Config from '../components/Config';

const EmojiStatusPrefixRegex = /<div><g-emoji (\w+.*)>/g;
const EmojiStatusSuffixRegex = /<\/g-emoji><\/div>/g;

const graphqlQuery = (login: string) => `
  query { 
    user(login: "${login}") {
      # funni number yes
      sponsorshipsAsMaintainer(last: 69) {
        totalCount
        nodes {
          createdAt
          privacyLevel
          sponsorEntity {
            ... on Organization {
              avatarUrl(size: 1024)
              
              websiteUrl
              description
              hasSponsorsListing
              isVerified
              login
              name
            }
            
            ... on User {
              followers(last: 69) {
                totalCount
              }
              
              following(last: 69) {
                totalCount
              }
              
              status {
                emojiHTML
                message
                expiresAt
              }
              
              websiteUrl
              twitterUsername
              hasSponsorsListing
              avatarUrl(size: 1024)
              company
              login
              name
              bio
            }
          }
          tier {
            monthlyPriceInDollars
            isCustomAmount
            createdAt
            name
          }
          tierSelectedAt
        }
      }

      sponsorshipsAsSponsor(last: 69) {
        totalCount
        nodes {
          createdAt
          privacyLevel
          sponsorable {
            ... on Organization {
              avatarUrl(size: 1024)
              
              websiteUrl
              description
              hasSponsorsListing
              isVerified
              login
              name
            }
            
            ... on User {
              followers(last: 69) {
                totalCount
              }
              
              following(last: 69) {
                totalCount
              }
              
              status {
                emojiHTML
                message
                expiresAt
              }
              
              websiteUrl
              twitterUsername
              hasSponsorsListing
              avatarUrl(size: 1024)
              company
              login
              name
              bio
            }
          }
          tier {
            monthlyPriceInDollars
            isCustomAmount
            createdAt
            name
          }
          tierSelectedAt
        }
      }
    }
  }
`;

type ShowPricingLike = 'cents' | 'dollars';

interface HanaSponsorshipResult {
  sponsoring: ISerializedSponsors;
  sponsors: ISerializedSponsors;
}

interface ISerializedSponsors {
  total_count: number;
  data: ISerializedSponsorData[];
}

interface ISerializedSponsorData {
  joined_at: Date;
  tier: ISerializedSponsorTier;
  tier_selected_at: string | null;

  followers: number;
  following: number;
  status: ISerializedUserStatus | null;
  website_url: string | null;
  twitter_handle: string | null;
  has_sponsors_listing: boolean;
  avatar_url: string | null;
  company: string | null;
  login: string | null;
  name: string | null;
  bio: string | null;
}

interface ISerializedUserStatus {
  expires_at: Date | null;
  message: string | null;
  emoji: string;
}

interface ISerializedSponsorTier {
  custom_amount: boolean;
  created_at: Date;
  price: number;
  name: string;
}

interface GitHubSponsorshipResult {
  data: {
    user: {
      sponsorshipsAsMaintainer: {
        totalCount: number;
        nodes: GitHubSponsorEntityData[];
      }
      sponsorshipsAsSponsor: {
        totalCount: number;
        nodes: GitHubSponsorableData[];
      }
    }
  }
}

interface GitHubSponsorEntityData {
  createdAt: string;
  privacyLevel: 'PUBLIC' | 'PRIVATE';
  tier: SponsorTier;
  tierSelectedAt: string | null;
  sponsorEntity: {
    followers: { totalCount: number };
    following: { totalCount: number };
    status: GitHubUserStatus | null;
    websiteUrl: string | null;
    twitterUsername: string | null;
    hasSponsorsListing: boolean;
    avatarUrl: string | null;
    company: string | null;
    login: string;
    name: string | null;
    bio: string | null;
  }
}

interface GitHubSponsorableData {
  createdAt: string;
  privacyLevel: 'PUBLIC' | 'PRIVATE';
  tier: SponsorTier;
  tierSelectedAt: string | null;
  sponsorable: {
    followers: { totalCount: number };
    following: { totalCount: number };
    status: GitHubUserStatus | null;
    websiteUrl: string | null;
    twitterUsername: string | null;
    hasSponsorsListing: boolean;
    avatarUrl: string | null;
    company: string | null;
    login: string;
    name: string | null;
    bio: string | null;
  }
}

interface GitHubUserStatus {
  emojiHTML: string;
  message: string;
  expiresAt: string | null;
}

interface SponsorTier {
  monthlyPriceInDollars?: number;
  monthlyPriceInCents?: number;
  isCustomAmount: boolean;
  createdAt: string;
  name: string;
}

type GitHubGraphQLResult<Struct> = GraphQLErrorResult | Struct;

interface GraphQLErrorResult {
  errors?: IGraphQLErrors[];
}

interface IGraphQLErrors {
  path: string[];
  extensions: {
    code: string;
    typeName: string;
    fieldName: string;
  };

  message: string;
  locations: IGraphQLineColumn[];
}

interface IGraphQLineColumn {
  column: number;
  line: number;
}

interface GitHubAPIErrorResult {
  message: string;
  documentation_url?: string;
}

@Service({
  priority: 0,
  name: 'github'
})
export default class GitHubService {
  @Inject
  private readonly config!: Config;

  @Inject
  private readonly http!: HttpClient;

  async getSponsorships(
    login: string,
    amount: ShowPricingLike = 'cents',
    includePrivate: boolean = false
  ): Promise<HanaSponsorshipResult | GraphQLErrorResult | GitHubAPIErrorResult> {
    const token = this.config.getProperty('githubSecret');
    const res = await this.http.request({
      url: 'https://api.github.com/graphql',
      method: 'POST',
      data: {
        query: graphqlQuery(login)
      },
      headers: {
        Authorization: `Bearer ${token}`
      }
    });

    // @ts-ignore
    const data = res.json<GitHubGraphQLResult<GitHubSponsorshipResult>>();

    if (data.hasOwnProperty('errors'))
      return data as GraphQLErrorResult;

    if (data.hasOwnProperty('message'))
      return data as GitHubAPIErrorResult;

    return ({
      sponsoring: {
        total_count: (data as GitHubSponsorshipResult).data.user.sponsorshipsAsSponsor.totalCount,
        data: (data as GitHubSponsorshipResult)
          .data
          .user
          .sponsorshipsAsSponsor
          .nodes
          //.filter(node => includePrivate ? false : node.privacyLevel === 'PRIVATE')
          .map<ISerializedSponsorData>(node => ({
            joined_at: new Date(node.createdAt),
            tier: {
              custom_amount: node.tier.isCustomAmount,
              created_at: new Date(node.tier.createdAt),
              price: amount === 'dollars' ? node.tier.monthlyPriceInDollars! : node.tier.monthlyPriceInCents!,
              name: node.tier.name
            },
            tier_selected_at: node.tierSelectedAt,

            followers: node.sponsorable.followers.totalCount,
            following: node.sponsorable.following.totalCount,
            status: node.sponsorable.status !== null ? {
              emoji: node.sponsorable.status.emojiHTML
                .replace(EmojiStatusPrefixRegex, '')
                .replace(EmojiStatusSuffixRegex, ''),

              message: node.sponsorable.status.message,
              expires_at: node.sponsorable.status.expiresAt !== null ? new Date(node.sponsorable.status.expiresAt) : null
            } : null,
            website_url: node.sponsorable.websiteUrl,
            twitter_handle: node.sponsorable.twitterUsername,
            has_sponsors_listing: node.sponsorable.hasSponsorsListing,
            avatar_url: node.sponsorable.avatarUrl,
            company: node.sponsorable.company,
            login: node.sponsorable.login,
            name: node.sponsorable.name,
            bio: node.sponsorable.bio
          }))
      },

      sponsors: {
        total_count: (data as GitHubSponsorshipResult).data.user.sponsorshipsAsMaintainer.totalCount,
        data: (data as GitHubSponsorshipResult)
          .data
          .user
          .sponsorshipsAsMaintainer
          .nodes
          //.filter(node => includePrivate ? false : node.privacyLevel === 'PRIVATE')
          .map<ISerializedSponsorData>(node => ({
            joined_at: new Date(node.createdAt),
            tier: {
              custom_amount: node.tier.isCustomAmount,
              created_at: new Date(node.tier.createdAt),
              price: amount === 'dollars' ? node.tier.monthlyPriceInDollars! : node.tier.monthlyPriceInCents!,
              name: node.tier.name
            },
            tier_selected_at: node.tierSelectedAt,

            followers: node.sponsorEntity.followers.totalCount,
            following: node.sponsorEntity.following.totalCount,
            status: node.sponsorEntity.status !== null ? {
              emoji: node.sponsorEntity.status.emojiHTML
                .replace(EmojiStatusPrefixRegex, '')
                .replace(EmojiStatusSuffixRegex, ''),

              message: node.sponsorEntity.status.message,
              expires_at: node.sponsorEntity.status.expiresAt !== null ? new Date(node.sponsorEntity.status.expiresAt) : null
            } : null,
            website_url: node.sponsorEntity.websiteUrl,
            twitter_handle: node.sponsorEntity.twitterUsername,
            has_sponsors_listing: node.sponsorEntity.hasSponsorsListing,
            avatar_url: node.sponsorEntity.avatarUrl,
            company: node.sponsorEntity.company,
            login: node.sponsorEntity.login,
            name: node.sponsorEntity.name,
            bio: node.sponsorEntity.bio
          }))
      }
    } as HanaSponsorshipResult);
  }
}
