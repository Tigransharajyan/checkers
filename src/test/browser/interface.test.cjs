const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const base = process.env.BASE_URL || 'http://localhost:8080';
const launch = () => chromium.launch({ executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined, headless: true, args: ['--no-sandbox'] });

test('drawer, help and dialogs retain keyboard focus and clear inert on resize', { timeout: 60000 }, async () => {
  const browser = await launch();
  try {
    const context = await browser.newContext({ viewport: { width: 390, height: 844 } });
    const page = await context.newPage();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.goto(base + '/?lang=en');
    await page.locator('#main-content').focus();
    await page.keyboard.press('Tab');
    assert(await page.locator('[data-find-random]').evaluate(el => el === document.activeElement));
    const menu = page.locator('[data-sidebar-toggle]');
    await menu.click();
    assert.equal(await menu.getAttribute('aria-expanded'), 'true');
    assert(await page.locator('#main-content').evaluate(el => el.inert));
    await page.keyboard.press('Escape');
    assert(await menu.evaluate(el => el === document.activeElement));
    assert(!(await page.locator('#main-content').evaluate(el => el.inert)));
    await menu.click();
    await page.setViewportSize({ width: 901, height: 900 });
    assert(!(await page.locator('#main-content').evaluate(el => el.inert)));
    assert(!(await page.locator('[data-sidebar]').evaluate(el => el.inert)));
    await page.setViewportSize({ width: 900, height: 900 });
    await page.waitForFunction(() => document.querySelector('[data-sidebar]').inert);
    assert.equal(await menu.getAttribute('aria-expanded'), 'false');
    await page.locator('[data-assistant-toggle]').click();
    assert.equal(await page.locator('[data-assistant-panel]').getAttribute('aria-modal'), 'true');
    await page.keyboard.press('Escape');
    assert(await page.locator('[data-assistant-toggle]').evaluate(el => el === document.activeElement));
    await page.goto(base + '/play?lang=en');
    await page.locator('[data-start-bot]').click();
    await page.waitForURL('**/play?id=*');
    await page.waitForSelector('[data-board] .can-move');
    const gameMenu = page.locator('[data-sidebar-toggle]');
    await gameMenu.click();
    assert.equal(await gameMenu.getAttribute('aria-expanded'), 'true');
    assert.equal(await page.locator('[data-sidebar]').getAttribute('aria-modal'), 'true');
    await page.keyboard.press('Escape');
    assert(await gameMenu.evaluate(el => el === document.activeElement));
    assert.equal(await page.locator('[data-board] [role=row]').count(), 8);
    assert.equal(await page.locator('[data-board] [role=row] > [role=gridcell]').count(), 64);
    await page.locator('[data-resign-btn]').click();
    assert(await page.locator('[data-cancel-resign]').evaluate(el => el === document.activeElement));
    await page.keyboard.press('Shift+Tab');
    assert(await page.locator('[data-confirm-resign]').evaluate(el => el === document.activeElement));
    await page.keyboard.press('Tab');
    assert(await page.locator('[data-cancel-resign]').evaluate(el => el === document.activeElement));
    await page.keyboard.press('Escape');
    assert(await page.locator('[data-resign-btn]').evaluate(el => el === document.activeElement));
    await page.goto(base + '/play?lang=en');
    await page.locator('[data-start-bot]').click();
    await page.waitForURL('**/play?id=*');
    await page.goto(base + '/home?lang=en');
    await page.waitForSelector('[data-end-active-game]');
    await page.locator('[data-end-active-game]').first().click();
    assert(await page.locator('[data-cancel-end]').evaluate(el => el === document.activeElement));
    assert((await page.locator('[data-end-description]').innerText()).includes('opponent will win'));
    await page.keyboard.press('Escape');
    assert(await page.locator('[data-end-active-game]').first().evaluate(el => el === document.activeElement));
    assert.deepEqual(errors, []);
  } finally { await browser.close(); }
});

