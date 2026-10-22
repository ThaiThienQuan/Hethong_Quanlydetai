const puppeteer = require('puppeteer-core');
const BASE = 'http://localhost:8080';
const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

(async () => {
  const browser = await puppeteer.launch({
    executablePath: CHROME, headless: 'new',
    args: ['--no-sandbox', '--disable-dev-shm-usage', '--hide-scrollbars'],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 375, height: 812, isMobile: true, deviceScaleFactor: 1 });
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle2' });
  await page.type('#login-username', 'truongkhoa');
  await page.type('#login-password', '123456');
  await Promise.all([page.waitForNavigation({ waitUntil: 'networkidle2' }), page.click('.login-submit')]);

  const url = process.argv[2] || '/head/emails';
  await page.goto(BASE + url, { waitUntil: 'networkidle2' });
  const info = await page.evaluate(() => {
    const cw = document.documentElement.clientWidth;
    const out = [];
    document.querySelectorAll('body *').forEach((el) => {
      if (el.closest('.app-sidebar')) return;
      const cs = getComputedStyle(el);
      const r = el.getBoundingClientRect();
      if (r.right > cw + 1 && cs.position !== 'fixed') {
        out.push({
          tag: el.tagName.toLowerCase(),
          cls: String(el.className || '').slice(0, 50),
          l: Math.round(r.left), r: Math.round(r.right), w: Math.round(r.width),
          pos: cs.position, ovf: cs.overflowX,
          parent: el.parentElement ? el.parentElement.tagName.toLowerCase() + '.' + String(el.parentElement.className || '').slice(0, 30) : '',
          pOv: el.parentElement ? getComputedStyle(el.parentElement).overflowX : '',
        });
      }
    });
    return { cw, scrollW: document.documentElement.scrollWidth, out: out.slice(0, 30) };
  });
  console.log('clientW=' + info.cw + ' scrollW=' + info.scrollW);
  info.out.forEach((b) => console.log(' ', JSON.stringify(b)));
  await browser.close();
})();
