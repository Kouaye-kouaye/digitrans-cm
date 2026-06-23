import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const loginFailureRate = new Rate('login_failures');
const batchFailureRate = new Rate('batch_failures');
const loginTrend = new Trend('login_duration');
const batchTrend = new Trend('batch_duration');
const traceTrend = new Trend('trace_duration');
const jwtTrend = new Trend('jwt_verify_duration');

const BASE_GATEWAY = 'http://localhost:8080';
const BASE_SUPPLY = 'http://localhost:8083';
const BASE_CRM = 'http://localhost:8082';
const BASE_ERP = 'http://localhost:8081';

const USERS = ['alice', 'bob', 'charlie', 'diana', 'ernest'];

export let options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 100 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    login_duration: ['p(95)<500', 'avg<200'],
    batch_duration: ['p(95)<1000', 'avg<300'],
    trace_duration: ['p(95)<500', 'avg<200'],
    jwt_verify_duration: ['p(95)<100', 'avg<30'],
    login_failures: ['rate<0.05'],
    batch_failures: ['rate<0.05'],
    http_req_failed: ['rate<0.05'],
  },
};

function getToken(serviceBase, username, password) {
  let payload = JSON.stringify({ username, password, role: 'ADMIN' });
  http.post(`${serviceBase}/api/auth/register`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  let res = http.post(`${serviceBase}/api/auth/login`, JSON.stringify({ username, password }), {
    headers: { 'Content-Type': 'application/json' },
  });
  if (res.status === 200) {
    return JSON.parse(res.body).token;
  }
  return null;
}

export function setup() {
  let tokens = {};
  for (let u of USERS) {
    tokens[u] = {
      gateway: getToken(BASE_GATEWAY, `k6-${u}`, 'test123'),
      supply: getToken(BASE_SUPPLY, `k6-supply-${u}`, 'test123'),
      crm: getToken(BASE_CRM, `k6-crm-${u}`, 'test123'),
      erp: getToken(BASE_ERP, `k6-erp-${u}`, 'test123'),
    };
  }
  return tokens;
}

export default function (tokens) {
  let idx = __VU % USERS.length;
  let user = USERS[idx];
  let t = tokens[user];

  // -- API Gateway: login (JWT generation) --
  let loginRes = http.post(`${BASE_GATEWAY}/api/auth/login`,
    JSON.stringify({ username: `k6-${user}`, password: 'test123' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  loginTrend.add(loginRes.timings.duration);
  loginFailureRate.add(loginRes.status !== 200);
  check(loginRes, { 'login ok': (r) => r.status === 200 });

  // -- API Gateway: JWT verification overhead --
  if (t.gateway) {
    let jwtRes = http.get(`${BASE_GATEWAY}/api/auth/register`, {
      headers: { Authorization: `Bearer ${t.gateway}` },
    });
    jwtTrend.add(jwtRes.timings.duration);
  }

  // -- Supply Chain: create batch (JPA + SHA-256) --
  if (t.supply) {
    let batchRes = http.post(`${BASE_SUPPLY}/api/supply/batches`,
      JSON.stringify({
        plantationId: 1,
        harvestDate: '2026-05-21',
        quantityKg: Math.floor(Math.random() * 2000) + 100,
        qualityGrade: 'A',
        currentLocation: 'Nkolbisson',
      }),
      {
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${t.supply}`,
        },
      }
    );
    batchTrend.add(batchRes.timings.duration);
    batchFailureRate.add(batchRes.status !== 200);
    check(batchRes, { 'batch created': (r) => r.status === 200 });

    if (batchRes.status === 200) {
      let batchCode = JSON.parse(batchRes.body).data.batchCode;

      // Status update
      http.put(`${BASE_SUPPLY}/api/supply/batches/${batchCode}/status`,
        JSON.stringify({ status: 'IN_TRANSIT', location: 'En route', operatorName: 'K6' }),
        { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t.supply}` } }
      );

      // Trace + verify (SHA-256 re-compute)
      let traceRes = http.get(`${BASE_SUPPLY}/api/supply/batches/${batchCode}/trace`, {
        headers: { Authorization: `Bearer ${t.supply}` },
      });
      traceTrend.add(traceRes.timings.duration);

      http.get(`${BASE_SUPPLY}/api/supply/batches/${batchCode}/verify`, {
        headers: { Authorization: `Bearer ${t.supply}` },
      });
    }
  }

  sleep(1);
}

export function teardown(tokens) {
  console.log('K6 load test completed.');
}
