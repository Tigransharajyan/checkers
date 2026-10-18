const { test } = require("node:test");
const base = process.env.BASE_URL || "http://localhost:8080";
const { chromium } = require("playwright");
const assert = require("node:assert/strict");
test(
  "daily puzzles use server rules, preserve progress and fit all languages",
  { timeout: 90000 },
  async () => {
    const b = await chromium.launch({
      executablePath:
        process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
      headless: true,
      args: ["--no-sandbox"],
    });
    try {
      const p = await b.newPage({ viewport: { width: 1440, height: 1000 } }),
        errors = [];
      p.on("pageerror", (e) => errors.push(e.message));
      await p.goto(base + "/tasks?lang=ru");
      await p.waitForFunction(
        () => document.querySelectorAll("[data-task-board] .sq").length === 64,
      );
      console.log(
        "initial",
        await p.locator("[data-task-instructions]").innerText(),
      );
      const initial = await p.evaluate(() => Checkers.DailyTask.load());
      let current = initial;
      let wrong;
      for (const path of current.legalMoves) {
        const v = await p.evaluate(
          ({ id, path }) =>
            Checkers.api("/api/tasks/" + id + "/moves", {
              method: "POST",
              body: JSON.stringify({ moves: [path] }),
            }),
          { id: current.id, path },
        );
        if (v.status === "INCORRECT") {
          wrong = path;
          break;
        }
      }
      await p.locator("[data-square=" + wrong[0] + "]").click();
      await p.locator("[data-square=" + wrong.at(-1) + "]").click();
      await p.waitForFunction(() =>
        document
          .querySelector("[data-task-feedback]")
          .classList.contains("bad"),
      );
      assert.equal(
        await p.locator("[data-task-progress]").innerText(),
        "Ходы: 0 / " + initial.maxPlayerMoves,
      );
      console.log(
        "PASS legal but wrong plan is rejected without resetting the position",
      );
      let sawCapture = false;
      while (current.status !== "SOLVED") {
        let choice, response;
        for (const path of current.legalMoves) {
          const v = await p.evaluate(
            ({ id, moves }) =>
              Checkers.api("/api/tasks/" + id + "/moves", {
                method: "POST",
                body: JSON.stringify({ moves }),
              }),
            { id: current.id, moves: [...current.playerMoves, path] },
          );
          if (v.status !== "INCORRECT") {
            choice = path;
            response = v;
            break;
          }
        }
        assert(choice);
        await p.locator("[data-square=e1]").evaluate((el) => {
          window.qaCell = el;
          return true;
        });
        await p.locator("[data-square=" + choice[0] + "]").click();
        // Select intermediate landings when multiple king routes have the same end square.
        for (const square of choice.slice(1))
          await p.locator("[data-square=" + square + "]").click();
        await p.waitForFunction(
          (n) =>
            document
              .querySelector("[data-task-progress]")
              .textContent.startsWith("Ходы: " + n + " /"),
          response.movesPlayed,
        );
        assert(
          await p
            .locator("[data-square=e1]")
            .evaluate((el) => el === window.qaCell),
        );
        for (const step of response.events) {
          sawCapture ||= step.capturedSquares.length > 0;
        }
        current = response;
        console.log(
          "PASS white turn",
          current.movesPlayed,
          "with real engine defence",
        );
      }
      assert(
        await p
          .locator("[data-task-feedback]")
          .evaluate((el) => el.classList.contains("ok")),
      );
      assert(sawCapture);
      await p.reload();
      await p.waitForFunction(() =>
        document.querySelector("[data-task-feedback]").classList.contains("ok"),
      );
      console.log("PASS completed solution is revalidated on reload");
      for (const lang of ["en", "ru", "hy"])
        for (const width of [320, 390, 768, 1280, 1774]) {
          await p.setViewportSize({ width, height: 900 });
          await p.goto(base + "/tasks?lang=" + lang);
          await p.waitForFunction(
            () =>
              document.querySelectorAll("[data-task-board] .sq").length === 64,
          );
          assert(
            await p.evaluate(
              () => document.documentElement.scrollWidth <= innerWidth,
            ),
            lang + " " + width,
          );
        }
      console.log("PASS all languages and task viewport sizes");
      await p.setViewportSize({ width: 1440, height: 1000 });
      await p.goto(base + "/tasks?lang=ru");
      await p.waitForFunction(
        () => document.querySelectorAll("[data-task-board] .sq").length === 64,
      );
      await p.locator("[data-reset-task]").click();
      await p.waitForFunction(() =>
        document
          .querySelector("[data-task-progress]")
          .textContent.startsWith("Ходы: 0 /"),
      );
      assert.deepEqual(errors, []);
      console.log("PASS no runtime errors");
    } finally {
      await b.close();
    }
  },
);
