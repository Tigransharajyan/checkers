const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const base = process.env.BASE_URL || 'http://localhost:8080';

/** Checks real form submission, duplicate protection, bounded context and safe text rendering. */
test('assistant sends bounded user-only context once and renders answers as plain text', { timeout: 30000 }, async () => {
  const browser = await chromium.launch({
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    headless: true, args: ['--no-sandbox'],
  });
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
    const requests = [];
    /** Replaces only assistant responses so browser tests never spend real provider quotas. */
    await page.route('**/api/assistant/chat', async route => {
      requests.push(route.request().postDataJSON());
      await new Promise(resolve => setTimeout(resolve, 100));
      await route.fulfill({ json: { answer: '<b id="injected-marker">Site help</b>', matched: true, faqPath: '/faq?lang=en' } });
    });
    await page.goto(base + '/faq?lang=en');
    await page.locator('[data-assistant-toggle]').click();
    for (let index = 0; index < 6; index++) {
      await page.locator('[data-assistant-input]').fill('Site question ' + index);
      /** Simulates two rapid submissions while the first request is still pending. */
      await page.locator('[data-assistant-form]').evaluate(form => {
        form.requestSubmit();
        form.requestSubmit();
      });
      await page.waitForFunction(() => !document.querySelector('[data-assistant-send]').disabled);
      assert.equal(requests.length, index + 1);
      assert.equal(requests[index].locale, 'en');
      assert.deepEqual(requests[index].recentQuestions,
        Array.from({ length: index }, (_, n) => 'Site question ' + n).slice(-4));
      assert.equal(await page.locator('.assistant-msg.bot p').last().textContent(), '<b id="injected-marker">Site help</b>');
      assert.equal(await page.locator('#injected-marker').count(), 0);
      assert(!JSON.stringify(requests[index]).includes('gsk_'));
    }
    await page.reload();
    await page.locator('[data-assistant-toggle]').click();
    assert.equal(await page.locator('.assistant-msg.user').count(), 6);
  } finally { await browser.close(); }
});
