export interface TechItem {
    id: string;
    name: string;
    color: string;
    initial: string;
}

export const TECH_ITEMS: TechItem[] = [
    // Languages
    { id: 'Java',        name: 'Java',        color: '#E76F00', initial: 'Ja' },
    { id: 'Python',      name: 'Python',      color: '#3776AB', initial: 'Py' },
    { id: 'JavaScript',  name: 'JavaScript',  color: '#F7DF1E', initial: 'JS' },
    { id: 'TypeScript',  name: 'TypeScript',  color: '#3178C6', initial: 'TS' },
    { id: 'Go',          name: 'Go',          color: '#00ADD8', initial: 'Go' },
    { id: 'Kotlin',      name: 'Kotlin',      color: '#7F52FF', initial: 'Kt' },
    { id: 'Swift',       name: 'Swift',       color: '#F05138', initial: 'Sw' },
    { id: 'C',           name: 'C',           color: '#A8B9CC', initial: 'C'  },
    { id: 'C++',         name: 'C++',         color: '#00599C', initial: 'C+' },
    { id: 'Rust',        name: 'Rust',        color: '#CE422B', initial: 'Rs' },
    { id: 'SQL',         name: 'SQL',         color: '#336791', initial: 'SQ' },

    // Backend Frameworks
    { id: 'Spring Boot', name: 'Spring Boot', color: '#6DB33F', initial: 'SB' },
    { id: 'JPA',         name: 'JPA',         color: '#59666C', initial: 'JP' },
    { id: 'Django',      name: 'Django',      color: '#092E20', initial: 'Dj' },
    { id: 'FastAPI',     name: 'FastAPI',     color: '#009688', initial: 'FA' },
    { id: 'Node.js',     name: 'Node.js',     color: '#339933', initial: 'No' },
    { id: 'Express',     name: 'Express',     color: '#000000', initial: 'Ex' },
    { id: 'NestJS',      name: 'NestJS',      color: '#E0234E', initial: 'Ne' },

    // Frontend Frameworks
    { id: 'React',       name: 'React',       color: '#61DAFB', initial: 'Re' },
    { id: 'Next.js',     name: 'Next.js',     color: '#000000', initial: 'Nx' },
    { id: 'Vue',         name: 'Vue',         color: '#4FC08D', initial: 'Vu' },
    { id: 'Angular',     name: 'Angular',     color: '#DD0031', initial: 'An' },

    // Mobile
    { id: 'Flutter',      name: 'Flutter',      color: '#02569B', initial: 'Fl' },
    { id: 'React Native', name: 'React Native', color: '#61DAFB', initial: 'RN' },

    // DevOps / Cloud
    { id: 'Docker',      name: 'Docker',      color: '#2496ED', initial: 'Dk' },
    { id: 'Kubernetes',  name: 'Kubernetes',  color: '#326CE5', initial: 'K8' },
    { id: 'AWS',         name: 'AWS',         color: '#FF9900', initial: 'AW' },
    { id: 'GCP',         name: 'GCP',         color: '#4285F4', initial: 'GC' },
    { id: 'Linux',       name: 'Linux',       color: '#FCC624', initial: 'Lx' },
    { id: 'Git',         name: 'Git',         color: '#F05032', initial: 'Gi' },
    { id: 'Terraform',   name: 'Terraform',   color: '#7B42BC', initial: 'Tf' },

    // Databases
    { id: 'MySQL',       name: 'MySQL',       color: '#4479A1', initial: 'My' },
    { id: 'PostgreSQL',  name: 'PostgreSQL',  color: '#4169E1', initial: 'Pg' },
    { id: 'MongoDB',     name: 'MongoDB',     color: '#47A248', initial: 'Mg' },
    { id: 'Redis',       name: 'Redis',       color: '#DC382D', initial: 'Rd' },
    { id: 'Kafka',       name: 'Kafka',       color: '#231F20', initial: 'Kf' },

    // AI/ML
    { id: 'PyTorch',     name: 'PyTorch',     color: '#EE4C2C', initial: 'PT' },
    { id: 'TensorFlow',  name: 'TensorFlow',  color: '#FF6F00', initial: 'TF' },

    // API
    { id: 'GraphQL',     name: 'GraphQL',     color: '#E10098', initial: 'GQ' },
];
