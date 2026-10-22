/**
 * Chụp ảnh giao diện UniResearch ở nhiều theme × nhiều độ rộng.
 * Dùng: node tools/shots/shoot.js <outDir> [user] [pass]
 */
const puppeteer = require('puppeteer-core');
const fs = require('fs');
const path = require('path');

const BASE = 'http://localhost:8080';
const OUT = process.argv[2] || path.join(__dirname, 'out');
const USER = process.argv[3] || 'truongkhoa';
const PASS = process.argv[4] || '123456';
// /roles và /admin/users chỉ mở cho ADMIN (SecurityConfig: hasAuthority USER_MANAGE /
// ROLE_MANAGE, bảng role_permissions chỉ gán 2 quyền này cho ADMIN) → phải chụp bằng
// phiên đăng nhập riêng, không đổi phân quyền để "cho vừa" tài khoản trưởng khoa.
const ADMIN_USER = process.argv[5] || 'admin';
const ADMIN_PASS = process.argv[6] || '123456';
const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

const VIEWPORTS = [
  { name: '375', width: 375, height: 812, mobile: true },
  { name: '768', width: 768, height: 1024, mobile: true },
  { name: '1366', width: 1366, height: 900, mobile: false },
  { name: '1920', width: 1920, height: 1080, mobile: false },
];

// slug -> path
const PAGES = [
  ['dang-nhap', '/login'],
  ['tong-quan', '/'],
  ['de-tai', '/topics'],
  ['dot-dang-ky', '/periods'],
  ['duyet-de-tai', '/head/topics'],
  ['hoi-dong', '/head/councils'],
  ['ket-qua-cong-bo', '/head/results'],
  ['nhat-ky-email', '/head/emails'],
  ['thong-bao', '/announcements'],
  ['bo-mon', '/admin/departments'],
  ['ho-so-gv', '/admin/profiles/lecturers'],
  ['tai-khoan', '/account/profile'],
  ['doi-mat-khau', '/account/password'],
];

// Các màn hình chỉ ADMIN mới vào được - chụp bằng phiên đăng nhập admin.
const ADMIN_PAGES = [
  ['phan-quyen', '/roles'],
  ['quan-ly-tai-khoan', '/admin/users'],
  ['style-guide', '/ui/style-guide'],
];

const slugify = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');

