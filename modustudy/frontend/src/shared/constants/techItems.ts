export interface TechItem {
    id: string;
    name: string;
    color: string;
    initial: string;
    icon?: string; // devicon CDN URL
}

// devicon CDN 베이스 URL
const DEVICON_BASE = 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons';

// Hex 색상을 HSL 값으로 변환하는 헬퍼 함수
const hexToHSL = (hex: string): { h: number; s: number; l: number } => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    if (!result) return { h: 0, s: 0, l: 0 };

    const r = parseInt(result[1], 16) / 255;
    const g = parseInt(result[2], 16) / 255;
    const b = parseInt(result[3], 16) / 255;

    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    let h = 0;
    let s = 0;
    const l = (max + min) / 2;

    if (max !== min) {
        const d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        switch (max) {
            case r: h = ((g - b) / d + (g < b ? 6 : 0)) / 6; break;
            case g: h = ((b - r) / d + 2) / 6; break;
            case b: h = ((r - g) / d + 4) / 6; break;
        }
    }

    return { h: h * 360, s, l };
};

// 검정/회색 계열인지 판단 (채도가 낮거나 명도가 매우 낮은 경우)
export const isDarkOrGray = (hex: string): boolean => {
    const { s, l } = hexToHSL(hex);
    return s < 0.15 || l < 0.2;
};

