import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)

const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.config.errorHandler = (err, vm, info) => {
  console.error('[Vue Error]', err, info)
  const el = document.getElementById('app')
  if (el && !el.querySelector('.vue-global-error')) {
    const box = document.createElement('div')
    box.className = 'vue-global-error'
    box.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:#fff;color:#c0392b;padding:40px;font-family:monospace;z-index:99999;overflow:auto;'
    box.innerHTML = '<h2 style="color:#c0392b">页面渲染错误</h2><pre style="white-space:pre-wrap">' + (err?.stack || err) + '</pre><p style="color:#888;font-size:12px">' + (info || '') + '</p>'
    el.appendChild(box)
  }
}

app.mount('#app')