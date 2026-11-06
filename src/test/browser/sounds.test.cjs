const { test } = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const base = process.env.BASE_URL || 'http://localhost:8080';

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
      for (const name of ['move-1', 'move-2', 'move-3', 'capture', 'start', 'end']) {
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
