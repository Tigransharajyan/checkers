window.Checkers = window.Checkers || {};

Checkers.BoardUI = (function () {
  const FILES = 'abcdefgh';
  const DRAG_THRESHOLD = 6;
  const MOVE_ANIM_MS = 240;
  const CAPTURE_ANIM_MS = 220;

  function parseBoard(boardState) {
    if (!boardState) return { turn: 'WHITE', squares: {} };
    if (typeof boardState === 'string') {
      try {
        return JSON.parse(boardState);
      } catch {
        return { turn: 'WHITE', squares: {} };
      }
    }
    return boardState;
  }

  function algebraic(file, rank) {
    return FILES[file] + (rank + 1);
  }

  function updateCoordLabels(flip) {
    const ranks = document.querySelector('[data-rank-labels]');
    const files = document.querySelector('[data-file-labels]');
    if (ranks) {
      const order = flip ? [1, 2, 3, 4, 5, 6, 7, 8] : [8, 7, 6, 5, 4, 3, 2, 1];
      ranks.innerHTML = order.map((n) => '<span>' + n + '</span>').join('');
    }
    if (files) {
      const order = flip ? 'hgfedcba' : 'abcdefgh';
      files.innerHTML = order.split('').map((c) => '<span>' + c + '</span>').join('');
    }
  }

  function isCaptureStep(fromAlg, toAlg) {
    if (!fromAlg || !toAlg) return false;
    const df = Math.abs(fromAlg.charCodeAt(0) - toAlg.charCodeAt(0));
    const dr = Math.abs(fromAlg.charCodeAt(1) - toAlg.charCodeAt(1));
    return df >= 2 && dr >= 2;
  }

  function isCapturePath(path) {
    if (!path || path.length < 2) return false;
    if (path.length > 2) return true;
    return isCaptureStep(path[0], path[1]);
  }

  function prefersReducedMotion() {
    return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  }

  function isDarkAlg(alg) {
    if (!alg || alg.length < 2) return false;
    const f = alg.charCodeAt(0) - 97;
    const r = alg.charCodeAt(1) - 49;
    return f >= 0 && f < 8 && r >= 0 && r < 8 && (f + r) % 2 === 0;
  }

  function pathsEqual(a, b) {
    return !!(a && b && a.length === b.length && a.every((s, i) => s === b[i]));
  }

  /** Match queued premove against real legal moves (exact path, else unique from→to). */
  function matchPremove(paths, premove) {
    if (!premove || premove.length < 2 || !paths || !paths.length) return null;
    const exact = paths.find((p) => pathsEqual(p, premove));
    if (exact) return exact;
    const from = premove[0];
    const to = premove[premove.length - 1];
    const byEnds = paths.filter((p) => p[0] === from && p[p.length - 1] === to);
    return byEnds.length === 1 ? byEnds[0] : null;
  }

  function create(root, options) {
    const state = {
      root,
      flip: !!(options && options.flip),
      board: { turn: 'WHITE', squares: {} },
      legal: [],
      selected: null,
      partial: [],
      lastFrom: null,
      lastTo: null,
      onMove: options && options.onMove,
      animating: false,
      circles: new Set(),
      arrows: new Set(),
      rightStart: null,
      rightStartPoint: null,
      drag: null,
      animToken: 0,
      premoveMode: false,
      premoveColor: null,
      premove: null
    };

    function ownColorChar() {
      return state.premoveColor === 'BLACK' ? 'B' : 'W';
    }

    function isOwnPiece(alg) {
      const code = state.board.squares && state.board.squares[alg];
      return !!(code && state.premoveMode && state.premoveColor && code[0] === ownColorChar());
    }

    function invalidatePremoveIfBroken() {
      if (!state.premove || !state.premove.length) return;
      const from = state.premove[0];
      const code = state.board.squares && state.board.squares[from];
      const want = ownColorChar();
      if (!code || code[0] !== want) {
        state.premove = null;
      }
    }

    function mandatoryCaptureSources() {
      const sources = new Set();
      if (state.premoveMode) return sources;
      const hasCaptures = state.legal.some(isCapturePath);
      if (hasCaptures) {
        state.legal.forEach((path) => {
          if (isCapturePath(path)) {
            sources.add(path[0]);
          }
        });
      }
      return sources;
    }

    function isMovableSquare(alg) {
      if (!alg || state.animating) return false;
      if (state.premoveMode) return isOwnPiece(alg);
      if (state.partial.length > 1) {
        return alg === state.partial[state.partial.length - 1] || alg === state.selected;
      }
      return state.legal.some((path) => path[0] === alg);
    }

    function render() {
      const dragFrom = state.drag && state.drag.started ? state.drag.from : null;
      root.innerHTML = '';
      root.className = 'board' + (dragFrom ? ' is-dragging' : '');
      const orderRanks = state.flip
        ? [0, 1, 2, 3, 4, 5, 6, 7]
        : [7, 6, 5, 4, 3, 2, 1, 0];
      const orderFiles = state.flip
        ? [7, 6, 5, 4, 3, 2, 1, 0]
        : [0, 1, 2, 3, 4, 5, 6, 7];

      const highlights = highlightSet();
      const mandatorySources = mandatoryCaptureSources();
      updateCoordLabels(state.flip);

      orderRanks.forEach((rank) => {
        orderFiles.forEach((file) => {
          const alg = algebraic(file, rank);
          const sq = document.createElement('div');
          const dark = (file + rank) % 2 === 0;
          sq.className = 'sq ' + (dark ? 'dark' : 'light');
          sq.dataset.square = alg;
          if (state.selected === alg || state.partial.includes(alg)) {
            sq.classList.add('selected');
          }
          if (state.lastFrom === alg) sq.classList.add('last-from');
          if (state.lastTo === alg) sq.classList.add('last-to');
          if (state.premove && state.premove.length >= 2) {
            if (state.premove[0] === alg) sq.classList.add('premove-from');
            if (state.premove[state.premove.length - 1] === alg) sq.classList.add('premove-to');
            if (state.premove.includes(alg)) sq.classList.add('premove-path');
          }
          if (highlights.dest.has(alg)) sq.classList.add(state.premoveMode ? 'premove-hl' : 'hl');
          if (highlights.cap.has(alg)) sq.classList.add(state.premoveMode ? 'premove-cap' : 'cap');
          if (mandatorySources.has(alg)) sq.classList.add('must-capture-sq');
          if (highlights.dest.has(alg) || highlights.cap.has(alg)) {
            sq.classList.add('drop-target');
          }

          const code = state.board.squares && state.board.squares[alg];
          if (code) {
            const piece = document.createElement('div');
            piece.className = 'piece ' + (code[0] === 'W' ? 'white' : 'black');
            if (code[1] === 'K') {
              piece.classList.add('king');
              piece.innerHTML = '<svg class="crown-icon" viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M5 16L3 5l5.5 5L12 4l3.5 6L21 5l-2 11H5zm14 3c0 .6-.4 1-1 1H6c-.6 0-1-.4-1-1v-1h14v1z"/></svg>';
            }
            if (mandatorySources.has(alg) && !state.selected && !dragFrom) {
              piece.classList.add('must-capture');
            }
            if (dragFrom === alg) {
              piece.classList.add('is-drag-source');
            }
            if (isMovableSquare(alg)) {
              sq.classList.add('can-move');
            }
            sq.appendChild(piece);
          }
          root.appendChild(sq);
        });
      });
      renderAnnotations();
    }

    function arrowKey(from, to) { return from + ':' + to; }

    function squareCenter(alg) {
      const square = root.querySelector('[data-square="' + alg + '"]');
      if (!square) return null;
      const squareRect = square.getBoundingClientRect();
      const boardRect = root.getBoundingClientRect();
      return { x: squareRect.left - boardRect.left + squareRect.width / 2, y: squareRect.top - boardRect.top + squareRect.height / 2 };
    }

    function renderAnnotations() {
      root.querySelector('.board-annotations')?.remove();
      const namespace = 'http://www.w3.org/2000/svg';
      const overlay = document.createElementNS(namespace, 'svg');
      overlay.classList.add('board-annotations');
      overlay.setAttribute('viewBox', '0 0 100 100');
      overlay.setAttribute('preserveAspectRatio', 'none');
      overlay.setAttribute('aria-hidden', 'true');
      const defs = document.createElementNS(namespace, 'defs');
      const marker = document.createElementNS(namespace, 'marker');
      marker.setAttribute('id', 'board-arrowhead');
      marker.setAttribute('viewBox', '0 0 10 10');
      marker.setAttribute('refX', '8');
      marker.setAttribute('refY', '5');
      marker.setAttribute('markerWidth', '5');
      marker.setAttribute('markerHeight', '5');
      marker.setAttribute('orient', 'auto-start-reverse');
      const tip = document.createElementNS(namespace, 'path');
      tip.setAttribute('d', 'M 0 0 L 10 5 L 0 10 z');
      tip.setAttribute('class', 'board-arrow-tip');
      marker.appendChild(tip);
      defs.appendChild(marker);
      overlay.appendChild(defs);
      const rect = root.getBoundingClientRect();
      state.arrows.forEach((key) => {
        const [from, to] = key.split(':');
        const start = squareCenter(from);
        const end = squareCenter(to);
        if (!start || !end) return;
        const line = document.createElementNS(namespace, 'line');
        line.setAttribute('x1', String(start.x / rect.width * 100));
        line.setAttribute('y1', String(start.y / rect.height * 100));
        line.setAttribute('x2', String(end.x / rect.width * 100));
        line.setAttribute('y2', String(end.y / rect.height * 100));
        line.setAttribute('marker-end', 'url(#board-arrowhead)');
        line.setAttribute('class', 'board-arrow');
        overlay.appendChild(line);
      });
      state.circles.forEach((alg) => {
        const center = squareCenter(alg);
        if (!center) return;
        const circle = document.createElementNS(namespace, 'circle');
        circle.setAttribute('cx', String(center.x / rect.width * 100));
        circle.setAttribute('cy', String(center.y / rect.height * 100));
        circle.setAttribute('r', '3.35');
        circle.setAttribute('class', 'board-circle');
        overlay.appendChild(circle);
      });
      root.appendChild(overlay);
    }

    function clearAnnotations() {
      if (!state.circles.size && !state.arrows.size) return;
      state.circles.clear();
      state.arrows.clear();
      renderAnnotations();
    }

    function squareAtPoint(clientX, clientY) {
      const target = document.elementFromPoint(clientX, clientY);
      const square = target && target.closest && target.closest('[data-square]');
      return square && root.contains(square) ? square.dataset.square : null;
    }

    function moveGhost(clientX, clientY) {
      const drag = state.drag;
      if (!drag || !drag.ghost) return;
      const x = clientX - drag.offsetX;
      const y = clientY - drag.offsetY;
      drag.ghost.style.transform = 'translate3d(' + x + 'px, ' + y + 'px, 0) scale(1.12)';
    }

    function clearGhost() {
      const drag = state.drag;
      if (!drag) return;
      if (drag.raf) cancelAnimationFrame(drag.raf);
      if (drag.ghost && drag.ghost.parentNode) drag.ghost.parentNode.removeChild(drag.ghost);
      drag.ghost = null;
      root.classList.remove('is-dragging');
      document.body.classList.remove('board-piece-dragging');
    }

    function endDrag() {
      clearGhost();
      state.drag = null;
    }

    function stopDragListeners() {
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('pointerup', onPointerUp);
      window.removeEventListener('pointercancel', onPointerUp);
    }

    /** Cancel in-progress drag (e.g. RMB). Clears selection when a piece was being dragged. */
    function cancelActiveDrag() {
      const drag = state.drag;
      if (!drag) return;
      const wasDragging = drag.started;
      stopDragListeners();
      endDrag();
      if (wasDragging) {
        state.selected = null;
        state.partial = [];
        render();
      } else {
        render();
      }
    }

    function clearPremove() {
      if (!state.premove) return false;
      state.premove = null;
      if (!state.animating) render();
      return true;
    }

    function beginDragVisual(clientX, clientY) {
      const drag = state.drag;
      if (!drag || drag.started || !drag.canDrag) return;
      const piece = root.querySelector('[data-square="' + drag.from + '"] .piece');
      if (!piece) return;

      drag.started = true;
      clearAnnotations();

      if (!state.selected || (state.selected !== drag.from && state.partial[state.partial.length - 1] !== drag.from)) {
        state.selected = drag.from;
        state.partial = [drag.from];
      } else if (!state.partial.length) {
        state.partial = [drag.from];
      }

      const rect = piece.getBoundingClientRect();
      drag.offsetX = clientX - rect.left;
      drag.offsetY = clientY - rect.top;

      const ghost = piece.cloneNode(true);
      ghost.classList.remove('must-capture', 'is-drag-source', 'is-moving');
      ghost.classList.add('piece-ghost');
      ghost.style.width = rect.width + 'px';
      ghost.style.height = rect.height + 'px';
      document.body.appendChild(ghost);
      drag.ghost = ghost;
      document.body.classList.add('board-piece-dragging');
      moveGhost(clientX, clientY);

      render();
      moveGhost(clientX, clientY);
    }

    function onPointerMove(event) {
      const drag = state.drag;
      if (!drag || event.pointerId !== drag.pointerId) return;

      const dist = Math.hypot(event.clientX - drag.startX, event.clientY - drag.startY);
      if (!drag.started && drag.canDrag && dist >= DRAG_THRESHOLD) {
        beginDragVisual(event.clientX, event.clientY);
      }
      if (!drag.started || !drag.ghost) return;

      event.preventDefault();
      moveGhost(event.clientX, event.clientY);

      const over = squareAtPoint(event.clientX, event.clientY);
      if (over !== drag.hover) {
        if (drag.hover) {
          root.querySelector('[data-square="' + drag.hover + '"]')?.classList.remove('drag-over');
        }
        drag.hover = over;
        if (over) {
          root.querySelector('[data-square="' + over + '"]')?.classList.add('drag-over');
        }
      }
    }

    function onPointerUp(event) {
      const drag = state.drag;
      if (!drag || event.pointerId !== drag.pointerId) return;

      stopDragListeners();

      const started = drag.started;
      const from = drag.from;
      const wasSelected = drag.wasSelected;
      const to = squareAtPoint(event.clientX, event.clientY);
      endDrag();

      if (started) {
        if (to && to !== from) {
          // Drag-drop premove: any dark square, including own pieces.
          if (state.premoveMode && isDarkAlg(to)) {
            commitPath([from, to]);
          } else {
            onClick(to);
          }
        } else if (wasSelected) {
          // Second click on the same piece (with tiny drag) — remove focus.
          clearSelection();
        } else {
          // Accidental micro-drag while selecting — keep focus.
          render();
        }
        return;
      }

      // Plain click: select / move / deselect.
      onClick(to || from);
    }

    root.addEventListener('contextmenu', (event) => event.preventDefault());
    root.addEventListener('mousedown', (event) => {
      if (event.button === 0) {
        clearAnnotations();
        return;
      }
      if (event.button !== 2) return;
      // RMB while dragging — cancel drag and clear focus.
      if (state.drag) {
        event.preventDefault();
        state.rightStart = null;
        state.rightStartPoint = null;
        cancelActiveDrag();
        return;
      }
      // RMB cancels a queued premove (chess.com-style).
      if (state.premove) {
        event.preventDefault();
        clearPremove();
        state.rightStart = null;
        state.rightStartPoint = null;
        return;
      }
      const square = event.target.closest('[data-square]');
      if (!square) return;
      event.preventDefault();
      state.rightStart = square.dataset.square;
      state.rightStartPoint = { x: event.clientX, y: event.clientY };
    });

    // Also catch RMB via pointer events (some browsers skip mousedown order).
    window.addEventListener('pointerdown', (event) => {
      if (event.button !== 2) return;
      if (state.drag) {
        event.preventDefault();
        state.rightStart = null;
        state.rightStartPoint = null;
        cancelActiveDrag();
        return;
      }
      if (state.premove) {
        event.preventDefault();
        clearPremove();
        state.rightStart = null;
        state.rightStartPoint = null;
      }
    });
    root.addEventListener('mouseup', (event) => {
      if (event.button !== 2 || !state.rightStart) return;
      event.preventDefault();
      const from = state.rightStart;
      const to = squareAtPoint(event.clientX, event.clientY);
      const moved = Math.hypot(event.clientX - state.rightStartPoint.x, event.clientY - state.rightStartPoint.y) > 8;
      state.rightStart = null;
      state.rightStartPoint = null;
      if (!to || (!moved && to !== from)) return;
      if (!moved || to === from) {
        state.circles.has(from) ? state.circles.delete(from) : state.circles.add(from);
      } else {
        const key = arrowKey(from, to);
        state.arrows.has(key) ? state.arrows.delete(key) : state.arrows.add(key);
      }
      renderAnnotations();
    });

    root.addEventListener('pointerdown', (event) => {
      if (event.button !== 0 || state.animating || state.drag) return;
      const square = event.target.closest('[data-square]');
      if (!square || !root.contains(square)) return;
      const alg = square.dataset.square;
      const canDrag =
        !!square.querySelector('.piece') &&
        (isMovableSquare(alg) ||
          (state.premoveMode && isOwnPiece(alg)) ||
          (state.selected && state.partial.length && alg === state.selected));

      state.drag = {
        pointerId: event.pointerId,
        from: alg,
        startX: event.clientX,
        startY: event.clientY,
        offsetX: 0,
        offsetY: 0,
        started: false,
        canDrag,
        wasSelected: state.selected === alg && state.partial.length <= 1,
        ghost: null,
        hover: null,
        raf: 0
      };

      window.addEventListener('pointermove', onPointerMove, { passive: false });
      window.addEventListener('pointerup', onPointerUp);
      window.addEventListener('pointercancel', onPointerUp);
    });

    function highlightSet() {
      const dest = new Set();
      const cap = new Set();

      // Premoves: any dark square is a valid destination (no legality checks).
      if (state.premoveMode) {
        if (!state.selected) return { dest, cap };
        for (let f = 0; f < 8; f++) {
          for (let r = 0; r < 8; r++) {
            if ((f + r) % 2 !== 0) continue;
            const alg = algebraic(f, r);
            if (alg === state.selected) continue;
            dest.add(alg);
            if (state.board.squares && state.board.squares[alg]) cap.add(alg);
          }
        }
        return { dest, cap };
      }

      const prefix = state.partial.length ? state.partial : (state.selected ? [state.selected] : []);
      if (!prefix.length) return { dest, cap };

      state.legal.forEach((path) => {
        if (path.length <= prefix.length) return;
        let match = true;
        for (let i = 0; i < prefix.length; i++) {
          if (path[i] !== prefix[i]) {
            match = false;
            break;
          }
        }
        if (!match) return;
        const next = path[prefix.length];
        const prev = prefix[prefix.length - 1];
        dest.add(next);
        if (isCaptureStep(prev, next) || isCapturePath(path)) {
          cap.add(next);
        }
      });
      return { dest, cap };
    }

    function clearSelection() {
      state.selected = null;
      state.partial = [];
      render();
    }

    function commitPath(path) {
      state.selected = null;
      state.partial = [];
      if (state.premoveMode) {
        state.premove = path.slice();
        render();
        return;
      }
      render();
      if (state.onMove) state.onMove(path);
    }

    function onClick(alg) {
      if (state.animating || !alg) return;
      const code = state.board.squares && state.board.squares[alg];

      // Free premove: own piece → any dark square (even "illegal" ones).
      if (state.premoveMode) {
        if (!state.selected) {
          if (!isOwnPiece(alg)) return;
          state.selected = alg;
          state.partial = [alg];
          render();
          return;
        }
        if (alg === state.selected) {
          clearSelection();
          return;
        }
        if (isOwnPiece(alg)) {
          state.selected = alg;
          state.partial = [alg];
          render();
          return;
        }
        if (isDarkAlg(alg)) {
          commitPath([state.selected, alg]);
          return;
        }
        clearSelection();
        return;
      }

      if (!state.selected) {
        if (!code) return;
        const starts = state.legal.filter((p) => p[0] === alg);
        if (!starts.length) return;
        state.selected = alg;
        state.partial = [alg];
        render();
        return;
      }

      // Second click on the same piece removes focus.
      if (alg === state.selected && state.partial.length === 1) {
        clearSelection();
        return;
      }

      // Click final destination of a multi-jump path directly.
      const candidateDirect = state.legal.filter((p) => p[0] === state.selected && p[p.length - 1] === alg);
      if (candidateDirect.length === 1 && state.partial.length === 1) {
        commitPath(candidateDirect[0]);
        return;
      }

      const candidate = state.partial.concat([alg]);
      const exact = state.legal.find((p) => p.length === candidate.length && p.every((s, i) => s === candidate[i]));
      const extends_ = state.legal.some((p) => {
        if (p.length <= candidate.length) return false;
        return candidate.every((s, i) => p[i] === s);
      });

      if (exact) {
        commitPath(exact);
        return;
      }
      if (extends_) {
        state.partial = candidate;
        render();
        return;
      }

      // Another movable piece — switch selection.
      if (code) {
        const starts = state.legal.filter((p) => p[0] === alg);
        if (starts.length) {
          state.selected = alg;
          state.partial = [alg];
          render();
          return;
        }
      }

      // Click anywhere else on the board — remove focus.
      clearSelection();
    }

    function finishBoardUpdate(next, token) {
      if (token !== state.animToken) return;
      state.board = next;
      state.animating = false;
      invalidatePremoveIfBroken();
      render();
    }

    function applyBoard(next, lastFrom, lastTo) {
      const oldSquares = (state.board && state.board.squares) || {};
      const newSquares = (next && next.squares) || {};
      const removed = Object.keys(oldSquares).filter((k) => oldSquares[k] && !newSquares[k]);
      const token = ++state.animToken;

      if (lastFrom) state.lastFrom = lastFrom;
      if (lastTo) state.lastTo = lastTo;

      if (state.drag) {
        endDrag();
      }

      const fromEl = lastFrom ? root.querySelector('[data-square="' + lastFrom + '"] .piece') : null;
      const toSq = lastTo ? root.querySelector('[data-square="' + lastTo + '"]') : null;
      const canAnimateMove =
        !prefersReducedMotion() &&
        fromEl &&
        toSq &&
        lastFrom &&
        lastTo &&
        lastFrom !== lastTo &&
        oldSquares[lastFrom];

      if (canAnimateMove) {
        state.animating = true;
        state.selected = null;
        state.partial = [];

        removed.forEach((alg) => {
          if (alg === lastFrom) return;
          const el = root.querySelector('[data-square="' + alg + '"] .piece');
          if (el) el.classList.add('capturing');
        });

        const fromSq = root.querySelector('[data-square="' + lastFrom + '"]');
        const fromRect = fromSq.getBoundingClientRect();
        const toRect = toSq.getBoundingClientRect();
        const dx = toRect.left - fromRect.left;
        const dy = toRect.top - fromRect.top;

        fromEl.classList.add('is-moving');
        fromEl.style.transform = 'translate3d(0, 0, 0)';
        // Force layout so the transition always runs from the origin.
        void fromEl.offsetWidth;
        fromEl.style.transform = 'translate3d(' + dx + 'px, ' + dy + 'px, 0) scale(1.04)';

        setTimeout(() => finishBoardUpdate(next, token), MOVE_ANIM_MS);
        return;
      }

      if (removed.length && root.querySelector('.piece') && !prefersReducedMotion()) {
        state.animating = true;
        removed.forEach((alg) => {
          const el = root.querySelector('[data-square="' + alg + '"] .piece');
          if (el) el.classList.add('capturing');
        });
        if (lastTo) {
          const dest = root.querySelector('[data-square="' + lastTo + '"] .piece');
          if (dest) {
            dest.classList.add('is-landing');
          }
        }
        setTimeout(() => finishBoardUpdate(next, token), CAPTURE_ANIM_MS);
        return;
      }

      finishBoardUpdate(next, token);
    }

    updateCoordLabels(state.flip);

    return {
      setBoard(boardState, lastMove) {
        const next = parseBoard(boardState);
        const from = lastMove && lastMove.from;
        const to = lastMove && lastMove.to;
        applyBoard(next, from, to);
      },
      setLastMove(from, to) {
        state.lastFrom = from || null;
        state.lastTo = to || null;
        if (!state.animating) render();
      },
      setLegal(paths) {
        state.legal = paths || [];
        state.selected = null;
        state.partial = [];
        if (!state.animating) render();
      },
      setPremoveMode(enabled, color) {
        state.premoveMode = !!enabled;
        state.premoveColor = enabled ? (color || null) : null;
        state.selected = null;
        state.partial = [];
        if (!state.animating) render();
      },
      setPremove(path) {
        state.premove = path && path.length >= 2 ? path.slice() : null;
        state.selected = null;
        state.partial = [];
        if (!state.animating) render();
      },
      getPremove() {
        return state.premove ? state.premove.slice() : null;
      },
      clearPremove() {
        return clearPremove();
      },
      setFlip(flip) {
        state.flip = !!flip;
        if (!state.animating) render();
      },
      getBoard() {
        return state.board;
      },
      render
    };
  }

  return { create, parseBoard, algebraic, isCapturePath, pathsEqual, matchPremove };
})();
