import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  testMatch: /.*\.spec\.ts/,
  timeout: 180_000,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: process.env.CSAGENT_UI_URL ?? 'http://localhost:5173',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
});
