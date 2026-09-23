window.Checkers = window.Checkers || {};

Checkers.BoardUI = (function () {
  const FILES = 'abcdefgh';

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
      animating: false
    };

    function mandatoryCaptureSources() {
      const sources = new Set();
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

    function render() {
      root.innerHTML = '';
      root.className = 'board';
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
          if (highlights.dest.has(alg)) sq.classList.add('hl');
          if (highlights.cap.has(alg)) sq.classList.add('cap');
          if (mandatorySources.has(alg)) sq.classList.add('must-capture-sq');

          const code = state.board.squares && state.board.squares[alg];
          if (code) {
            const piece = document.createElement('div');
            piece.className = 'piece ' + (code[0] === 'W' ? 'white' : 'black');
            if (code[1] === 'K') {
              piece.classList.add('king');
              piece.innerHTML = '<svg class="crown-icon" viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M5 16L3 5l5.5 5L12 4l3.5 6L21 5l-2 11H5zm14 3c0 .6-.4 1-1 1H6c-.6 0-1-.4-1-1v-1h14v1z"/></svg>';
            }
            if (mandatorySources.has(alg) && !state.selected) {
              piece.classList.add('must-capture');
            }
            sq.appendChild(piece);
          }
          sq.addEventListener('click', () => onClick(alg));
          root.appendChild(sq);
        });
      });
    }

    function highlightSet() {
      const dest = new Set();
      const cap = new Set();
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

    function onClick(alg) {
      if (state.animating) return;
      const code = state.board.squares && state.board.squares[alg];
      if (!state.selected) {
        if (!code) return;
        const starts = state.legal.filter((p) => p[0] === alg);
        if (!starts.length) return;
        state.selected = alg;
        state.partial = [alg];
        render();
        return;
      }

      if (alg === state.selected && state.partial.length === 1) {
        state.selected = null;
        state.partial = [];
        render();
        return;
      }

      // Check if user clicked the final destination of a multi-jump path directly
      const candidateDirect = state.legal.filter((p) => p[0] === state.selected && p[p.length - 1] === alg);
      if (candidateDirect.length === 1 && state.partial.length === 1) {
        const fullPath = candidateDirect[0].slice();
        state.selected = null;
        state.partial = [];
        render();
        if (state.onMove) state.onMove(fullPath);
        return;
      }

      const candidate = state.partial.concat([alg]);
      const exact = state.legal.find((p) => p.length === candidate.length && p.every((s, i) => s === candidate[i]));
      const extends_ = state.legal.some((p) => {
        if (p.length <= candidate.length) return false;
        return candidate.every((s, i) => p[i] === s);
      });

      if (exact) {
        const path = exact.slice();
        state.selected = null;
        state.partial = [];
        render();
        if (state.onMove) state.onMove(path);
        return;
      }
      if (extends_) {
        state.partial = candidate;
        render();
        return;
      }

      if (code) {
        const starts = state.legal.filter((p) => p[0] === alg);
        if (starts.length) {
          state.selected = alg;
          state.partial = [alg];
          render();
        }
      }
    }

    function applyBoard(next, lastFrom, lastTo) {
      const oldSquares = (state.board && state.board.squares) || {};
      const newSquares = (next && next.squares) || {};
      const removed = Object.keys(oldSquares).filter((k) => oldSquares[k] && !newSquares[k]);

      if (lastFrom) state.lastFrom = lastFrom;
      if (lastTo) state.lastTo = lastTo;

      if (removed.length && root.querySelector('.piece')) {
        state.animating = true;
        removed.forEach((alg) => {
          const el = root.querySelector('[data-square="' + alg + '"] .piece');
          if (el) el.classList.add('capturing');
        });
        if (lastTo) {
          const dest = root.querySelector('[data-square="' + lastTo + '"] .piece');
          if (dest) {
            dest.style.transform = 'scale(1.08)';
            setTimeout(() => { dest.style.transform = ''; }, 180);
          }
        }
        setTimeout(() => {
          state.board = next;
          state.animating = false;
          render();
        }, 240);
      } else {
        state.board = next;
        render();
      }
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
        render();
      },
      setLegal(paths) {
        state.legal = paths || [];
        state.selected = null;
        state.partial = [];
        render();
      },
      setFlip(flip) {
        state.flip = !!flip;
        render();
      },
      getBoard() {
        return state.board;
      },
      render
    };
  }

  return { create, parseBoard, algebraic, isCapturePath };
})();
