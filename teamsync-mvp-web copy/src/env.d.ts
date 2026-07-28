/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'echarts-wordcloud' {
  const plugin: any
  export default plugin
}

declare module 'vuedraggable' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<any, any, any>
  export default component
}
