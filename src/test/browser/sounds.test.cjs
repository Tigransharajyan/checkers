const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const base = process.env.BASE_URL || 'http://localhost:8080';
const launch = () => chromium.launch({
  executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
  headless: true, args: ['--no-sandbox'],
});

test('wooden board samples decode, stay short and do not clip', { timeout: 30000 }, async () => {
  const browser = await chromium.launch({
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    headless: true, args: ['--no-sandbox'],
  });
  try {
    const page = await browser.newPage();
    await page.goto(base + '/faq?lang=en');
    const clips = await page.evaluate(async () => {
      const decoder = new OfflineAudioContext(1, 44100, 44100);
      const results = [];
      for (const name of ['move-1', 'move-2', 'move-3', 'capture']) {
        const response = await fetch('/sounds/board/' + name + '.wav');
        if (!response.ok) throw new Error(name + ': HTTP ' + response.status);
        const decoded = await decoder.decodeAudioData(await response.arrayBuffer());
        const pcm = decoded.getChannelData(0);
        let peak = 0, energy = 0, tailPeak = 0;
        for (let i = 0; i < pcm.length; i++) {
          peak = Math.max(peak, Math.abs(pcm[i]));
          energy += pcm[i] * pcm[i];
          if (i >= pcm.length - 441) tailPeak = Math.max(tailPeak, Math.abs(pcm[i]));
        }
        results.push({ name, duration: decoded.duration, peak, rms: Math.sqrt(energy / pcm.length), tailPeak });
      }
      return results;
    });
    for (const clip of clips) {
      assert(clip.duration >= .1 && clip.duration <= .55, `${clip.name}: ${clip.duration}s`);
      assert(clip.peak > .15 && clip.peak < .8, `${clip.name}: peak ${clip.peak}`);
      assert(clip.rms > .005, `${clip.name} is silent`);
      assert(clip.tailPeak < .01, `${clip.name} has an abrupt ending`);
    }
  } finally { await browser.close(); }
});

async function soundBoard(page) {
  await page.goto(base + '/faq?lang=en');
  await page.addScriptTag({ url: base + '/js/board.js' });
  await page.evaluate(() => {
    const root = document.createElement('div');
    root.style.cssText = 'width:480px;height:480px;position:relative;margin:20px';
    document.querySelector('main').replaceChildren(root);
    window.soundRoot = root;
    window.landings = [];
    window.soundBoard = Checkers.BoardUI.create(root, {
      onMove: path => window.confirmSoundMove?.(path),
      onLand: kind => {
        const moving = root.querySelector('.piece-motion')?.getBoundingClientRect();
        const destination = moving && [...root.querySelectorAll('.sq')].find(square => {
          const rect = square.getBoundingClientRect();
          return Math.abs(rect.x + rect.width / 2 - moving.x - moving.width / 2) < 1
            && Math.abs(rect.y + rect.height / 2 - moving.y - moving.height / 2) < 1;
        });
        window.landings.push({ kind, time: performance.now(), square: destination?.dataset.square });
      },
    });
    window.captureStart = { turn: 'WHITE', squares: { a1: 'WM', b2: 'BM', d4: 'BM', f6: 'BM', h8: 'BM' } };
    window.captureEnd = { turn: 'BLACK', squares: { g7: 'WM', h8: 'BM' } };
    window.capturePath = ['a1', 'c3', 'e5', 'g7'];
  });
}