test('transient account check failure keeps the saved session and retry restores Home', { timeout: 60000 }, async () => {
  const browser = await launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
    await page.goto(base + '/play?lang=en');
    await page.locator('[data-start-bot]').click();
    await page.waitForURL('**/play?id=*');
    await page.route('**/api/auth/me', route => route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ errorCode: 'INTERNAL_ERROR' }) }));
    await page.goto(base + '/home?lang=en');
    await page.locator('[data-account-refresh-error]').waitFor({ state: 'visible' });
    assert(await page.evaluate(() => Checkers.Auth.isLoggedIn()));
    await page.locator('[data-end-active-game]').first().waitFor({ state: 'visible' });
    await page.unroute('**/api/auth/me');
    await page.locator('[data-account-refresh-retry]').click();
    await page.locator('[data-account-refresh-error]').waitFor({ state: 'hidden' });
    assert(await page.evaluate(() => Checkers.Auth.isLoggedIn()));
  } finally { await browser.close(); }
});

test('a rejected saved session clears cleanly instead of showing a retry banner', { timeout: 60000 }, async () => {
  const browser = await launch();
  try {
    const page = await browser.newPage();
    await page.goto(base + '/play?lang=en');
    await page.locator('[data-start-bot]').click();
    await page.waitForURL('**/play?id=*');
    await page.route('**/api/auth/me', route => route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ errorCode: 'UNAUTHORIZED' }) }));
    await page.route('**/api/auth/refresh', route => route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ errorCode: 'INVALID_REFRESH_TOKEN' }) }));
    await page.goto(base + '/home?lang=en');
    await page.waitForFunction(() => !Checkers.Auth.isLoggedIn());
    assert(!(await page.locator('[data-account-refresh-error]').isVisible()));
    assert(!(await page.locator('[data-active-games-box]').isVisible()));
    assert(await page.locator('.sidebar-auth').isVisible());
    assert(await page.locator('[data-find-random]').isVisible());
    await page.unroute('**/api/auth/me');
    await page.unroute('**/api/auth/refresh');
    await page.goto(base + '/play?lang=en');
    await page.locator('[data-start-bot]').click();
    await page.waitForURL('**/play?id=*');
  } finally { await browser.close(); }
});

test('forms explain errors and preserve values, including invitation Enter submission', { timeout: 60000 }, async () => {
  const browser = await launch();
  try {
    const context = await browser.newContext();
    const page = await context.newPage();
    await page.goto(base + '/register?lang=en');
    await page.locator('[type=submit]').click();
    assert.equal(await page.locator('#username').getAttribute('aria-invalid'), 'true');
    assert(await page.locator('#username').evaluate(el => el === document.activeElement));
    await page.locator('#username').fill('ab');
    await page.locator('#email').fill('not-an-email');
    await page.locator('#password').fill('short');
    await page.locator('[type=submit]').click();
    assert((await page.locator('#username-error').innerText()).includes('3 to 50'));
    assert((await page.locator('#email-error').innerText()).includes('valid email'));
    assert((await page.locator('#password-error').innerText()).includes('at least 8'));
    assert.equal(await page.locator('#email').inputValue(), 'not-an-email');
    await page.goto(base + '/play?lang=en');
    await page.locator('[data-join-code]').focus();
    await page.keyboard.press('Enter');
    assert.equal(await page.locator('[data-join-code]').getAttribute('aria-invalid'), 'true');
    assert(await page.locator('[data-join-error]').innerText());
    await page.locator('[data-join-code]').fill('INVALID123');
    await page.keyboard.press('Enter');
    await page.waitForFunction(() => !document.querySelector('[data-join-friend]').disabled);
    assert(await page.locator('[data-join-error]').innerText());
    assert.equal(await page.locator('[data-join-code]').inputValue(), 'INVALID123');
  } finally { await browser.close(); }
});

test('drawer backdrop dismisses and a correct puzzle turn announces real moves', { timeout: 60000 }, async () => {
  const browser = await launch();
  try {
    const context = await browser.newContext({ viewport: { width: 390, height: 844 } });
    const page = await context.newPage();
    await page.goto(base + '/tasks?lang=en');
    await page.locator('[data-sidebar-toggle]').click();
    await page.mouse.click(380, 400);
    await page.waitForFunction(() => document.querySelector('[data-sidebar-toggle]').getAttribute('aria-expanded') === 'false');
    await page.waitForSelector('[data-task-board] .can-move');
    const task = await page.evaluate(() => Checkers.DailyTask.load());
    let choice;
    for (const path of task.legalMoves) {
      const result = await page.evaluate(({ id, path }) => Checkers.api('/api/tasks/' + id + '/moves', { method:'POST', body: JSON.stringify({ moves:[path] }) }), { id:task.id, path });
      if (result.status === 'ACTIVE') { choice = path; break; }
    }
    assert(choice);
    await page.locator('[data-square=' + choice[0] + ']').focus();
    await page.keyboard.press('Enter');
    // Focus the actual destination and activate through the ordinary keyboard handler.
    await page.locator('[data-square=' + choice.at(-1) + ']').focus();
    await page.keyboard.press('Enter');
    await page.waitForFunction(() => document.querySelector('[data-task-announcement]').textContent.includes('Your move:'));
    assert((await page.locator('[data-task-announcement]').innerText()).includes('Black:'));
    assert(await page.locator('[data-square=' + choice.at(-1) + ']').evaluate(el => el === document.activeElement));
  } finally { await browser.close(); }
});

