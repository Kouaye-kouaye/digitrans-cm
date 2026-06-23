import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  vus: 1,
  iterations: 5,
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.1'],
  },
};

const BASE = 'http://localhost:8080';

function getToken() {
  const user = `smoke-${Date.now()}`;
  http.post(`${BASE}/api/auth/register`,
    JSON.stringify({ username: user, password: 'test123', role: 'ADMIN' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  let res = http.post(`${BASE}/api/auth/login`,
    JSON.stringify({ username: user, password: 'test123' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  return res.status === 200 ? JSON.parse(res.body).token : null;
}

export default function () {
  let token = getToken();
  check(token, { 'token obtained': (t) => t !== null });

  if (token) {
    // Protected endpoint - 404 expected but JWT is validated
    let res = http.get(`${BASE}/api/auth/check`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    check(res, { 'jwt accepted': (r) => r.status === 404 });
  }
  sleep(1);
}
