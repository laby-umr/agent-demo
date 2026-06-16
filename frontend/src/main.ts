import { createApp } from 'vue'
import './style.css'
import App from './App.vue'

const app = createApp(App)
app.config.errorHandler = (err, _instance, info) => {
  console.error('[vue-error]', err, info)
  const root = document.getElementById('app')
  if (root && root.innerHTML.trim() === '') {
    root.innerHTML = `<div style="padding:24px;font-family:sans-serif;color:#b91c1c">
      <h2>前端加载失败</h2>
      <p>${err instanceof Error ? err.message : String(err)}</p>
      <p style="color:#666;font-size:14px">请打开浏览器控制台(F12)查看详情，或重启 npm run dev</p>
    </div>`
  }
}
app.mount('#app')
