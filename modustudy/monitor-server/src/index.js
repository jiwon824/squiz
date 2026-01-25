const express = require('express');
const cors = require('cors');
const Docker = require('dockerode');
const si = require('systeminformation');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 9090;
const MONITOR_API_KEY = process.env.MONITOR_API_KEY || 'squiz-monitor-key';

// Docker 클라이언트 (소켓 연결)
const docker = new Docker({ socketPath: '/var/run/docker.sock' });

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, '../public')));

// API Key 인증 미들웨어
const authMiddleware = (req, res, next) => {
  const apiKey = req.headers['x-api-key'] || req.query.apiKey;
  if (apiKey !== MONITOR_API_KEY) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  next();
};

// ============================================================
// 시스템 메트릭 API
// ============================================================

// 전체 시스템 상태
app.get('/api/stats', authMiddleware, async (req, res) => {
  try {
    const [cpu, mem, disk, network, time] = await Promise.all([
      si.currentLoad(),
      si.mem(),
      si.fsSize(),
      si.networkStats(),
      si.time()
    ]);

    res.json({
      timestamp: new Date().toISOString(),
      uptime: time.uptime,
      cpu: {
        usage: Math.round(cpu.currentLoad * 100) / 100,
        cores: cpu.cpus.length
      },
      memory: {
        total: mem.total,
        used: mem.used,
        free: mem.free,
        usagePercent: Math.round((mem.used / mem.total) * 100 * 100) / 100
      },
      disk: disk.map(d => ({
        fs: d.fs,
        mount: d.mount,
        size: d.size,
        used: d.used,
        usagePercent: Math.round(d.use * 100) / 100
      })),
      network: network.map(n => ({
        interface: n.iface,
        rxBytes: n.rx_bytes,
        txBytes: n.tx_bytes,
        rxSec: n.rx_sec,
        txSec: n.tx_sec
      }))
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// CPU 상세
app.get('/api/stats/cpu', authMiddleware, async (req, res) => {
  try {
    const [cpu, cpuTemp] = await Promise.all([
      si.currentLoad(),
      si.cpuTemperature()
    ]);
    res.json({
      currentLoad: cpu.currentLoad,
      cores: cpu.cpus.map((c, i) => ({
        core: i,
        load: c.load
      })),
      temperature: cpuTemp.main
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// 메모리 상세
app.get('/api/stats/memory', authMiddleware, async (req, res) => {
  try {
    const mem = await si.mem();
    res.json({
      total: mem.total,
      used: mem.used,
      free: mem.free,
      available: mem.available,
      buffcache: mem.buffcache,
      swapTotal: mem.swaptotal,
      swapUsed: mem.swapused,
      swapFree: mem.swapfree
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ============================================================
// Docker 컨테이너 API
// ============================================================

// 모든 컨테이너 목록
app.get('/api/containers', authMiddleware, async (req, res) => {
  try {
    const containers = await docker.listContainers({ all: true });
    const containerList = await Promise.all(
      containers
        .filter(c => c.Names.some(n => n.includes('squiz')))
        .map(async (c) => {
          const container = docker.getContainer(c.Id);
          let stats = null;

          if (c.State === 'running') {
            try {
              const statsData = await container.stats({ stream: false });
              const cpuDelta = statsData.cpu_stats.cpu_usage.total_usage -
                              statsData.precpu_stats.cpu_usage.total_usage;
              const systemDelta = statsData.cpu_stats.system_cpu_usage -
                                  statsData.precpu_stats.system_cpu_usage;
              const cpuPercent = (cpuDelta / systemDelta) * 100 * statsData.cpu_stats.online_cpus;

              stats = {
                cpuPercent: Math.round(cpuPercent * 100) / 100,
                memoryUsage: statsData.memory_stats.usage,
                memoryLimit: statsData.memory_stats.limit,
                memoryPercent: Math.round((statsData.memory_stats.usage / statsData.memory_stats.limit) * 100 * 100) / 100
              };
            } catch (e) {
              // stats 가져오기 실패 시 무시
            }
          }

          return {
            id: c.Id.substring(0, 12),
            name: c.Names[0].replace('/', ''),
            image: c.Image,
            state: c.State,
            status: c.Status,
            created: new Date(c.Created * 1000).toISOString(),
            stats
          };
        })
    );

    res.json(containerList);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// 특정 컨테이너 로그
app.get('/api/containers/:id/logs', authMiddleware, async (req, res) => {
  try {
    const { id } = req.params;
    const { tail = 100, since = 0 } = req.query;

    const container = docker.getContainer(id);
    const logs = await container.logs({
      stdout: true,
      stderr: true,
      tail: parseInt(tail),
      since: parseInt(since),
      timestamps: true
    });

    // Buffer를 문자열로 변환하고 줄 단위로 파싱
    const logString = logs.toString('utf8');
    const lines = logString
      .split('\n')
      .filter(line => line.trim())
      .map(line => {
        // Docker 로그 헤더 (8바이트) 제거
        const cleanLine = line.length > 8 ? line.substring(8) : line;
        return cleanLine;
      });

    res.json({ logs: lines });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// 컨테이너 로그 스트리밍 (SSE)
app.get('/api/containers/:id/logs/stream', authMiddleware, async (req, res) => {
  try {
    const { id } = req.params;
    const container = docker.getContainer(id);

    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');

    const logStream = await container.logs({
      stdout: true,
      stderr: true,
      follow: true,
      tail: 50,
      timestamps: true
    });

    logStream.on('data', (chunk) => {
      const line = chunk.toString('utf8').substring(8);
      res.write(`data: ${JSON.stringify({ log: line })}\n\n`);
    });

    logStream.on('end', () => {
      res.end();
    });

    req.on('close', () => {
      logStream.destroy();
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// 컨테이너 재시작
app.post('/api/containers/:id/restart', authMiddleware, async (req, res) => {
  try {
    const { id } = req.params;
    const container = docker.getContainer(id);
    await container.restart();
    res.json({ success: true, message: `Container ${id} restarted` });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ============================================================
// 헬스체크 API
// ============================================================

app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// 대시보드 HTML
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, '../public/index.html'));
});

// ============================================================
// 서버 시작
// ============================================================

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Monitor server running on port ${PORT}`);
  console.log(`Dashboard: http://localhost:${PORT}`);
  console.log(`API Key required: ${MONITOR_API_KEY}`);
});