test('each capture landing sounds once, at its square; duplicates and quiet kings stay correct', { timeout: 30000 }, async () => {
  const browser = await launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
    await soundBoard(page);
    const result = await page.evaluate(async () => {
      await soundBoard.setBoard(captureStart);
      if (landings.length) throw new Error('Loading a board produced a sound');
      await soundBoard.setBoard(captureEnd, { path: capturePath });
      const capture = landings.slice();
      await soundBoard.setBoard(captureEnd, { path: capturePath });
      if (landings.length !== capture.length) throw new Error('Duplicate confirmation replayed capture sounds');
      landings = [];
      await soundBoard.setBoard({ turn: 'WHITE', squares: { a1: 'WK', h8: 'BM' } });
      await soundBoard.setBoard({ turn: 'BLACK', squares: { g7: 'WK', h8: 'BM' } }, { path: ['a1', 'g7'] });
      const king = landings.slice();
      landings = [];
      await soundBoard.setBoard({ turn: 'WHITE', squares: { c3: 'WM', d4: 'BM', d6: 'BM', b6: 'BM', b4: 'BM', h8: 'BM' } });
      await soundBoard.setBoard({ turn: 'BLACK', squares: { c3: 'WM', h8: 'BM' } }, { path: ['c3', 'e5', 'c7', 'a5', 'c3'] });
      return { capture, king, cycle: landings.slice() };
    });
    assert.deepEqual(result.capture.map(event => event.kind), ['capture', 'capture', 'capture']);
    assert.deepEqual(result.capture.map(event => event.square), ['c3', 'e5', 'g7']);
    for (let index = 1; index < result.capture.length; index++) {
      assert(result.capture[index].time - result.capture[index - 1].time >= 140, 'Contacts collapsed into one burst');
    }
    assert.deepEqual(result.king.map(event => event.kind), ['move']);
    assert.deepEqual(result.cycle.map(event => event.kind), ['capture', 'capture', 'capture', 'capture']);
    assert.deepEqual(result.cycle.map(event => event.square), ['e5', 'c7', 'a5', 'c3']);
  } finally { await browser.close(); }
});

test('local click and drag captures share landing sounds with server confirmation', { timeout: 30000 }, async () => {
  const browser = await launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
    await soundBoard(page);
    async function prepare() {
      await page.evaluate(async () => {
        landings = [];
        await soundBoard.setBoard(captureStart);
        soundBoard.setLegal([capturePath]);
        window.confirmSoundMove = path => {
          // Confirm while the optimistic animation is still running.
          setTimeout(() => { window.confirmedSoundMove = soundBoard.setBoard(captureEnd, { path }); }, 20);
          return true;
        };
      });
    }
    await prepare();
    await page.locator('[data-square=a1]').click();
    await page.locator('[data-square=g7]').click();
    await page.waitForFunction(() => soundBoard.getBoard().squares.g7 === 'WM');
    assert.deepEqual(await page.evaluate(() => landings.map(event => event.kind)), ['capture', 'capture', 'capture']);

    await prepare();
    const from = await page.locator('[data-square=a1]').boundingBox();
    const to = await page.locator('[data-square=g7]').boundingBox();
    await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2);
    await page.mouse.down();
    await page.mouse.move(to.x + to.width / 2, to.y + to.height / 2, { steps: 12 });
    await page.mouse.up();
    await page.waitForFunction(() => soundBoard.getBoard().squares.g7 === 'WM');
    assert.deepEqual(await page.evaluate(() => landings.map(event => event.square)), ['c3', 'e5', 'g7']);
    assert.deepEqual(await page.evaluate(() => landings.map(event => event.kind)), ['capture', 'capture', 'capture']);
  } finally { await browser.close(); }
});

test('reduced motion preserves separate contacts and cancellation stops the remaining sounds', { timeout: 30000 }, async () => {
  const browser = await launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 900 }, reducedMotion: 'reduce' });
    await soundBoard(page);
    const sequence = await page.evaluate(async () => {
      await soundBoard.setBoard(captureStart);
      await soundBoard.setBoard(captureEnd, { path: capturePath });
      if (soundRoot.querySelector('.piece-motion')) throw new Error('Reduced motion created an animation');
      return landings.slice();
    });
    assert.deepEqual(sequence.map(event => event.kind), ['capture', 'capture', 'capture']);
    assert(sequence[1].time - sequence[0].time >= 140);
    assert(sequence[2].time - sequence[1].time >= 140);
    const cancelled = await page.evaluate(async () => {
      landings = [];
      await soundBoard.setBoard(captureStart);
      const update = soundBoard.setBoard(captureEnd, { path: capturePath });
      soundBoard.cancelPendingMove();
      await update;
      return landings.map(event => event.kind);
    });
    assert.deepEqual(cancelled, ['capture']);
    const hidden = await page.evaluate(async () => {
      landings = [];
      await soundBoard.setBoard(captureStart);
      Object.defineProperty(document, 'visibilityState', { configurable: true, value: 'hidden' });
      try { await soundBoard.setBoard(captureEnd, { path: capturePath }); }
      finally { delete document.visibilityState; }
      return landings.length;
    });
    assert.equal(hidden, 0);
  } finally { await browser.close(); }
});
