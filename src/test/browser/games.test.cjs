const { test } = require("node:test");
const assert = require("node:assert/strict");
const { chromium } = require("playwright");
const base = process.env.BASE_URL || "http://localhost:8080";

test(
  "normal games animate and stay in sync with the server, including premoves",
  { timeout: 90000 },
  async () => {
    const browser = await chromium.launch({
      executablePath:
        process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
      headless: true,
      args: ["--no-sandbox"],
    });
    try {
      const context = await browser.newContext({
        viewport: { width: 1440, height: 1000 },
      });
      const page = await context.newPage();
      const errors = [];
      page.on("pageerror", (error) => errors.push(error.message));
      const api = (path) => page.evaluate((path) => Checkers.api(path), path);
      await page.goto(base + "/register?lang=en");
      const username = "browser_" + Date.now();
      await page.locator("#username").fill(username);
      await page.locator("#email").fill(username + "@example.test");
      await page.locator("#password").fill("Browser_test_password_512");
      await page.locator("[type=submit]").click();
      await page.waitForURL(base + "/?lang=en");
      await page.waitForLoadState("networkidle");
      await page.locator("[data-bot-diff]").selectOption("EASY");
      await page.locator("[data-start-bot]").click();
      await page.waitForURL("**/play?id=*&lang=en");
      await page.waitForLoadState("networkidle");
      const id = new URL(page.url()).searchParams.get("id");
      for (let turn = 0; turn < 6; turn++) {
        await page.waitForFunction(
          () =>
            document.querySelectorAll("[data-board] .can-move").length > 0 &&
            !document.querySelector(".piece-motion"),
        );
        const paths = await api("/api/games/" + id + "/legal-moves");
        const path = paths[0];
        assert(path);
        if (turn === 1 && path.length === 2) {
          const from = await page
            .locator("[data-square=" + path[0] + "]")
            .boundingBox();
          const to = await page
            .locator("[data-square=" + path.at(-1) + "]")
            .boundingBox();
          await page.mouse.move(
            from.x + from.width / 2,
            from.y + from.height / 2,
          );
          await page.mouse.down();
          await page.mouse.move(to.x + to.width / 2, to.y + to.height / 2, {
            steps: 8,
          });
          await page.mouse.up();
        } else {
          await page.locator("[data-square=" + path[0] + "]").click();
          await page.locator("[data-square=" + path.at(-1) + "]").click();
        }
        await page.waitForFunction(
          async ({ id, count }) =>
            (await Checkers.api("/api/games/" + id)).moves.length >= count,
          { id, count: (turn + 1) * 2 },
        );
        await page.waitForFunction(
          () =>
            !document.querySelector(".piece-motion") &&
            document
              .querySelector("[data-game-status]")
              .textContent.startsWith(Checkers.t("play.yourTurn")),
        );
        const game = await api("/api/games/" + id);
        const squares = await page.evaluate(() =>
          Object.fromEntries(
            [...document.querySelectorAll("[data-board] .sq > .piece")].map(
              (piece) => [
                piece.parentElement.dataset.square,
                piece.dataset.code,
              ],
            ),
          ),
        );
        assert.deepEqual(squares, JSON.parse(game.boardState).squares);
      }
      await page.locator("[data-resign-btn]").click();
      await page.locator("[data-confirm-resign]").click();
      await page.waitForFunction(
        async (id) =>
          (await Checkers.api("/api/games/" + id)).status === "FINISHED",
        id,
      );
      await page.goto(base + "/history?lang=en");
      await page.waitForSelector("[data-history-body] tr");
      assert.equal(
        await page
          .locator("[data-history-body] tr")
          .first()
          .locator(".res-loss")
          .innerText(),
        "You lost",
      );

      await page.goto(base + "/?lang=en");
      await page.locator("[data-create-friend]").click();
      await page.waitForFunction(
        () =>
          document.querySelector("[data-invite-link]").textContent.length > 0,
      );
      const invite = await page.locator("[data-invite-link]").innerText();
      const other = await (await browser.newContext()).newPage();
      other.on("pageerror", (error) => errors.push(error.message));
      await other.goto(invite);
      await other.waitForLoadState("networkidle");
      const friendId = new URL(other.url()).searchParams.get("id");
      await page.goto(base + "/play?id=" + friendId + "&lang=en");
      await page.waitForLoadState("networkidle");
      // Black queues a legal premove while white is still thinking.
      await other.locator("[data-square=b6]").click();
      await other.locator("[data-square=a5]").click();
      assert(await other.locator(".premove-to").count());
      await page.locator("[data-square=a3]").click();
      await page.locator("[data-square=b4]").click();
      await page.waitForFunction(
        async (id) =>
          (await Checkers.api("/api/games/" + id)).moves.length === 2,
        friendId,
      );
      await other.waitForFunction(
        () =>
          !document.querySelector(".piece-motion") &&
          !document.querySelector(".premove-to") &&
          document.querySelector("[data-square=a5] .piece.black"),
      );
      assert(await other.locator("[data-square=a5] .piece.black").count());
      await page.locator("[data-draw-offer-btn]").click();
      await other.locator("[data-draw-offer]").waitFor({ state: "visible" });
      await other.locator("[data-draw-accept]").click();
      await page.waitForFunction(
        async (id) =>
          (await Checkers.api("/api/games/" + id)).status === "FINISHED",
        friendId,
      );
      assert.equal((await api("/api/games/" + friendId)).finishReason, "DRAW");
      assert.deepEqual(errors, []);
    } finally {
      await browser.close();
    }
  },
);
