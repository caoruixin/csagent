import { expect, test } from '@playwright/test';

const apiBase = process.env.CSAGENT_API_URL ?? 'http://localhost:8080';
const artifacts = new URL('../artifacts/', import.meta.url).pathname;

test.beforeEach(async ({ request }, testInfo) => {
  try {
    const health = await request.get(`${apiBase}/actuator/health`, { timeout: 10_000 });
    if (!health.ok()) {
      testInfo.skip(true, `UNVERIFIED: API health is ${health.status()} at ${apiBase}; services are user-managed.`);
    }
  } catch (error) {
    testInfo.skip(true, `UNVERIFIED: this runner cannot reach ${apiBase}: ${String(error)}`);
  }
});

test('scenario 7: pre-chat to grounded answer renders bot bubble and source card', async ({ page }) => {
  // Product criterion: the browser flow must preserve form context and render a sourced self-service FAQ answer.
  await page.goto('/');
  await page.getByRole('button', { name: 'Open chat' }).click();
  await page.locator('input[type="text"]').first().fill('Riley');
  await page.locator('input[type="email"]').fill('riley.ui.e2e@example.test');
  await page.locator('select').selectOption('Ad Support');
  await page.locator('input[type="text"]').nth(1).fill('AD-2002');
  await page.locator('textarea').fill('How do I relist my advert after it expired?');
  await page.getByRole('button', { name: 'Start Chat' }).click();

  await expect(page.getByText('Gumtree Support')).toBeVisible();
  await expect(page.getByText(/Hi Riley!/)).toBeVisible();
  await page.getByPlaceholder('Type a message...').fill('How do I relist my advert after it expired?');
  await page.getByRole('button', { name: 'Send' }).click();
  await expect(page.getByText('How do I relist my advert after it expired?', { exact: true })).toBeVisible();
  await expect(page.getByPlaceholder('Type a message...')).toBeEnabled({ timeout: 90_000 });
  await page.screenshot({ path: `${artifacts}/07-chat-e2e.png`, fullPage: true });

  const sourceCards = page.locator('a[href^="http"]');
  await expect.poll(async () => sourceCards.count(), { timeout: 5_000 }).toBeGreaterThan(0);
});

test('scenario 8: admin lists a session and opens its trace', async ({ page, request }) => {
  // Product criterion: Admin trace is an operationally critical browser flow, not a dead link.
  const create = await request.post(`${apiBase}/v1/chat/sessions`, {
    data: {
      first_name: 'Riley',
      email: 'riley.admin.e2e@example.test',
      topic_subject: 'Ad Support',
      ad_id: 'AD-2002',
      description: 'Admin trace verification session.',
    },
  });
  await expect(create).toBeOK();
  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Admin Dashboard' })).toBeVisible();
  await expect(page.getByText('Loading sessions...')).toBeHidden({ timeout: 20_000 });
  const firstSessionRow = page.locator('tbody tr').first();
  await expect(firstSessionRow).toBeVisible();
  await firstSessionRow.click();
  await expect(page.getByRole('heading', { name: /Trace:/ })).toBeVisible({ timeout: 20_000 });
  await expect(page.getByText('Loading trace...')).toBeHidden({ timeout: 20_000 });
  await page.screenshot({ path: `${artifacts}/08-admin-trace.png`, fullPage: true });
});
