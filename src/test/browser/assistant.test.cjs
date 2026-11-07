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

/** Verifies that help is reachable without scrolling and fits the visible area in all languages. */
test('assistant launcher stays fixed and its panel fits desktop, mobile and landscape screens', { timeout: 60000 }, async () => {
  const browser = await chromium.launch({
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    headless: true, args: ['--no-sandbox'],
  });
  try {
    const page = await browser.newPage();
    for (const viewport of [{ width: 1440, height: 900 }, { width: 390, height: 844 },
      { width: 320, height: 740 }, { width: 844, height: 390 }]) {
      await page.setViewportSize(viewport);
      for (const language of ['en', 'ru', 'hy']) {
        await page.goto(base + '/faq?lang=' + language);
        const toggle = page.locator('[data-assistant-toggle]');
        const before = await toggle.boundingBox();
        assert(before && before.y > 0 && before.y + before.height <= viewport.height);
        assert.equal(await toggle.evaluate(el => getComputedStyle(el.closest('.assistant')).position), 'fixed');
        await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
        const after = await toggle.boundingBox();
        assert(Math.abs(before.x - after.x) < 1 && Math.abs(before.y - after.y) < 1);
        await toggle.click();
        const panel = page.locator('[data-assistant-panel]');
        await panel.waitFor({ state: 'visible' });
        const rect = await panel.boundingBox();
        assert(rect.x >= 10 && rect.y >= 10, JSON.stringify({ viewport, language, rect }));
        assert(rect.x + rect.width <= viewport.width - 10);
        assert(rect.y + rect.height <= viewport.height - 10);
        assert(await page.locator('[data-assistant-input]').evaluate(el => el === document.activeElement));
        assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
        if (viewport.width <= 900 && language === 'hy') {
          await page.locator('[data-assistant-backdrop]').click({ position: { x: 2, y: 2 } });
        } else {
          await page.keyboard.press('Escape');
        }
        await panel.waitFor({ state: 'hidden' });
        assert(await toggle.evaluate(el => el === document.activeElement));
        assert(!(await page.locator('#main-content').evaluate(el => el.inert)));
      }
    }
    await page.setViewportSize({ width: 1280, height: 900 });
    await page.goto(base + '/play?lang=en');
    await page.locator('[data-start-bot]').click();
    await page.waitForURL('**/play?id=*');
    await page.locator('[data-board] .piece').first().waitFor();
    for (const viewport of [{ width: 1280, height: 720 }, { width: 390, height: 844 },
      { width: 844, height: 390 }, { width: 320, height: 568 }]) {
      await page.setViewportSize(viewport);
      await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
      const overlaps = await page.evaluate(() => {
        const launcher = document.querySelector('[data-assistant-toggle]').getBoundingClientRect();
        return ['[data-resign-btn]', '[data-draw-offer-btn]', '[data-flip-board]', '.game-lobby-link', '[data-board]']
          .filter(selector => {
            const rect = document.querySelector(selector).getBoundingClientRect();
            return Math.min(launcher.right, rect.right) > Math.max(launcher.left, rect.left)
              && Math.min(launcher.bottom, rect.bottom) > Math.max(launcher.top, rect.top);
          });
      });
      assert.deepEqual(overlaps, [], JSON.stringify(viewport));
    }
    await page.locator('[data-resign-btn]').click();
    await page.locator('[data-confirm-resign]').click();
  } finally { await browser.close(); }
});

/** Exercises visible typing feedback, multiline entry, reduced motion and cleanup after failures. */
test('assistant shows animated typing until success or failure without saving the indicator', { timeout: 30000 }, async () => {
  const browser = await chromium.launch({
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    headless: true, args: ['--no-sandbox'],
  });
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
    let release, fail = false;
    const pending = new Promise(resolve => { release = resolve; });
    await page.route('**/api/assistant/chat', async route => {
      if (!fail) await pending;
      await route.fulfill(fail ? { status: 500, json: { errorCode: 'INTERNAL_ERROR' } }
        : { json: { answer: 'A checked site answer', matched: true, faqPath: '/faq?lang=ru' } });
    });
    await page.goto(base + '/faq?lang=ru');
    await page.locator('[data-assistant-toggle]').click();
    const input = page.locator('[data-assistant-input]');
    await input.fill('Как начать?');
    await input.press('Enter');
    const typing = page.locator('[data-assistant-typing]');
    await typing.waitFor({ state: 'visible' });
    assert((await typing.innerText()).includes('Ассистент печатает'));
    assert(await page.locator('[data-assistant-send]').isDisabled());
    assert.equal(await page.locator('[data-assistant-form]').getAttribute('aria-busy'), 'true');
    assert.notEqual(await page.locator('.assistant-typing-dots i').first().evaluate(el => getComputedStyle(el).animationName), 'none');
    await input.fill('Первая строка');
    await input.press('Shift+Enter');
    await input.press('B');
    assert((await input.inputValue()).includes('\n'));
    await page.emulateMedia({ reducedMotion: 'reduce' });
    assert.equal(await page.locator('.assistant-typing-dots i').first().evaluate(el => getComputedStyle(el).animationName), 'none');
    release();
    await typing.waitFor({ state: 'hidden' });
    assert(!(await page.locator('[data-assistant-send]').isDisabled()));
    assert.equal(await page.locator('.assistant-msg.bot p').last().textContent(), 'A checked site answer');
    const history = await page.evaluate(() => sessionStorage.getItem('checkers.assistant.session.ru'));
    assert(!history.includes('печатает'));
    fail = true;
    await input.fill('Повторить вопрос');
    await input.press('Enter');
    await page.waitForFunction(() => !document.querySelector('[data-assistant-send]').disabled);
    assert(!(await typing.isVisible()));
    assert.equal(await page.locator('[data-assistant-form]').getAttribute('aria-busy'), 'false');
    await page.locator('[data-assistant-close]').click();
    await page.locator('[data-assistant-panel]').waitFor({ state: 'hidden' });
    assert(await page.locator('[data-assistant-toggle]').evaluate(el => el === document.activeElement));
  } finally { await browser.close(); }
});