test('switching from a queued match cancels before creating an invitation', { timeout: 60000 }, async () => {
  const browser = await launch();
  try {
    const context = await browser.newContext();
    const page = await context.newPage();
    const requests = [];
    page.on('request', r => { if (r.url().includes('/api/games')) requests.push(r.method() + ' ' + new URL(r.url()).pathname); });
    await page.goto(base + '/play?lang=en');
    await page.locator('[data-find-random]').click();
    await page.waitForFunction(() => window._mmClient?.connected);
    assert(await page.locator('[data-cancel-random]').evaluate(el => el === document.activeElement));
    await page.locator('[data-create-friend]').click();
    await page.locator('[data-invite-box]:not(.hidden)').waitFor();
    assert(requests.indexOf('DELETE /api/games/matchmaking/leave') < requests.lastIndexOf('POST /api/games'));
    assert(await page.locator('[data-mm-search-state]').evaluate(el => el.classList.contains('hidden')));
    assert.equal(new URL(page.url()).pathname, '/play');
    await page.locator('[data-find-random]').click();
    await page.waitForFunction(() => window._mmClient?.connected);
    await page.route('**/api/games/matchmaking/leave', route => route.fulfill({ status:500, contentType:'application/json', body:JSON.stringify({ errorCode:'INTERNAL_ERROR' }) }));
    const before = requests.filter(x => x === 'POST /api/games').length;
    await page.locator('[data-create-friend]').click();
    await page.locator('.toast.error').waitFor();
    assert.equal(requests.filter(x => x === 'POST /api/games').length, before);
    assert(await page.locator('[data-mm-search-state]').isVisible());
    await page.unroute('**/api/games/matchmaking/leave');
    await page.locator('[data-cancel-random]').click();
    await page.waitForFunction(() => document.querySelector('[data-mm-search-state]').classList.contains('hidden'));
    assert(await page.locator('[data-find-random]').evaluate(el => el === document.activeElement));
  } finally { await browser.close(); }
});

