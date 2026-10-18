const { test } = require("node:test");
const base = process.env.BASE_URL || "http://localhost:8080";
const { chromium } = require("playwright");
const assert = require("node:assert/strict");
test(
  "board animations preserve DOM, paths, drag position and rapid updates",
  { timeout: 90000 },
  async () => {
    const b = await chromium.launch({
      executablePath:
        process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
      headless: true,
      args: ["--no-sandbox"],
    });
    try {
      const p = await b.newPage({ viewport: { width: 1280, height: 1000 } });
      const errors = [];
      p.on("pageerror", (e) => errors.push(e.message));
      await p.goto(base + "/faq?lang=ru");
      await p.addScriptTag({ url: base + "/js/board.js" });
      await p.evaluate(() => {
        const root = document.createElement("div");
        root.id = "qa-board";
        root.style.cssText =
          "width:480px;height:480px;margin:20px;position:relative";
        document.querySelector("main").replaceChildren(root);
        window.qaRoot = root;
        window.animationsLog = [];
        const native = Element.prototype.animate;
        Element.prototype.animate = function (frames, options) {
          if (this.classList.contains("piece-motion"))
            window.animationsLog.push(frames);
          return native.call(this, frames, options);
        };
        window.ui = Checkers.BoardUI.create(root);
      });
      const result = await p.evaluate(async () => {
        const board = (turn, squares) => ({
          turn,
          squares,
          outcome: "IN_PROGRESS",
        });
        await ui.setBoard(
          board("WHITE", { c3: "WM", d4: "BM", f6: "BM", h8: "BM" }),
        );
        const cell = qaRoot.querySelector("[data-square=a1]");
        ui.setLegal([["c3", "e5", "g7"]]);
        ui.setFlip(false);
        if (cell !== qaRoot.querySelector("[data-square=a1]"))
          throw new Error("Cells replaced");
        await ui.setBoard(board("BLACK", { g7: "WM", h8: "BM" }), {
          path: ["c3", "e5", "g7"],
        });
        if (animationsLog.length !== 2)
          throw new Error("Jump path was not animated segment by segment");
        const captureFrames = animationsLog.slice();
        animationsLog = [];
        await ui.setBoard(board("WHITE", { a1: "WM", h8: "BM" }));
        const next = board("BLACK", { b2: "WM", h8: "BM" }),
          last = board("WHITE", { b2: "WM", g7: "BM" });
        await Promise.all([
          ui.setBoard(next, { path: ["a1", "b2"] }),
          ui.setBoard(next, { path: ["a1", "b2"] }),
          ui.setBoard(last, { path: ["h8", "g7"] }),
        ]);
        if (animationsLog.length !== 2)
          throw new Error("Duplicate/reordered animation");
        if (ui.getBoard().squares.g7 !== "BM")
          throw new Error("Lost rapid response");
        animationsLog = [];
        await ui.setBoard(
          board("WHITE", {
            c3: "WM",
            d4: "BM",
            d6: "BM",
            b6: "BM",
            b4: "BM",
            h8: "BM",
          }),
        );
        await ui.setBoard(board("BLACK", { c3: "WM", h8: "BM" }), {
          path: ["c3", "e5", "c7", "a5", "c3"],
        });
        if (animationsLog.length !== 4)
          throw new Error("Cycle capture collapsed");
        await ui.setBoard(board("WHITE", { c7: "WM", h8: "BM" }));
        await ui.setBoard(board("BLACK", { b8: "WK", h8: "BM" }), {
          path: ["c7", "b8"],
        });
        if (!qaRoot.querySelector("[data-square=b8] .piece.king"))
          throw new Error("Promotion lost");
        if (
          Checkers.BoardUI.isCapturePath(
            ["a1", "f6"],
            board("WHITE", { a1: "WK", h8: "BM" }),
          )
        )
          throw new Error("Quiet king move shown as capture");
        if (
          !Checkers.BoardUI.isCapturePath(
            ["a1", "f6"],
            board("WHITE", { a1: "WK", d4: "BM" }),
          )
        )
          throw new Error("King capture not recognized");
        return {
          captureFrames: captureFrames.length,
          queuedMoves: 2,
          cycleLegs: 4,
        };
      });
      console.log(
        "PASS persistent DOM, capture path, queue, duplicates, loop captures, promotion, king hints",
        result,
      );
      // Use a fresh root so only one input handler owns it.
      await p.reload();
      await p.addScriptTag({ url: base + "/js/board.js" });
      await p.evaluate(async () => {
        const root = document.createElement("div");
        root.id = "qa-board";
        root.style.cssText =
          "width:480px;height:480px;margin:20px;position:relative";
        document.querySelector("main").replaceChildren(root);
        window.qaRoot = root;
        window.moveCount = 0;
        window.ui = Checkers.BoardUI.create(root, {
          onMove: (path) => {
            window.moveCount++;
            setTimeout(
              () =>
                ui.setBoard(
                  { turn: "BLACK", squares: { b2: "WM", h8: "BM" } },
                  { path },
                ),
              200,
            );
          },
        });
        await ui.setBoard({ turn: "WHITE", squares: { a1: "WM", h8: "BM" } });
        ui.setLegal([["a1", "b2"]]);
      });
      const from = await p.locator("[data-square=a1]").boundingBox(),
        to = await p.locator("[data-square=b2]").boundingBox();
      await p.mouse.move(from.x + from.width / 2, from.y + from.height / 2);
      await p.mouse.down();
      await p.mouse.move(to.x + to.width / 2, to.y + to.height / 2, {
        steps: 12,
      });
      await p.mouse.up();
      const drop = await p.evaluate(() => {
        const g = qaRoot.querySelector(".piece-motion"),
          s = qaRoot.querySelector("[data-square=b2]");
        if (!g) throw new Error("No retained drop visual");
        const a = g.getBoundingClientRect(),
          b = s.getBoundingClientRect();
        return Math.hypot(
          a.x + a.width / 2 - b.x - b.width / 2,
          a.y + a.height / 2 - b.y - b.height / 2,
        );
      });
      assert(drop < 5, "Drop snapped back " + drop);
      await p.waitForFunction(() => ui.getBoard().squares.b2 === "WM");
      assert.equal(await p.evaluate(() => moveCount), 1);
      console.log("PASS drag drop continues from the released position");
      await p.emulateMedia({ reducedMotion: "reduce" });
      await p.evaluate(async () => {
        await ui.setBoard({ turn: "WHITE", squares: { a1: "WM", h8: "BM" } });
        await ui.setBoard(
          { turn: "BLACK", squares: { b2: "WM", h8: "BM" } },
          { path: ["a1", "b2"] },
        );
        if (qaRoot.querySelector(".piece-motion"))
          throw new Error("Reduced motion ignored");
      });
      console.log("PASS reduced motion");
      await p.evaluate(async () => {
        await ui.setBoard({ turn: "WHITE", squares: { a1: "WM", h8: "BM" } });
        ui.setLegal([["a1", "b2"]]);
      });
      await p.locator("[data-square=a1]").focus();
      await p.keyboard.press("Enter");
      await p.locator("[data-square=b2]").focus();
      await p.keyboard.press("Enter");
      await p.waitForFunction(() => ui.getBoard().squares.b2 === "WM");
      assert.equal(await p.evaluate(() => moveCount), 2);
      console.log("PASS keyboard move uses the regular move callback");
      assert.deepEqual(errors, []);
    } finally {
      await b.close();
    }
  },
);
