import http from 'k6/http';
import { check } from 'k6';

export const options = {
  insecureSkipTLSVerify: true,
  stages: [
    { duration: '15s', target: 500 },
    { duration: '30s', target: 500 },
    { duration: '5s',  target: 0 },
  ],
};

export default function () {
  const res = http.post(
    'https://localhost:8080/api/auth/login',
    JSON.stringify({ email: 'worker@test.com', password: __ENV.TEST_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}