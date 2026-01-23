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
        host: '0.0.0.0',
        port: 3000,
        open: true,
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
                    // React 코어 라이브러리 분리
                    'react-vendor': ['react', 'react-dom', 'react-router-dom'],
                    // UI 라이브러리 분리
                    'ui-vendor': ['lucide-react'],
                },
            },
        },
    },
});
