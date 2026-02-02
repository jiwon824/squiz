/// <reference types="vite/client" />

declare module '*.md?raw' {
    const content: string;
    export default content;
}

interface ImportMetaEnv {
    readonly VITE_API_URL: string;
    // 다른 환경 변수들을 여기에 추가
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}
