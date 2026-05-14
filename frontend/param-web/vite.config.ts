import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '', '');
  const proxyTarget = env.VITE_PROXY_TARGET;
  const configProxyTarget = (env.VITE_CONFIG_PROXY_TARGET || '').trim()

  return {
    plugins: [vue()],
    server: {
      port: 8080,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
        '/core_config/v2': {
          target: configProxyTarget,
          changeOrigin: true,
        }
      },
    },
  }
})
