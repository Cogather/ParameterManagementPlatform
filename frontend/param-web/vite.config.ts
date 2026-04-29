import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '', '')
  const devPort = Number(env.VITE_DEV_PORT || '8080')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://127.0.0.1:8081'

  return {
    plugins: [vue()],
    server: {
      port: devPort,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
  }
})

