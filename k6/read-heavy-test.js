import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ── 커스텀 메트릭 ──
const errorRate = new Rate('error_rate');
const getByIdDuration = new Trend('get_by_id_duration', true);
const listAllDuration = new Trend('list_all_duration', true);

// ── 읽기 집중 시나리오 ──
// 캐싱 효과를 측정하려면 읽기 비중이 높아야 함
// 실제 쇼핑몰: 읽기 90% / 쓰기 10%
export const options = {
    scenarios: {
        read_heavy: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 100 },
                { duration: '1m', target: 100 },
                { duration: '20s', target: 300 },
                { duration: '1m', target: 300 },
                { duration: '20s', target: 500 },
                { duration: '1m', target: 500 },
                { duration: '20s', target: 0 },
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<300', 'p(99)<500'],
        error_rate: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ── setup: 테스트 데이터 미리 등록 ──
export function setup() {
    const ids = [];
    for (let i = 0; i < 20; i++) {
        const payload = JSON.stringify({
            name: `테스트 상품 ${i}`,
            price: 100000 + i * 10000,
            seller: 'W컨셉',
            description: `테스트 상품 설명 ${i}`,
            options: [
                { size: 'M', color: 'BLACK', additionalPrice: 0, stockQuantity: 100 },
            ],
            images: [
                { url: `https://cdn.example.com/img-${i}.jpg`, sortOrder: 0, primaryImage: true },
            ],
        });

        const res = http.post(`${BASE_URL}/product`, payload, {
            headers: { 'Content-Type': 'application/json' },
        });

        if (res.status === 201) {
            ids.push(res.json('data.id'));
        }
    }
    return { productIds: ids };
}

// ── 테스트 시나리오: 읽기 90% / 쓰기 10% ──
export default function (data) {
    const ids = data.productIds;
    if (ids.length === 0) return;

    const randomId = ids[Math.floor(Math.random() * ids.length)];
    const roll = Math.random();

    if (roll < 0.6) {
        // 60% — 단건 조회 (캐싱 대상)
        const res = http.get(`${BASE_URL}/product/${randomId}`);
        getByIdDuration.add(res.timings.duration);
        const ok = check(res, { 'GET /{id} → 200': (r) => r.status === 200 });
        errorRate.add(!ok);
    } else if (roll < 0.9) {
        // 30% — 전체 조회
        const res = http.get(`${BASE_URL}/product`);
        listAllDuration.add(res.timings.duration);
        const ok = check(res, { 'GET / → 200': (r) => r.status === 200 });
        errorRate.add(!ok);
    } else {
        // 10% — 상품 등록 (쓰기)
        const payload = JSON.stringify({
            name: `신규 상품 ${Date.now()}`,
            price: 200000,
            seller: 'W컨셉',
            description: '동적 생성',
            options: [{ size: 'S', color: 'RED', additionalPrice: 0, stockQuantity: 5 }],
            images: [{ url: `https://cdn.example.com/new-${Date.now()}.jpg`, sortOrder: 0, primaryImage: true }],
        });
        http.post(`${BASE_URL}/product`, payload, {
            headers: { 'Content-Type': 'application/json' },
        });
    }

    sleep(0.1);
}

// ── 결과 요약 ──
export function handleSummary(data) {
    const summary = {
        timestamp: new Date().toISOString(),
        scenario: 'read-heavy (60% getById + 30% listAll + 10% create)',
        vus_max: data.metrics.vus_max ? data.metrics.vus_max.values.max : 0,
        http_reqs: data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0,
        http_req_duration_avg: data.metrics.http_req_duration ? data.metrics.http_req_duration.values.avg.toFixed(2) : 0,
        http_req_duration_p95: data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'].toFixed(2) : 0,
        http_req_duration_p99: data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(99)'].toFixed(2) : 0,
        error_rate: data.metrics.error_rate ? (data.metrics.error_rate.values.rate * 100).toFixed(2) + '%' : '0%',
        get_by_id_avg: data.metrics.get_by_id_duration ? data.metrics.get_by_id_duration.values.avg.toFixed(2) : 0,
        get_by_id_p95: data.metrics.get_by_id_duration ? data.metrics.get_by_id_duration.values['p(95)'].toFixed(2) : 0,
        list_all_avg: data.metrics.list_all_duration ? data.metrics.list_all_duration.values.avg.toFixed(2) : 0,
        list_all_p95: data.metrics.list_all_duration ? data.metrics.list_all_duration.values['p(95)'].toFixed(2) : 0,
    };

    return {
        'stdout': JSON.stringify(summary, null, 2) + '\n',
        'k6/read-heavy-result.json': JSON.stringify(summary, null, 2),
    };
}