test('home, play, puzzle and progress use distinct page layouts at wide and mobile sizes', { timeout: 60000 }, async () => {
  const browser = await launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
    await page.goto(base + '/home?lang=en');
    assert(await page.locator('.home-start').isVisible());
    assert.equal(await page.locator('.home-start').count(), 1);
    assert.equal(await page.locator('.home-hero-board .position-board').count(), 1);
    assert.equal(await page.locator('.home-hero-board .piece').count(), 24);
    assert((await page.locator('.home-heading').innerText()).includes('Russian checkers on an 8×8 board'));
    assert(await page.locator('[data-daily-task]').isVisible());
    assert.equal(await page.locator('link[rel=canonical]').getAttribute('href'), 'http://localhost:8080/?lang=en');

    await page.goto(base + '/play?lang=en');
    assert.equal(await page.locator('.home-start').count(), 0);
    assert((await page.locator('.play-launcher-heading').innerText()).toLowerCase().includes('russian checkers'));
    await page.locator('.position-board').waitFor({ state: 'visible' });
    const board = await page.locator('.position-board').boundingBox();
    assert(board.x >= 180 && board.x <= 212, `preview left edge was ${board.x}`);
    assert(board.width >= 640, `desktop preview was only ${board.width}px wide`);
    assert(await page.locator('#mode-random').isVisible());
    await page.goto(base + '/play?lang=en#mode-friend');
    await page.waitForFunction(() => document.activeElement?.id === 'mode-friend');

    for (const width of [390, 360]) {
      await page.setViewportSize({ width, height: 844 });
      await page.goto(base + '/home?lang=en');
      assert(await page.locator('.home-start').isVisible());
      assert(await page.locator('[data-find-random]').isVisible());
      assert(await page.locator('[data-daily-task]').isVisible());
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
      await page.goto(base + '/play?lang=en');
      assert(await page.locator('.play-launcher-options').isVisible());
      assert(!(await page.locator('.position-board').isVisible()));
      assert(await page.locator('[data-join-code]').isVisible());
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    }

    await page.setViewportSize({ width: 390, height: 844 });
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.goto(base + '/tasks?lang=en');
    const describedBy = (await page.locator('[data-task-board]').getAttribute('aria-describedby')).split(/\s+/);
    assert(describedBy.includes('task-objective'));
    assert(describedBy.includes('task-instructions'));
    assert(describedBy.includes('board-instructions'));
    const objective = await page.locator('.task-context').boundingBox();
    const puzzleBoard = await page.locator('.task-board-card').boundingBox();
    assert(objective.y < puzzleBoard.y);
    assert(puzzleBoard.width <= 560, `desktop puzzle board wrapper was ${puzzleBoard.width}px wide`);
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(base + '/rating?lang=en');
    assert(await page.locator('.progress-page').isVisible());
    assert.equal(await page.locator('.rating-grid').count(), 0);
    await page.waitForFunction(() => document.querySelector('[data-progress-content]').classList.contains('guest-mode'));
    assert(!(await page.locator('[data-rating-score]').isVisible()));
    await page.setViewportSize({ width: 844, height: 390 });
    await page.goto(base + '/play?lang=en');
    await page.locator('[data-start-bot]').click();
    await page.waitForURL('**/play?id=*');
    await page.waitForSelector('[data-board] .can-move');
    const landscapeBoard = await page.locator('.game-page .board-frame').boundingBox();
    assert(landscapeBoard.width >= 350, `landscape board compressed to ${landscapeBoard.width}px`);
    assert(landscapeBoard.y < 190, `landscape board started too low at ${landscapeBoard.y}px`);
    for (const player of await page.locator('.game-page .board-column > .player-card').all()) {
      const box = await player.boundingBox();
      assert.equal(box.width, landscapeBoard.width, 'landscape player bar and board should align');
      const nameLines = await player.locator('.player-meta strong').evaluate(el => {
        const range = document.createRange(); range.selectNodeContents(el); return range.getClientRects().length;
      });
      assert(nameLines <= 1, 'landscape player name should stay on one line');
    }
    await page.goto(base + '/history?lang=en');
    assert(await page.locator('[data-history-guest]').isVisible());
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
  } finally { await browser.close(); }
});

test('registered Home preserves active, start, puzzle, progress, history order on mobile', { timeout: 60000 }, async () => {
  const browser = await launch();
  try {
    const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
    await page.goto(base + '/register?lang=en');
    const tag = String(Date.now()).slice(-7);
    await page.locator('#username').fill('Order' + tag);
    await page.locator('#email').fill('order' + tag + '@example.test');
    await page.locator('#password').fill('Review_pass_2026');
    await page.locator('[type=submit]').click();
    await page.waitForURL(url => url.pathname === '/' || url.pathname === '/home');
    await page.goto(base + '/play?lang=en');
    await page.locator('[data-start-bot]').click();
    await page.waitForURL('**/play?id=*');
    await page.goto(base + '/home?lang=en');
    await page.locator('[data-end-active-game]').waitFor({ state: 'visible' });
    await page.locator('[data-personal-metrics]').waitFor({ state: 'visible' });
    const boxes = await page.evaluate(() => ['[data-active-games-box]', '.home-start', '[data-daily-task]', '[data-personal-metrics]', '[data-recent-games-box]']
      .map(selector => { const rect = document.querySelector(selector).getBoundingClientRect(); return { top: rect.top, width: rect.width }; }));
    const tops = boxes.map(box => box.top);
    assert(tops.every((top, index) => index === 0 || tops[index - 1] < top), `mobile Home order was ${tops.join(', ')}`);
    assert(boxes.every(box => box.width >= 320), `mobile Home column widths were ${boxes.map(box => box.width).join(', ')}`);
    assert(!(await page.locator('[data-personal-rate-row]').isVisible()));
    assert(await page.locator('[data-personal-empty]').isVisible());
    await page.locator('#main-content').focus();
    await page.keyboard.press('Tab');
    assert(await page.locator('[data-active-games-box] .btn-primary').evaluate(el => el === document.activeElement));
  } finally { await browser.close(); }
});
