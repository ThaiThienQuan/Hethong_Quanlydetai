const puppeteer = require('puppeteer-core');
const path = require('path');
const fs = require('fs');
const BASE = 'http://localhost:8080';
const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const OUT = path.join(__dirname, '..', '..', 'docs', 'ui', 'after');

(async () => {
  const browser = await puppeteer.launch({
    executablePath: CHROME, headless: 'new',
    args: ['--no-sandbox', '--disable-dev-shm-usage', '--hide-scrollbars'],
  });
  const page = await browser.newPage();
  const errs = [];
  page.on('console', (m) => { if (m.type() === 'error') errs.push(m.text()); });

  await page.setViewport({ width: 1366, height: 900 });
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle2' });
  await page.type('#login-username', 'admin');
  await page.type('#login-password', '123456');
  await Promise.all([page.waitForNavigation({ waitUntil: 'networkidle2' }), page.click('.login-submit')]);
  console.log('login url =', page.url());

  fs.mkdirSync(OUT, { recursive: true });
  for (const theme of ['light', 'dark']) {
    await page.goto(`${BASE}/ui/style-guide`, { waitUntil: 'networkidle2' });
    await page.evaluate((t) => window.setTheme && window.setTheme(t), theme);
    await new Promise((r) => setTimeout(r, 300));
    for (const vp of [{ n: '1366', w: 1366, h: 900 }, { n: '375', w: 375, h: 812, m: true }]) {
      await page.setViewport({ width: vp.w, height: vp.h, isMobile: !!vp.m, deviceScaleFactor: 1 });
      await new Promise((r) => setTimeout(r, 200));
      await page.screenshot({ path: path.join(OUT, `style-guide-${theme}-${vp.n}.png`), fullPage: true });
    }
    console.log('✓', theme);
  }
  console.log('console errors:', errs.length ? errs.join('\n') : 'none');
  await browser.close();
})();
