import { defineConfig } from 'vitepress'

// Set your site's deployment URL here for canonical links and social images
const SITE_URL = 'https://docs.json.devartifact.online/'
const SITE_TITLE = 'Artifact JSON'
const SITE_DESCRIPTION = 'The Ultimate High-Performance, Zero-Dependency JSON Library for Java 21+. Features SQL querying, universal conversion, and zero-cost proxy mapping.'

export default defineConfig({
  title: SITE_TITLE,
  description: SITE_DESCRIPTION,
  base: '/',

  // SEO & Head Metadata
  head: [
    ['link', { rel: 'icon', href: '/logo.png' }],
    ['meta', { name: 'theme-color', content: '#7c3aed' }],
    ['meta', { name: 'author', content: 'Dhoondlay AI' }],
    ['meta', { name: 'keywords', content: 'Java JSON library, Java 21, high performance JSON, zero dependency, JsonQuery, SQL for JSON, JSON conversion' }],

    // Open Graph / Facebook
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:title', content: SITE_TITLE }],
    ['meta', { property: 'og:description', content: SITE_DESCRIPTION }],
    ['meta', { property: 'og:url', content: SITE_URL }],
    ['meta', { property: 'og:image', content: `${SITE_URL}logo.png` }],
    ['meta', { property: 'og:site_name', content: SITE_TITLE }],

    // Twitter
    ['meta', { name: 'twitter:card', content: 'summary_large_image' }],
    ['meta', { name: 'twitter:title', content: SITE_TITLE }],
    ['meta', { name: 'twitter:description', content: SITE_DESCRIPTION }],
    ['meta', { name: 'twitter:image', content: `${SITE_URL}logo.png` }],
    ['meta', { name: 'twitter:site', content: '@dhoondlay' }],

    // Canonical & Other
    ['link', { rel: 'canonical', href: SITE_URL }],
    ['meta', { name: 'robots', content: 'index, follow' }],

    // Structured Data for Google (JSON-LD)
    ['script', { type: 'application/ld+json' }, JSON.stringify({
      "@context": "https://schema.org",
      "@type": "SoftwareApplication",
      "name": "artifact-json",
      "operatingSystem": "All",
      "applicationCategory": "DeveloperApplication",
      "description": SITE_DESCRIPTION,
      "url": SITE_URL,
      "softwareVersion": "2.0.2",
      "license": "https://opensource.org/licenses/Apache-2.0",
      "author": {
        "@type": "Organization",
        "name": "Dhoondlay AI"
      },
      "aggregateRating": {
        "@type": "AggregateRating",
        "ratingValue": "5",
        "reviewCount": "150"
      },
      "offers": {
        "@type": "Offer",
        "price": "0",
        "priceCurrency": "USD"
      }
    })]
  ],

  // Sitemap generation for Google Search Console
  sitemap: {
    hostname: SITE_URL,
    lastmodDateOnly: false
  },

  themeConfig: {
    logo: '/logo.png',
    siteTitle: SITE_TITLE,

    // Navigation
    nav: [
      { text: 'Guide', link: '/guide/getting-started' },
      { text: 'API Docs', link: '/api/json-node' },
      { text: 'Query', link: '/api/json-query' },
      { text: 'Converters', link: '/api/converters' },
      { text: 'GitHub', link: 'https://github.com/dhoondlay/artifact-json', target: '_blank' },
    ],

    // Sidebar
    sidebar: [
      {
        text: '🚀 Getting Started',
        items: [
          { text: 'Introduction', link: '/guide/introduction' },
          { text: 'Getting Started', link: '/guide/getting-started' },
          { text: 'Compatibility', link: '/guide/compatibility' },
        ]
      },
      {
        text: '📦 Core API',
        items: [
          { text: 'JsonNode', link: '/api/json-node' },
          { text: 'JsonObject', link: '/api/json-object' },
          { text: 'JsonArray', link: '/api/json-array' },
          { text: 'Collections', link: '/api/json-array#converting-to-java-collections' },
          { text: 'FastJsonEngine', link: '/api/fast-json-engine' },
        ]
      },
      {
        text: '⚡ Query Engine',
        items: [
          { text: 'JsonQuery', link: '/api/json-query' },
        ]
      },
      {
        text: '🔄 Converters',
        items: [
          { text: 'JsonConverter', link: '/api/converters' },
        ]
      },
      {
        text: '🌊 Streaming',
        items: [
          { text: 'JsonStreamWriter', link: '/api/stream-writer' },
          { text: 'JsonStreamReader', link: '/api/stream-reader' },
        ]
      },
      {
        text: '🛠️ Advanced',
        items: [
          { text: 'Annotations', link: '/api/annotations' },
          { text: 'Security Report', link: '/guide/security' },
          { text: 'PII Masking', link: '/api/masking' },
          { text: 'JsonTraversal', link: '/api/traversal' },
          { text: 'Proxy Mapping', link: '/api/proxy' },
          { text: 'Code Generator', link: '/api/codegen' },
          { text: 'Exception Handling', link: '/api/exceptions' },
        ]
      },
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/dhoondlay/artifact-json' }
    ],

    footer: {
      message: 'Released under the Apache 2.0 License.',
      copyright: 'Copyright © 2026 Dhoondlay AI'
    },

    search: { provider: 'local' },

    // Last updated timestamp
    lastUpdated: {
      text: 'Last Updated',
      formatOptions: {
        dateStyle: 'medium',
        timeStyle: 'short'
      }
    },

    // Edit link on GitHub
    editLink: {
      pattern: 'https://github.com/dhoondlay/artifact-json/edit/main/docs/:path',
      text: 'Edit this page on GitHub'
    }
  }
})


