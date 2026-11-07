const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const base = process.env.BASE_URL || 'http://localhost:8080';
const launch = () => chromium.launch({ executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined, headless: true, args: ['--no-sandbox'] });

test('multiple premoves project future pieces, combine capture jumps and cancel safely', { timeout: 30000 }, async () => {
  const browser = await launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
    await page.goto(base + '/faq?lang=en'); await page.addScriptTag({ url: base + '/js/board.js' });
    await page.evaluate(async () => {
      const root = document.createElement('div'); root.id = 'queue-board'; root.style.cssText = 'width:480px;height:480px;position:relative;margin:20px';
      document.querySelector('main').replaceChildren(root);
      window.queueBoard = Checkers.BoardUI.create(root);
      await queueBoard.setBoard({ turn: 'WHITE', squares: { h6: 'BM', g7: 'BM', a3: 'WM' } });
      queueBoard.setPremoveMode(true, 'BLACK');
    });
    for (const square of ['h6', 'g5', 'g5', 'h4', 'g7', 'h6']) await page.locator('[data-square=' + square + ']').click();
    assert.deepEqual(await page.evaluate(() => queueBoard.getPremoves()), [['h6', 'g5'], ['g5', 'h4'], ['g7', 'h6']]);
    assert(await page.locator('[data-square=h4] .piece.black').count());
    assert.equal(await page.locator('[data-square=g5] .piece').count(), 0);
    assert.deepEqual(await page.evaluate(() => queueBoard.getBoard().squares), { h6: 'BM', g7: 'BM', a3: 'WM' }, 'Queued moves changed the authoritative board');
    await page.locator('[data-square=h4]').click({ button: 'right' });
    assert.deepEqual(await page.evaluate(() => queueBoard.getPremoves()), []);
    assert.equal(await page.locator('.board-circle').count(), 0, 'Cancelling a queue must not also add an annotation');
    assert(await page.locator('[data-square=h6] .piece.black').count());

    const matches = await page.evaluate(async () => {
      const legal = [['a1', 'c3', 'e5', 'g7']];
      const result = Checkers.BoardUI.matchPremoveQueue(legal, [['a1', 'c3'], ['c3', 'e5'], ['e5', 'g7'], ['g7', 'h8']]);
      const invalid = Checkers.BoardUI.matchPremoveQueue(legal, [['a1', 'b2'], ['b2', 'g7']]);
      await queueBoard.setBoard({ turn: 'WHITE', squares: { c3: 'WM', d4: 'BM', f6: 'BM', h8: 'BM' } });
      queueBoard.setPremoveMode(true, 'BLACK');
      queueBoard.enqueuePremove(['d4', 'c3']); queueBoard.enqueuePremove(['c3', 'b2']);
      await queueBoard.setBoard({ turn: 'BLACK', squares: { g7: 'WM', h8: 'BM' } }, { path: ['c3', 'e5', 'g7'] });
      return { result, invalid, cancelled: queueBoard.getPremoves() };
    });
    assert.deepEqual(matches.result, { path: ['a1', 'c3', 'e5', 'g7'], count: 3 });
    assert.equal(matches.invalid, null);
    assert.deepEqual(matches.cancelled, []);
    await page.evaluate(() => { queueBoard.setPremoveMode(true, 'BLACK'); queueBoard.enqueuePremove(['h8', 'f6']); });
    await page.locator('[data-square=h8]').focus(); await page.keyboard.press('Escape');
    assert.deepEqual(await page.evaluate(() => queueBoard.getPremoves()), []);
  } finally { await browser.close(); }
});

