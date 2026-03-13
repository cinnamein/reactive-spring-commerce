import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ── 커스텀 메트릭 ──
const errorRate = new Rate('error_rate');
const createDuration = new Trend('create_duration', true);
const getDuration = new Trend('get_duration', true);
const listDuration = new Trend('list_duration', true);

// ── 부하 시나리오 ──
// 동시 접속을 단계적으로 올리면서 안정성 확인
export const options = {
    scenarios: {
        ramp_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },    // 30초간 50명까지
                { duration: '1m', target: 100 },     // 1분간 100명 유지
                { duration: '30s', target: 200 },    // 30초간 200명까지
                { duration: '1m', target: 200 },     // 1분간 200명 유지
                { duration: '30s', target: 500 },    // 30초간 500명까지
                { duration: '1m', target: 500 },     // 1분간 500명 유지
                { duration: '30s', target: 0 },      // 30초간 0명으로 감소
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],  // p95 < 500ms, p99 < 1s
        error_rate: ['rate<0.01'],                         // 에러율 < 1%
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// ── 테스트 시나리오 ──
export default function () {
    // 1. 상품 등록
    const createPayload = JSON.stringify({
        name: `부하테스트 상품 ${Date.now()}`,
        price: 100000 + Math.floor(Math.random() * 900000),
        seller: 'W컨셉',
        description: '부하 테스트용 상품입니다',
        options: [
            { size: 'M', color: 'BLACK', additionalPrice: 0, stockQuantity: 10 },
            { size: 'L', color: 'WHITE', additionalPrice: 5000, stockQuantity: 5 },
        ],
        images: [
            { url: `https://cdn.example.com/img-${Date.now()}.jpg`, sortOrder: 0, primaryImage: true },
        ],
    });

    const createRes = http.post(`${BASE_URL}/product`, createPayload, {
        headers: { 'Content-Type': 'application/json' },
    });

    createDuration.add(createRes.timings.duration);
    const createOk = check(createRes, {
        'POST /product → 201': (r) => r.status === 201,
    });
    errorRate.add(!createOk);

    if (createRes.status !== 201) {
        sleep(0.5);
        return;
    }

    const productId = createRes.json('data.id');

    // 2. 단건 조회
    const getRes = http.get(`${BASE_URL}/product/${productId}`);
    getDuration.add(getRes.timings.duration);
    const getOk = check(getRes, {
        'GET /product/{id} → 200': (r) => r.status === 200,
    });
    errorRate.add(!getOk);

    // 3. 전체 조회
    const listRes = http.get(`${BASE_URL}/product`);
    listDuration.add(listRes.timings.duration);
    const listOk = check(listRes, {
        'GET /product → 200': (r) => r.status === 200,
    });
    errorRate.add(!listOk);

    // 4. 상태 전이 (publish)
    const publishRes = http.patch(`${BASE_URL}/product/${productId}/publish`);
    check(publishRes, {
        'PATCH /publish → 200': (r) => r.status === 200,
    });

    // 5. 삭제
    const deleteRes = http.del(`${BASE_URL}/product/${productId}`);
    check(deleteRes, {
        'DELETE /product/{id} → 200': (r) => r.status === 200,
    });

    sleep(0.3);
}

// ── 결과 요약 ──
export function handleSummary(data) {
    const summary = {
        timestamp: new Date().toISOString(),
        vus_max: data.metrics.vus_max ? data.metrics.vus_max.values.max : 0,
        http_reqs: data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 0,
        http_req_duration_avg: data.metrics.http_req_duration ? data.metrics.http_req_duration.values.avg.toFixed(2) : 0,
        http_req_duration_p95: data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'].toFixed(2) : 0,
        http_req_duration_p99: data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(99)'].toFixed(2) : 0,
        error_rate: data.metrics.error_rate ? (data.metrics.error_rate.values.rate * 100).toFixed(2) + '%' : '0%',
        create_avg: data.metrics.create_duration ? data.metrics.create_duration.values.avg.toFixed(2) : 0,
        get_avg: data.metrics.get_duration ? data.metrics.get_duration.values.avg.toFixed(2) : 0,
        list_avg: data.metrics.list_duration ? data.metrics.list_duration.values.avg.toFixed(2) : 0,
    };

    return {
        'stdout': JSON.stringify(summary, null, 2) + '\n',
        'k6/load-test-result.json': JSON.stringify(summary, null, 2),
    };
}