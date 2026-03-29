// .vitepress/theme/index.ts
import DefaultTheme from 'vitepress/theme'
import './custom.css'

export default {
  ...DefaultTheme,
  // Enhance functionality if needed in the future
  enhanceApp({ app, router, siteData }) {
    // ...
  }
}
