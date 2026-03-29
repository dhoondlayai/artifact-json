import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'artifact-json',
  description: 'High-Performance Zero-Dependency JSON Library for Java 21+',
  base: '/',
  head: [
    ['link', { rel: 'icon', href: '/favicon.ico' }],
    ['meta', { name: 'theme-color', content: '#7c3aed' }],
  ],
  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'artifact-json',
    nav: [
      { text: 'Guide',     link: '/guide/getting-started' },
      { text: 'API Docs',  link: '/api/json-node' },
      { text: 'Query',     link: '/api/json-query' },
      { text: 'Converters',link: '/api/converters' },
      { text: 'GitHub',    link: 'https://github.com/dhoondlay/artifact-json', target: '_blank' },
    ],
    sidebar: [
      {
        text: '🚀 Getting Started',
        items: [
          { text: 'Introduction',    link: '/guide/introduction' },
          { text: 'Getting Started', link: '/guide/getting-started' },
          { text: 'vs Jackson',      link: '/guide/vs-jackson' },
        ]
      },
      {
        text: '📦 Core API',
        items: [
          { text: 'JsonNode',       link: '/api/json-node' },
          { text: 'JsonObject',     link: '/api/json-object' },
          { text: 'JsonArray',      link: '/api/json-array' },
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
          { text: 'JsonTraversal',    link: '/api/traversal' },
          { text: 'Annotations',      link: '/api/annotations' },
          { text: 'Proxy Mapping',    link: '/api/proxy' },
          { text: 'PII Masking',      link: '/api/masking' },
          { text: 'Code Generator',   link: '/api/codegen' },
          { text: 'Exception Handling',link: '/api/exceptions' },
        ]
      },
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/dhoondlay/artifact-json' }
    ],
    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © 2024 dhoondlay.io'
    },
    search: { provider: 'local' }
  }
})
