import http from 'k6/http';
import { Counter } from 'k6/metrics';

// two counters so the summary shows exactly what happened
const wrongGuess = new Counter('guesses_401');   // Eve's guess was checked and rejected
const blocked = new Counter('blocked_429');      // Eve was stopped by the rate limiter

export const options = {
  insecureSkipTLSVerify: true,
  scenarios: {
    eve: {
      executor: 'shared-iterations',  // a fixed number of attempts in total
      vus: 10,                        // 10 connections, so nginx spreads them over the 3 containers
      iterations: 30,                 // Eve tries 30 wrong passwords in total
      maxDuration: '30s',
    },
  },
};

export default function () {
  const res = http.post(
    'https://localhost:8080/api/auth/login',
    JSON.stringify({ email: 'worker@test.com', password: 'WrongPass1@' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (res.status === 401) wrongGuess.add(1);
  if (res.status === 429) blocked.add(1);
}