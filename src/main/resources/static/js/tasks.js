document.addEventListener('DOMContentLoaded', () => {
  const boardRoot = document.querySelector('[data-task-board]');
  const feedback = document.querySelector('[data-task-feedback]');
  const instructions = document.querySelector('[data-task-instructions]');
  const today = localDateKey(new Date());
  const task = dailyTasks()[dayNumber(today) % 3];
  const storageKey = 'checkers.dailyTask.solved.' + today;
  let board = cloneSquares(task.squares);
  let selected = null;
  let solved = isSolvedToday();

  let stepIndex = 0;

  function show(message, ok) {
    if (!feedback) return;
    feedback.textContent = message;
    feedback.className = 'task-feedback ' + (ok ? 'ok' : 'bad');
  }

  function dayNumber(isoDate) {
    return Math.floor(new Date(isoDate + 'T00:00:00Z').getTime() / 86400000);
  }

  function localDateKey(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return year + '-' + month + '-' + day;
  }

  function cloneSquares(squares) {
    return { ...squares };
  }

  function isSolvedToday() {
    try {
      return localStorage.getItem(storageKey) === '1';
    } catch (_) {
      return false;
    }
  }

  function midpoint(from, to) {
    const files = 'abcdefgh';
    const fromFile = files.indexOf(from[0]);
    const toFile = files.indexOf(to[0]);
    const fromRank = Number(from[1]);
    const toRank = Number(to[1]);
    return files[(fromFile + toFile) / 2] + ((fromRank + toRank) / 2);
  }

  function resetAfterWrong() {
    stepIndex = 0;
    board = cloneSquares(task.squares);
    selected = null;
    render();
    show(Checkers.t('tasks.wrong'), false);
  }

  function completeTask() {
    solved = true;
    stepIndex = 0;
    selected = null;
    try {
      localStorage.setItem(storageKey, '1');
    } catch (_) { /* ignore storage failures */ }
    render();
    show(Checkers.t('tasks.correct'), true);
    if (instructions) instructions.textContent = Checkers.t('tasks.completed');
  }

  function nextSquares(from) {
    if (from !== task.solution[stepIndex]) return [];
    const next = task.solution[stepIndex + 1];
    return next ? [next] : [];
  }

  function onSquareClick(alg) {
    if (solved) {
      show(Checkers.t('tasks.alreadySolved'), true);
      return;
    }

    if (!selected) {
      if (!board[alg]) return;
      if (board[alg][0] !== 'W') return;
      selected = alg;
      render();
      return;
    }

    if (alg === selected) {
      selected = null;
      render();
      return;
    }

    if (board[alg] && board[alg][0] === 'W') {
      if (stepIndex > 0) {
        resetAfterWrong();
        return;
      }
      selected = alg;
      render();
      return;
    }

    if (selected !== task.solution[stepIndex]) {
      resetAfterWrong();
      return;
    }

    const expectedNext = task.solution[stepIndex + 1];
    if (alg !== expectedNext) {
      resetAfterWrong();
      return;
    }

    // Valid step in multi-jump sequence
    const currentPos = task.solution[stepIndex];
    delete board[midpoint(currentPos, alg)];
    delete board[currentPos];
    board[alg] = 'WM';
    stepIndex++;

    if (stepIndex >= task.solution.length - 1) {
      completeTask();
    } else {
      selected = alg;
      render();
    }
  }

  function render() {
    if (!boardRoot) return;
    boardRoot.innerHTML = '';
    boardRoot.className = 'task-board-interactive board' + (solved ? ' is-solved' : '');

    for (let rank = 7; rank >= 0; rank--) {
      for (let file = 0; file < 8; file++) {
        const alg = Checkers.BoardUI.algebraic(file, rank);
        const square = document.createElement('button');
        const dark = (file + rank) % 2 === 0;
        square.type = 'button';
        square.className = 'sq ' + (dark ? 'dark' : 'light');
        square.dataset.square = alg;
        square.disabled = solved;
        if (selected === alg) square.classList.add('selected');
        if (selected && nextSquares(selected).includes(alg)) square.classList.add('cap');

        const code = board[alg];
        if (code) {
          const piece = document.createElement('span');
          piece.className = 'piece ' + (code[0] === 'W' ? 'white' : 'black');
          square.appendChild(piece);
        }
        square.addEventListener('click', () => onSquareClick(alg));
        boardRoot.appendChild(square);
      }
    }
  }

  function dailyTasks() {
    return [
      {
        squares: { e3: 'WM', b2: 'WM', d4: 'BM', d6: 'BM', f6: 'BM' },
        solution: ['e3', 'c5', 'e7']
      },
      {
        squares: { c3: 'WM', g3: 'WM', d4: 'BM', f6: 'BM', b6: 'BM' },
        solution: ['c3', 'e5', 'g7']
      },
      {
        squares: { b2: 'WM', e3: 'WM', c3: 'BM', e5: 'BM', g7: 'BM' },
        solution: ['b2', 'd4', 'f6', 'h8']
      }
    ];
  }

  document.querySelector('[data-reset-task]')?.addEventListener('click', () => {
    solved = false;
    stepIndex = 0;
    try { localStorage.removeItem(storageKey); } catch (_) {}
    board = cloneSquares(task.squares);
    selected = null;
    render();
    if (instructions) instructions.textContent = Checkers.t('tasks.instructions');
    if (feedback) feedback.className = 'task-feedback hidden';
  });

  window.addEventListener('checkers:localechange', () => {
    if (solved) {
      show(Checkers.t('tasks.alreadySolved'), true);
      if (instructions) instructions.textContent = Checkers.t('tasks.completed');
    } else {
      if (instructions) instructions.textContent = Checkers.t('tasks.instructions');
    }
  });

  render();
  if (solved) {
    show(Checkers.t('tasks.alreadySolved'), true);
    if (instructions) instructions.textContent = Checkers.t('tasks.completed');
  }
});
