import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 빌드 산출물이 곧바로 Spring Boot 의 정적 리소스 자리에 놓인다. 복사 단계가 없다.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    // 개발 중에는 Vite dev 서버로 띄우고 API 만 백엔드로 넘긴다.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
