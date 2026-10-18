const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const base = process.env.BASE_URL || 'http://localhost:8080';

test('public content, native language links and local resources work on mobile and desktop', { timeout: 90000 }, async () => {
  const browser = await chromium.launch({
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    headless: true, args: ['--no-sandbox'],
  });
  try {
    const page = await browser.newPage();
    const errors = [], external = [], brokenAssets = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('request', request => {
      if (new URL(request.url()).origin !== new URL(base).origin) external.push(request.url());
    });
    page.on('response', response => {
      if (response.status() >= 400 && /\/(css|js|fonts|images)\//.test(response.url())) brokenAssets.push(response.url());
    });
    for (const width of [390, 1440]) {
      await page.setViewportSize({ width, height: 1000 });
      for (const lang of ['en', 'ru', 'hy']) {
        for (const route of ['/', '/faq']) {
          const response = await page.goto(`${base}${route}?lang=${lang}`);
          assert.equal(response.status(), 200);
          await page.evaluate(() => document.fonts.ready);
          assert.equal(await page.locator('html').getAttribute('lang'), lang);
          assert.equal(await page.locator('h1').count(), 1);
          assert.equal(new URL(await page.locator('link[rel=canonical]').getAttribute('href')).search, `?lang=${lang}`);
          assert.equal(await page.locator('link[rel=alternate][hreflang]').count(), 4);
          assert(await page.locator('meta[name=description]').getAttribute('content'));
          const graph = JSON.parse(await page.locator('script[type="application/ld+json"]').textContent());
          assert.equal(graph['@context'], 'https://schema.org');
          assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
          // Check the live DOM after common.js has rewritten ordinary navigation links.
          for (const target of ['en', 'ru', 'hy']) {
            const link = page.locator(`a[data-lang="${target}"]`).first();
            assert.equal(new URL(await link.getAttribute('href'), base).searchParams.get('lang'), target);
          }
        }
      }
    }
    await page.locator('a[data-lang="en"]').first().click();
    await page.waitForURL('**/faq?lang=en');
    assert.equal(await page.locator('html').getAttribute('lang'), 'en');
    assert.deepEqual(errors, []);
    assert.deepEqual(external, []);
    assert.deepEqual(brokenAssets, []);
  } finally { await browser.close(); }
});