(async () => {
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: 'new',
    args: ['--no-sandbox', '--disable-dev-shm-usage', '--hide-scrollbars'],
  });
  fs.mkdirSync(OUT, { recursive: true });

  const consoleErrors = [];
  const failedRequests = [];

  // Mỗi vai trò dùng một browser context riêng (cookie tách biệt) để có thể
  // đăng nhập nhiều tài khoản trong cùng một lần chạy.
  const makePage = async (ctx) => {
    const p = await ctx.newPage();
    p.on('console', (m) => {
      if (m.type() === 'error') consoleErrors.push(`${p.url()} :: ${m.text()}`);
    });
    p.on('requestfailed', (r) => failedRequests.push(`${r.url()} :: ${r.failure() && r.failure().errorText}`));
    p.on('response', (r) => {
      if (r.status() >= 400) failedRequests.push(`${r.status()} ${r.url()}`);
    });
    return p;
  };

  const mainCtx = await browser.createBrowserContext();
  const page = await makePage(mainCtx);

  // --- Đăng nhập ---
  const login = async (pg, user, pass) => {
    await pg.setViewport({ width: 1366, height: 900 });
    await pg.goto(`${BASE}/login`, { waitUntil: 'networkidle2' });
    await pg.type('#login-username', user);
    await pg.type('#login-password', pass);
    await Promise.all([
      pg.waitForNavigation({ waitUntil: 'networkidle2' }),
      pg.click('.login-submit'),
    ]);
    return !pg.url().includes('/login');
  };

  const loggedIn = await login(page, USER, PASS);
  console.log(`Đăng nhập (${USER}):`, loggedIn ? 'OK' : 'THẤT BẠI (' + page.url() + ')');
  if (!loggedIn) { await browser.close(); process.exit(1); }

  // Chụp một danh sách trang ở mọi theme × bề rộng, dùng phiên của trang pg.
  const shootPages = async (pg, pages) => {
    for (const theme of ['light', 'dark']) {
      // đặt theme qua API nội bộ của theme.js
      await pg.evaluate((t) => window.setTheme && window.setTheme(t), theme);
      for (const vp of VIEWPORTS) {
        await pg.setViewport({
          width: vp.width, height: vp.height, isMobile: vp.mobile, deviceScaleFactor: 1,
        });
        for (const [name, url] of pages) {
          const slug = slugify(name);
          const resp = await pg.goto(BASE + url, { waitUntil: 'networkidle2' });
          const status = resp ? resp.status() : 0;
          if (status >= 400) { console.log(`  ${status} ${url} (${theme}/${vp.name})`); continue; }
          await pg.evaluate(() => new Promise((r) => requestAnimationFrame(() => r())));
          const file = path.join(OUT, `${slug}-${theme}-${vp.name}.png`);
          await pg.screenshot({ path: file, fullPage: true });
        }
        console.log(`✓ ${theme} @ ${vp.name}px`);
      }
    }
  };

  await shootPages(page, PAGES);

  // --- Phiên admin cho các màn hình /roles, /admin/users, /ui/style-guide ---
  // Context riêng: cookie phiên trưởng khoa không "lấn" phiên admin.
  const adminCtx = await browser.createBrowserContext();
  const adminPage = await makePage(adminCtx);
  const adminLoggedIn = await login(adminPage, ADMIN_USER, ADMIN_PASS);
  console.log(`\nĐăng nhập (${ADMIN_USER}):`, adminLoggedIn ? 'OK' : 'THẤT BẠI (' + adminPage.url() + ')');
  if (adminLoggedIn) {
    await shootPages(adminPage, ADMIN_PAGES);
  } else {
    console.log(`BỎ QUA ${ADMIN_PAGES.length} màn hình admin-only - đăng nhập thất bại.`);
  }

  // --- Kiểm tra tràn ngang ---
  await page.setViewport({ width: 375, height: 812, isMobile: true, deviceScaleFactor: 1 });
  const overflow = [];
  for (const [name, url] of PAGES) {
    await page.goto(BASE + url, { waitUntil: 'networkidle2' });
    const r = await page.evaluate(() => ({
      sw: document.documentElement.scrollWidth,
      cw: document.documentElement.clientWidth,
    }));
    if (r.sw > r.cw + 1) overflow.push(`${url}: scrollW=${r.sw} > clientW=${r.cw}`);
  }
  if (adminLoggedIn) {
    await adminPage.setViewport({ width: 375, height: 812, isMobile: true, deviceScaleFactor: 1 });
    for (const [name, url] of ADMIN_PAGES) {
      await adminPage.goto(BASE + url, { waitUntil: 'networkidle2' });
      const r = await adminPage.evaluate(() => ({
        sw: document.documentElement.scrollWidth,
        cw: document.documentElement.clientWidth,
      }));
      if (r.sw > r.cw + 1) overflow.push(`${url}: scrollW=${r.sw} > clientW=${r.cw}`);
    }
  }

  console.log('\n--- Tràn ngang ở 375px ---');
  console.log(overflow.length ? overflow.join('\n') : 'Không có trang nào tràn ngang.');
  console.log('\n--- Console errors ---');
  console.log(consoleErrors.length ? [...new Set(consoleErrors)].join('\n') : 'Sạch.');
  console.log('\n--- Request lỗi / 4xx-5xx ---');
  console.log(failedRequests.length ? [...new Set(failedRequests)].join('\n') : 'Không có.');

  await browser.close();
})();
