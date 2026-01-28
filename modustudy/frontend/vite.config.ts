import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import fs from 'fs';
import path from 'path';

export default defineConfig({
    assetsInclude: ['**/*.lottie'],
    plugins: [react()],
    define: {
        global: 'window',
    },
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    server: {
        allowedHosts: true,
        host: '0.0.0.0',
        port: 3000,
        open: true,
        proxy: {
            // 꼬멘틀 AI 서비스 (Python Flask - port 5000)
            '/api/words': {
                target: 'http://localhost:5000',
                changeOrigin: true,
                secure: false,
            },
            '/api/embedding': {
                target: 'http://localhost:5000',
                changeOrigin: true,
                secure: false,
            },
            '/api/leaderboard': {
                target: 'http://localhost:5000',
                changeOrigin: true,
                secure: false,
            },
            '/api/categories': {
                target: 'http://localhost:5000',
                changeOrigin: true,
                secure: false,
            },
            '/api/difficulties': {
                target: 'http://localhost:5000',
                changeOrigin: true,
                secure: false,
            },
            '/api/health': {
                target: 'http://localhost:5000',
                changeOrigin: true,
                secure: false,
            },
            // 메인 백엔드 (Spring Boot - port 8080)
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false,
                // 쿠키 도메인 재작성: modustudy.local로 접속 시 도메인 유지
                cookieDomainRewrite: 'modustudy.local',
            },
            '/oauth2': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false,
                cookieDomainRewrite: 'modustudy.local',
            },
        },
        https: (() => {
            const certPath = process.env.VITE_HTTPS_CERT
                ? path.resolve(process.env.VITE_HTTPS_CERT)
                : path.resolve(__dirname, '..', 'modustudy.local.pem');
            const keyPath = process.env.VITE_HTTPS_KEY
                ? path.resolve(process.env.VITE_HTTPS_KEY)
                : path.resolve(__dirname, '..', 'modustudy.local-key.pem');

            if (!fs.existsSync(certPath) || !fs.existsSync(keyPath)) {
                return undefined;
            }

            return {
                cert: fs.readFileSync(certPath),
                key: fs.readFileSync(keyPath),
            };
        })(),
    },
    build: {
        rollupOptions: {
            output: {
                manualChunks: {
                    'react-vendor': ['react', 'react-dom', 'react-router-dom'],
                    'ui-vendor': ['lucide-react'],
                },
            },
        },
    },
});
