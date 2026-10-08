import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
    stages: [
        { duration: '10s', target: 20 },
        { duration: '30s', target: 20 },
        { duration: '5s', target: 0 },
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};

export function setup() {
    const tenant = http.post(
        `${BASE}/api/v1/tenants`,
        JSON.stringify({ name: 'k6-load-test' }),
        { headers: JSON_HEADERS });
    const apiKey = tenant.json('apiKey');

    http.post(
        `${BASE}/api/v1/endpoints`,
        JSON.stringify({ url: 'http://host.docker.internal:9090/' }),
        { headers: Object.assign({ 'X-API-Key': apiKey }, JSON_HEADERS) });

    return { apiKey: apiKey };
}

export default function (data) {
    const key = `k6-${__VU}-${__ITER}-${Date.now()}`;
    const res = http.post(
        `${BASE}/api/v1/events`,
        JSON.stringify({ type: 'load.test', payload: '{"n":1}', idempotencyKey: key }),
        { headers: Object.assign({ 'X-API-Key': data.apiKey }, JSON_HEADERS) });

    check(res, { 'status is 201': (r) => r.status === 201 });
}