test('automatic bot draws display as draws, including the saved history', { timeout: 30000 }, async () => {
  const browser = await launch();
  try {
    const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
    await page.goto(base + '/register?lang=en');
    const username = 'draw_ui_' + Date.now();
    await page.locator('#username').fill(username); await page.locator('#email').fill(username + '@example.test');
    await page.locator('#password').fill('Draw_browser_password_2026'); await page.locator('[type=submit]').click();
    await page.waitForURL(base + '/?lang=en');
    await page.goto(base + '/play?lang=en'); await page.locator('[data-start-bot]').click();
    await page.waitForURL('**/play?id=*');
    const id = new URL(page.url()).searchParams.get('id');
    await page.route('**/api/games/' + id, async route => {
      const response = await route.fetch(); const game = await response.json();
      const board = JSON.parse(game.boardState); board.outcome = 'DRAW';
      await route.fulfill({ json: { ...game, status: 'FINISHED', winnerId: null, currentTurn: null, finishReason: 'DRAW_REPETITION', boardState: JSON.stringify(board) } });
    });
    await page.reload();
    await page.locator('[data-gameover-modal][data-result=draw]').waitFor({ state: 'visible' });
    assert.equal(await page.locator('[data-gameover-title]').innerText(), 'Draw');
    assert.equal(await page.locator('[data-result-you-score]').innerText(), '½');
    assert((await page.locator('[data-gameover-reason]').innerText()).includes('three times'));
    await page.route('**/api/games/history*', route => route.fulfill({ json: { content: [{ id: Number(id), mode: 'BOT', status: 'FINISHED', whiteUsername: username, blackUsername: 'BOT', winnerUsername: null, finishReason: 'DRAW_REPETITION', createdAt: new Date().toISOString() }] } }));
    await page.goto(base + '/history?lang=en'); await page.locator('[data-history-body] .res-draw').waitFor();
    assert.equal(await page.locator('[data-history-body] .res-loss').count(), 0);
  } finally { await browser.close(); }
});

test('three queued online moves execute in order across three opponent turns', { timeout: 60000 }, async () => {
  const browser = await launch();
  try {
    const white = await (await browser.newContext({ viewport: { width: 1440, height: 1000 } })).newPage();
    const black = await (await browser.newContext({ viewport: { width: 1440, height: 1000 } })).newPage();
    const errors = [];
    for (const page of [white, black]) page.on('pageerror', error => errors.push(error.message));
    await white.goto(base + '/play?lang=en'); await white.locator('[data-create-friend]').click();
    await white.waitForFunction(() => document.querySelector('[data-invite-link]').textContent.length > 0);
    await black.goto(await white.locator('[data-invite-link]').innerText());
    await black.locator('[data-board] .piece').first().waitFor();
    const id = new URL(black.url()).searchParams.get('id');
    await white.goto(base + '/play?id=' + id + '&lang=en');
    await white.locator('[data-board] .can-move').first().waitFor();
    for (const square of ['h6', 'g5', 'g5', 'h4', 'g7', 'h6']) await black.locator('[data-square=' + square + ']').click();
    assert((await black.locator('[data-game-status]').innerText()).includes('Queued moves: 3'));
    const whiteMoves = [['a3', 'b4'], ['b2', 'a3'], ['a1', 'b2']];
    for (let index = 0; index < whiteMoves.length; index++) {
      await white.waitForFunction(() => document.querySelector('[data-board] .can-move') && !document.querySelector('.piece-motion'));
      await white.locator('[data-square=' + whiteMoves[index][0] + ']').click();
      await white.locator('[data-square=' + whiteMoves[index][1] + ']').click();
      await white.waitForFunction(count => Number(document.querySelector('[data-move-count]').textContent) >= count, (index + 1) * 2);
      await black.waitForFunction(count => Number(document.querySelector('[data-move-count]').textContent) >= count, (index + 1) * 2);
    }
    const game = await white.evaluate(id => Checkers.api('/api/games/' + id), id);
    assert.deepEqual(game.moves.filter((_, index) => index % 2).map(move => move.path), ['h6,g5', 'g5,h4', 'g7,h6']);
    assert.equal(game.moves.length, 6);
    assert(!(await black.locator('[data-game-status]').innerText()).includes('Queued moves:'));
    assert.deepEqual(errors, []);
  } finally { await browser.close(); }
});
