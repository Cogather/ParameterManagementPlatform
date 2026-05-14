import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '', '')
  const devPort = Number(env.VITE_DEV_PORT || '8080')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://127.0.0.1:8081'
  const configProxyTarget = (env.VITE_CONFIG_PROXY_TARGET || '').trim()

  const proxy: Record<string, { target: string; changeOrigin: boolean }> = {
    '/api': {
      target: proxyTarget,
      changeOrigin: true,
    },
  }
  if (configProxyTarget) {
    proxy['/config'] = {
      target: configProxyTarget,
      changeOrigin: true,
    }
  }

  return {
    plugins: [vue()],
    server: {
      port: devPort,
      proxy,
    },
  }
})
