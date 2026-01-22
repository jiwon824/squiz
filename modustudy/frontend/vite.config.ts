import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
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
        port: 3000,
        open: true,
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