// 원본 기술 스택 목록
const RAW_TECH_ITEMS: TechItem[] = [
    // Languages
    { id: 'Java',        name: 'Java',        color: '#E76F00', initial: 'Ja', icon: `${DEVICON_BASE}/java/java-original.svg` },
    { id: 'Python',      name: 'Python',      color: '#3776AB', initial: 'Py', icon: `${DEVICON_BASE}/python/python-original.svg` },
    { id: 'JavaScript',  name: 'JavaScript',  color: '#F7DF1E', initial: 'JS', icon: `${DEVICON_BASE}/javascript/javascript-original.svg` },
    { id: 'TypeScript',  name: 'TypeScript',  color: '#3178C6', initial: 'TS', icon: `${DEVICON_BASE}/typescript/typescript-original.svg` },
    { id: 'Go',          name: 'Go',          color: '#00ADD8', initial: 'Go', icon: `${DEVICON_BASE}/go/go-original-wordmark.svg` },
    { id: 'Kotlin',      name: 'Kotlin',      color: '#7F52FF', initial: 'Kt', icon: `${DEVICON_BASE}/kotlin/kotlin-original.svg` },
    { id: 'Swift',       name: 'Swift',       color: '#F05138', initial: 'Sw', icon: `${DEVICON_BASE}/swift/swift-original.svg` },
    { id: 'C',           name: 'C',           color: '#A8B9CC', initial: 'C',  icon: `${DEVICON_BASE}/c/c-original.svg` },
    { id: 'C++',         name: 'C++',         color: '#00599C', initial: 'C+', icon: `${DEVICON_BASE}/cplusplus/cplusplus-original.svg` },
    { id: 'Rust',        name: 'Rust',        color: '#CE422B', initial: 'Rs', icon: `${DEVICON_BASE}/rust/rust-original.svg` },
    { id: 'SQL',         name: 'SQL',         color: '#336791', initial: 'SQ', icon: `${DEVICON_BASE}/azuresqldatabase/azuresqldatabase-original.svg` },

    // Backend Frameworks
    { id: 'Spring Boot', name: 'Spring Boot', color: '#6DB33F', initial: 'SB', icon: `${DEVICON_BASE}/spring/spring-original.svg` },
    { id: 'JPA',         name: 'JPA',         color: '#59666C', initial: 'JP', icon: `${DEVICON_BASE}/hibernate/hibernate-original.svg` },
    { id: 'Django',      name: 'Django',      color: '#092E20', initial: 'Dj', icon: `${DEVICON_BASE}/django/django-plain.svg` },
    { id: 'FastAPI',     name: 'FastAPI',     color: '#009688', initial: 'FA', icon: `${DEVICON_BASE}/fastapi/fastapi-original.svg` },
    { id: 'Node.js',     name: 'Node.js',     color: '#339933', initial: 'No', icon: `${DEVICON_BASE}/nodejs/nodejs-original.svg` },
    { id: 'Express',     name: 'Express',     color: '#000000', initial: 'Ex', icon: `${DEVICON_BASE}/express/express-original.svg` },
    { id: 'NestJS',      name: 'NestJS',      color: '#E0234E', initial: 'Ne', icon: `${DEVICON_BASE}/nestjs/nestjs-original.svg` },

    // Frontend Frameworks
    { id: 'React',       name: 'React',       color: '#61DAFB', initial: 'Re', icon: `${DEVICON_BASE}/react/react-original.svg` },
    { id: 'Next.js',     name: 'Next.js',     color: '#000000', initial: 'Nx', icon: `${DEVICON_BASE}/nextjs/nextjs-original.svg` },
    { id: 'Vue',         name: 'Vue',         color: '#4FC08D', initial: 'Vu', icon: `${DEVICON_BASE}/vuejs/vuejs-original.svg` },
    { id: 'Angular',     name: 'Angular',     color: '#DD0031', initial: 'An', icon: `${DEVICON_BASE}/angularjs/angularjs-original.svg` },

    // Mobile
    { id: 'Flutter',      name: 'Flutter',      color: '#02569B', initial: 'Fl', icon: `${DEVICON_BASE}/flutter/flutter-original.svg` },
    { id: 'React Native', name: 'React Native', color: '#61DAFB', initial: 'RN', icon: `${DEVICON_BASE}/react/react-original.svg` },

    // DevOps / Cloud
    { id: 'Docker',      name: 'Docker',      color: '#2496ED', initial: 'Dk', icon: `${DEVICON_BASE}/docker/docker-original.svg` },
    { id: 'Kubernetes',  name: 'Kubernetes',  color: '#326CE5', initial: 'K8', icon: `${DEVICON_BASE}/kubernetes/kubernetes-original.svg` },
    { id: 'AWS',         name: 'AWS',         color: '#FF9900', initial: 'AW', icon: `${DEVICON_BASE}/amazonwebservices/amazonwebservices-original-wordmark.svg` },
    { id: 'GCP',         name: 'GCP',         color: '#4285F4', initial: 'GC', icon: `${DEVICON_BASE}/googlecloud/googlecloud-original.svg` },
    { id: 'Linux',       name: 'Linux',       color: '#FCC624', initial: 'Lx', icon: `${DEVICON_BASE}/linux/linux-original.svg` },
    { id: 'Git',         name: 'Git',         color: '#F05032', initial: 'Gi', icon: `${DEVICON_BASE}/git/git-original.svg` },
    { id: 'Terraform',   name: 'Terraform',   color: '#7B42BC', initial: 'Tf', icon: `${DEVICON_BASE}/terraform/terraform-original.svg` },

    // Databases
    { id: 'MySQL',       name: 'MySQL',       color: '#4479A1', initial: 'My', icon: `${DEVICON_BASE}/mysql/mysql-original.svg` },
    { id: 'PostgreSQL',  name: 'PostgreSQL',  color: '#4169E1', initial: 'Pg', icon: `${DEVICON_BASE}/postgresql/postgresql-original.svg` },
    { id: 'MongoDB',     name: 'MongoDB',     color: '#47A248', initial: 'Mg', icon: `${DEVICON_BASE}/mongodb/mongodb-original.svg` },
    { id: 'Redis',       name: 'Redis',       color: '#DC382D', initial: 'Rd', icon: `${DEVICON_BASE}/redis/redis-original.svg` },
    { id: 'Kafka',       name: 'Kafka',       color: '#231F20', initial: 'Kf', icon: `${DEVICON_BASE}/apachekafka/apachekafka-original.svg` },

    // AI/ML
    { id: 'PyTorch',     name: 'PyTorch',     color: '#EE4C2C', initial: 'PT', icon: `${DEVICON_BASE}/pytorch/pytorch-original.svg` },
    { id: 'TensorFlow',  name: 'TensorFlow',  color: '#FF6F00', initial: 'TF', icon: `${DEVICON_BASE}/tensorflow/tensorflow-original.svg` },

    // API
    { id: 'GraphQL',     name: 'GraphQL',     color: '#E10098', initial: 'GQ', icon: `${DEVICON_BASE}/graphql/graphql-plain.svg` },

    // 기타
    { id: 'Other',       name: '그 외',       color: '#9CA3AF', initial: '···' },
];

// 색상(Hue) 기준으로 정렬된 기술 스택 목록
// 빨강 → 주황 → 노랑 → 초록 → 청록 → 파랑 → 보라 → 분홍 → 검정/회색 순서
export const TECH_ITEMS: TechItem[] = [...RAW_TECH_ITEMS].sort((a, b) => {
    const isDarkA = isDarkOrGray(a.color);
    const isDarkB = isDarkOrGray(b.color);

    // 검정/회색 계열은 맨 뒤로
    if (isDarkA && !isDarkB) return 1;
    if (!isDarkA && isDarkB) return -1;

    // 둘 다 검정/회색이면 명도순
    if (isDarkA && isDarkB) {
        return hexToHSL(a.color).l - hexToHSL(b.color).l;
    }

    // 일반 색상은 Hue 순
    return hexToHSL(a.color).h - hexToHSL(b.color).h;
});
