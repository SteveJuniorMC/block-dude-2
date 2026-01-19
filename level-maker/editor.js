// Block Dude 2 Level Editor - with horizontal scrolling support

class LevelEditor {
    constructor() {
        this.width = 32;  // Default wider for scrolling levels
        this.height = 16;
        this.grid = [];
        this.currentTool = 'grass';
        this.isDrawing = false;
        this.isPlayMode = false;

        // Scroll vs draw detection
        this.isPanning = false;
        this.panStartX = 0;
        this.panStartY = 0;
        this.panStartScrollLeft = 0;
        this.touchStartTime = 0;
        this.touchMoved = false;

        // Play mode state
        this.playerPos = null;
        this.playerFacing = 'right';
        this.holdingBlock = false;
        this.blocks = new Set();
        this.enemies = []; // Array of {x, y, facing}
        this.moves = 0;
        this.gameOver = false;

        this.init();
    }

    init() {
        this.createGrid();
        this.bindEvents();
        this.loadLevelsList();
        this.updateScrollInfo();
    }

    updateScrollInfo() {
        const info = document.getElementById('scrollInfo');
        info.textContent = `${this.width}x${this.height} - Swipe to scroll, tap to place`;
    }

    createGrid() {
        const gridEl = document.getElementById('grid');
        gridEl.innerHTML = '';
        gridEl.style.gridTemplateColumns = `repeat(${this.width}, 24px)`;
        gridEl.style.width = `${this.width * 24}px`;

        this.grid = [];
        for (let y = 0; y < this.height; y++) {
            const row = [];
            for (let x = 0; x < this.width; x++) {
                const cell = document.createElement('div');
                cell.className = 'cell';
                cell.dataset.x = x;
                cell.dataset.y = y;
                gridEl.appendChild(cell);
                row.push('empty');
            }
            this.grid.push(row);
        }

        // Add walls around border
        this.addBorderWalls();
        this.updateScrollInfo();
    }

    addBorderWalls() {
        // Only add terrain at the bottom with slight variance
        for (let x = 0; x < this.width; x++) {
            // Random chance for grass variation (about 30%)
            const terrainType = Math.random() < 0.3 ? 'grass2' : 'grass';
            this.setCell(x, this.height - 1, terrainType);
        }
    }

    setCell(x, y, type) {
        if (x < 0 || x >= this.width || y < 0 || y >= this.height) return;

        // Only one player and one door allowed
        if (type === 'player' || type === 'door') {
            this.removeAll(type);
        }

        this.grid[y][x] = type;
        this.updateCellVisual(x, y);
    }

    removeAll(type) {
        for (let y = 0; y < this.height; y++) {
            for (let x = 0; x < this.width; x++) {
                if (this.grid[y][x] === type) {
                    this.grid[y][x] = 'empty';
                    this.updateCellVisual(x, y);
                }
            }
        }
    }

    updateCellVisual(x, y) {
        const cell = document.querySelector(`.cell[data-x="${x}"][data-y="${y}"]`);
        if (cell) {
            cell.className = 'cell';
            const type = this.grid[y][x];
            if (type !== 'empty') {
                cell.classList.add(type);
            }
        }
    }

    bindEvents() {
        // Tool selection
        document.querySelectorAll('.tool-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.tool-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                this.currentTool = btn.dataset.tool;
            });
        });

        // Keyboard shortcuts for tools
        document.addEventListener('keydown', (e) => {
            if (this.isPlayMode) {
                this.handlePlayKeydown(e);
                return;
            }

            const shortcuts = { 'g': 'grass', 'w': 'wall', 'b': 'block', 'p': 'player', 'd': 'door', 's': 'enemy', 'e': 'erase' };
            if (shortcuts[e.key.toLowerCase()]) {
                this.currentTool = shortcuts[e.key.toLowerCase()];
                document.querySelectorAll('.tool-btn').forEach(b => b.classList.remove('active'));
                document.querySelector(`[data-tool="${this.currentTool}"]`).classList.add('active');
            }
        });

        // Grid drawing
        const grid = document.getElementById('grid');
        const container = document.getElementById('gridContainer');

        grid.addEventListener('mousedown', (e) => this.handleMouseDown(e));
        grid.addEventListener('mousemove', (e) => this.handleMouseMove(e));
        document.addEventListener('mouseup', () => {
            this.isDrawing = false;
            this.isPanning = false;
            container.style.cursor = 'default';
        });

        // Middle mouse button or shift+drag for panning
        container.addEventListener('mousedown', (e) => {
            if (e.button === 1 || e.shiftKey) { // Middle button or shift
                e.preventDefault();
                this.isPanning = true;
                this.panStartX = e.clientX;
                this.panStartScrollLeft = container.scrollLeft;
                container.style.cursor = 'grabbing';
            }
        });

        container.addEventListener('mousemove', (e) => {
            if (this.isPanning) {
                e.preventDefault();
                const dx = e.clientX - this.panStartX;
                container.scrollLeft = this.panStartScrollLeft - dx;
            }
        });

        // Touch support - swipe to scroll, tap to place
        grid.addEventListener('touchstart', (e) => this.handleTouchStart(e), { passive: false });
        grid.addEventListener('touchmove', (e) => this.handleTouchMove(e), { passive: false });
        grid.addEventListener('touchend', (e) => this.handleTouchEnd(e));

        // Buttons
        document.getElementById('resizeBtn').addEventListener('click', () => this.resize());
        document.getElementById('clearBtn').addEventListener('click', () => this.clear());
        document.getElementById('validateBtn').addEventListener('click', () => this.validate());
        document.getElementById('playBtn').addEventListener('click', () => this.enterPlayMode());
        document.getElementById('stopBtn').addEventListener('click', () => this.exitPlayMode());
        document.getElementById('saveBtn').addEventListener('click', () => this.save());
        document.getElementById('loadBtn').addEventListener('click', () => this.showLoadModal());
        document.getElementById('exportBtn').addEventListener('click', () => this.exportLevel());
        document.getElementById('closeModal').addEventListener('click', () => this.hideModal());

        // Play controls
        document.getElementById('ctrlUp').addEventListener('click', () => this.playMove('up'));
        document.getElementById('ctrlDown').addEventListener('click', () => this.playMove('down'));
        document.getElementById('ctrlLeft').addEventListener('click', () => this.playMove('left'));
        document.getElementById('ctrlRight').addEventListener('click', () => this.playMove('right'));
    }

    handleMouseDown(e) {
        if (this.isPlayMode) return;
        if (this.isPanning) return;
        if (e.shiftKey || e.button !== 0) return; // Only left click without shift
        const cell = e.target.closest('.cell');
        if (cell) {
            this.isDrawing = true;
            this.paintCell(cell);
        }
    }

    handleMouseMove(e) {
        if (this.isPanning) return;
        if (!this.isDrawing || this.isPlayMode) return;
        const cell = e.target.closest('.cell');
        if (cell) {
            this.paintCell(cell);
        }
    }

    handleTouchStart(e) {
        if (this.isPlayMode) return;
        if (e.touches.length !== 1) return;

        const container = document.getElementById('gridContainer');
        const touch = e.touches[0];

        this.panStartX = touch.clientX;
        this.panStartY = touch.clientY;
        this.panStartScrollLeft = container.scrollLeft;
        this.touchStartTime = Date.now();
        this.touchMoved = false;
        this.isPanning = true;
    }

    handleTouchMove(e) {
        if (this.isPlayMode) return;
        if (!this.isPanning || e.touches.length !== 1) return;

        const container = document.getElementById('gridContainer');
        const touch = e.touches[0];
        const dx = touch.clientX - this.panStartX;
        const dy = touch.clientY - this.panStartY;

        // If moved more than 10px, it's a scroll not a tap
        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
            this.touchMoved = true;
        }

        // Scroll horizontally
        if (this.touchMoved) {
            e.preventDefault();
            container.scrollLeft = this.panStartScrollLeft - dx;
        }
    }

    handleTouchEnd(e) {
        if (this.isPlayMode) return;

        // If didn't move much and was quick, treat as tap
        const tapDuration = Date.now() - this.touchStartTime;
        if (!this.touchMoved && tapDuration < 300) {
            const cell = document.elementFromPoint(this.panStartX, this.panStartY)?.closest('.cell');
            if (cell) {
                this.paintCell(cell);
            }
        }

        this.isPanning = false;
        this.touchMoved = false;
    }

    paintCell(cell) {
        const x = parseInt(cell.dataset.x);
        const y = parseInt(cell.dataset.y);
        const type = this.currentTool === 'erase' ? 'empty' : this.currentTool;
        this.setCell(x, y, type);
    }

    resize() {
        const newWidth = parseInt(document.getElementById('widthInput').value);
        const newHeight = parseInt(document.getElementById('heightInput').value);

        if (newWidth >= 16 && newWidth <= 100 && newHeight >= 10 && newHeight <= 30) {
            this.width = newWidth;
            this.height = newHeight;
            this.createGrid();
            this.showMessage('Grid resized to ' + newWidth + 'x' + newHeight, 'info');
        } else {
            this.showMessage('Width: 16-100, Height: 10-30', 'error');
        }
    }

    clear() {
        this.createGrid();
        this.showMessage('Grid cleared', 'info');
    }

    // Validation
    validate() {
        const errors = [];
        let playerCount = 0;
        let doorCount = 0;
        let playerPos = null;
        let doorPos = null;

        for (let y = 0; y < this.height; y++) {
            for (let x = 0; x < this.width; x++) {
                if (this.grid[y][x] === 'player') {
                    playerCount++;
                    playerPos = { x, y };
                }
                if (this.grid[y][x] === 'door') {
                    doorCount++;
                    doorPos = { x, y };
                }
            }
        }

        if (playerCount === 0) errors.push('No player placed');
        if (playerCount > 1) errors.push('Multiple players placed');
        if (doorCount === 0) errors.push('No door placed');
        if (doorCount > 1) errors.push('Multiple doors placed');

        // Helper to check if ground is solid
        const isSolidGround = (cell) => {
            return cell === 'wall' || cell === 'block' || cell === 'grass' || cell === 'grass2';
        };

        // Check player has ground
        if (playerPos) {
            const below = this.grid[playerPos.y + 1]?.[playerPos.x];
            if (!isSolidGround(below)) {
                errors.push('Player must be standing on solid ground');
            }
        }

        // Check door has ground
        if (doorPos) {
            const below = this.grid[doorPos.y + 1]?.[doorPos.x];
            if (!isSolidGround(below)) {
                errors.push('Door must be on solid ground');
            }
        }

        if (errors.length === 0) {
            this.showMessage('Level is valid!', 'success');
            return true;
        } else {
            this.showMessage('Validation failed:\n' + errors.join('\n'), 'error');
            return false;
        }
    }

    // Play Mode
    enterPlayMode() {
        if (!this.validate()) return;

        this.isPlayMode = true;
        this.moves = 0;
        this.gameOver = false;

        // Find player, blocks, and enemies
        this.blocks = new Set();
        this.enemies = [];
        for (let y = 0; y < this.height; y++) {
            for (let x = 0; x < this.width; x++) {
                if (this.grid[y][x] === 'player') {
                    this.playerPos = { x, y };
                }
                if (this.grid[y][x] === 'block') {
                    this.blocks.add(`${x},${y}`);
                }
                if (this.grid[y][x] === 'enemy') {
                    this.enemies.push({ x, y, facing: 'left' });
                }
            }
        }

        this.playerFacing = 'right';
        this.holdingBlock = false;

        document.getElementById('modeLabel').textContent = 'Play Mode';
        document.querySelector('.mode-indicator').classList.add('play-mode');
        document.getElementById('playBtn').style.display = 'none';
        document.getElementById('stopBtn').style.display = 'block';
        document.getElementById('playControls').style.display = 'flex';

        this.updatePlayStatus();
        this.renderPlayState();
        this.scrollToPlayer();
        this.showMessage('Play mode! Use arrow keys or buttons. UP to climb, DOWN to pick/place.', 'info');
    }

    scrollToPlayer() {
        if (!this.playerPos) return;
        const container = document.getElementById('gridContainer');
        const cellWidth = 24;
        const scrollX = (this.playerPos.x * cellWidth) - (container.clientWidth / 2) + (cellWidth / 2);
        container.scrollLeft = Math.max(0, scrollX);
    }

    exitPlayMode() {
        this.isPlayMode = false;

        document.getElementById('modeLabel').textContent = 'Edit Mode';
        document.querySelector('.mode-indicator').classList.remove('play-mode');
        document.getElementById('playBtn').style.display = 'block';
        document.getElementById('stopBtn').style.display = 'none';
        document.getElementById('playControls').style.display = 'none';

        // Restore original grid visuals
        for (let y = 0; y < this.height; y++) {
            for (let x = 0; x < this.width; x++) {
                this.updateCellVisual(x, y);
            }
        }

        this.showMessage('Back to edit mode', 'info');
    }

    handlePlayKeydown(e) {
        const keyMap = {
            'ArrowUp': 'up',
            'ArrowDown': 'down',
            'ArrowLeft': 'left',
            'ArrowRight': 'right'
        };
        if (keyMap[e.key]) {
            e.preventDefault();
            this.playMove(keyMap[e.key]);
        }
    }

    isSolid(x, y) {
        const cell = this.grid[y]?.[x];
        return cell === 'wall' || cell === 'grass' || cell === 'grass2';
    }

    playMove(direction) {
        if (!this.isPlayMode || this.gameOver) return;

        if (direction === 'left' || direction === 'right') {
            this.playerFacing = direction;
            this.movePlayer(direction === 'left' ? -1 : 1);
        } else if (direction === 'up') {
            this.climbUp();
        } else if (direction === 'down') {
            this.pickOrPlace();
        }

        // Move enemies after player moves
        this.moveEnemies();

        // Check for collision with enemies
        this.checkEnemyCollision();

        this.checkWin();
        this.renderPlayState();
        this.updatePlayStatus();
        this.scrollToPlayer();
    }

    moveEnemies() {
        for (const enemy of this.enemies) {
            // Calculate distance to player
            const dx = this.playerPos.x - enemy.x;
            const dy = this.playerPos.y - enemy.y;
            const distance = Math.abs(dx) + Math.abs(dy);

            // Only move if within 10 blocks
            if (distance > 10) continue;

            // Determine direction to move
            const moveDir = dx > 0 ? 1 : dx < 0 ? -1 : 0;
            if (moveDir === 0) continue;

            enemy.facing = moveDir > 0 ? 'right' : 'left';

            // Try to move horizontally
            const newX = enemy.x + moveDir;

            // Check bounds
            if (newX < 0 || newX >= this.width) continue;

            // Check collision with solid terrain
            if (this.isSolid(newX, enemy.y)) {
                // Try to climb up
                const climbY = enemy.y - 1;
                if (!this.isSolid(newX, climbY) && !this.blocks.has(`${newX},${climbY}`) && !this.isEnemyAt(newX, climbY)) {
                    enemy.x = newX;
                    enemy.y = climbY;
                }
                continue;
            }

            // Check collision with block
            if (this.blocks.has(`${newX},${enemy.y}`)) {
                // Try to climb up
                const climbY = enemy.y - 1;
                if (!this.isSolid(newX, climbY) && !this.blocks.has(`${newX},${climbY}`) && !this.isEnemyAt(newX, climbY)) {
                    enemy.x = newX;
                    enemy.y = climbY;
                }
                continue;
            }

            // Check collision with other enemies
            if (this.isEnemyAt(newX, enemy.y)) continue;

            // Move and apply gravity
            enemy.x = newX;
            this.applyEnemyGravity(enemy);
        }
    }

    isEnemyAt(x, y) {
        return this.enemies.some(e => e.x === x && e.y === y);
    }

    applyEnemyGravity(enemy) {
        while (true) {
            const belowY = enemy.y + 1;
            if (belowY >= this.height) break;
            if (this.isSolid(enemy.x, belowY)) break;
            if (this.blocks.has(`${enemy.x},${belowY}`)) break;
            enemy.y = belowY;
        }
    }

    checkEnemyCollision() {
        for (const enemy of this.enemies) {
            if (enemy.x === this.playerPos.x && enemy.y === this.playerPos.y) {
                this.gameOver = true;
                this.showMessage('Game Over! Caught by a slug!', 'error');
                return;
            }
        }
    }

    movePlayer(dx) {
        const newX = this.playerPos.x + dx;
        const newY = this.playerPos.y;

        // Check bounds
        if (newX < 0 || newX >= this.width) return;

        // Check collision with solid terrain
        if (this.isSolid(newX, newY)) return;

        // Check collision with block (can't push blocks in Block Dude)
        if (this.blocks.has(`${newX},${newY}`)) return;

        // If holding a block, check if there's space above target position
        if (this.holdingBlock) {
            const aboveNewY = newY - 1;
            if (this.isSolid(newX, aboveNewY)) return;
            if (this.blocks.has(`${newX},${aboveNewY}`)) return;
        }

        // Move to new position and apply gravity
        this.playerPos.x = newX;
        this.applyPlayerGravity();
        this.moves++;
    }

    climbUp() {
        const dx = this.playerFacing === 'left' ? -1 : 1;
        const frontX = this.playerPos.x + dx;
        const climbX = frontX;
        const climbY = this.playerPos.y - 1;

        // Check if there's something to climb
        const hasFrontObstacle = this.isSolid(frontX, this.playerPos.y) || this.blocks.has(`${frontX},${this.playerPos.y}`);
        if (!hasFrontObstacle) return;

        // Check if climb position is clear
        if (this.isSolid(climbX, climbY)) return;
        if (this.blocks.has(`${climbX},${climbY}`)) return;

        // If holding block, check extra space above
        if (this.holdingBlock) {
            const aboveClimbY = climbY - 1;
            const aboveCurrentY = this.playerPos.y - 1;
            // Check above climb position
            if (this.isSolid(climbX, aboveClimbY)) return;
            if (this.blocks.has(`${climbX},${aboveClimbY}`)) return;
            // Check above current position
            if (this.isSolid(this.playerPos.x, aboveCurrentY)) return;
            if (this.blocks.has(`${this.playerPos.x},${aboveCurrentY}`)) return;
        }

        this.playerPos.x = climbX;
        this.playerPos.y = climbY;
        this.moves++;
    }

    pickOrPlace() {
        if (this.holdingBlock) {
            this.placeBlock();
        } else {
            this.pickUpBlock();
        }
    }

    pickUpBlock() {
        const dx = this.playerFacing === 'left' ? -1 : 1;
        const blockX = this.playerPos.x + dx;
        const blockY = this.playerPos.y;

        if (!this.blocks.has(`${blockX},${blockY}`)) return;

        // Check if space above block is clear
        if (this.isSolid(blockX, blockY - 1)) return;
        if (this.blocks.has(`${blockX},${blockY - 1}`)) return;

        // Check if space above player is clear
        if (this.isSolid(this.playerPos.x, this.playerPos.y - 1)) return;
        if (this.blocks.has(`${this.playerPos.x},${this.playerPos.y - 1}`)) return;

        this.blocks.delete(`${blockX},${blockY}`);
        this.holdingBlock = true;
        this.moves++;
    }

    placeBlock() {
        const dx = this.playerFacing === 'left' ? -1 : 1;
        const frontX = this.playerPos.x + dx;
        const frontY = this.playerPos.y;

        // Check bounds
        if (frontX < 0 || frontX >= this.width) return;

        // If front position is blocked, try to place above it (on top of obstacle)
        if (this.isSolid(frontX, frontY) || this.blocks.has(`${frontX},${frontY}`)) {
            const aboveY = frontY - 1;
            // Check if space above obstacle is clear
            if (aboveY < 0) return;
            if (this.isSolid(frontX, aboveY)) return;
            if (this.blocks.has(`${frontX},${aboveY}`)) return;

            this.blocks.add(`${frontX},${aboveY}`);
            this.holdingBlock = false;
            this.moves++;
            return;
        }

        // Place in front and apply gravity
        this.blocks.add(`${frontX},${frontY}`);
        this.applyBlockGravity(frontX, frontY);
        this.holdingBlock = false;
        this.moves++;
    }

    applyPlayerGravity() {
        while (true) {
            const belowY = this.playerPos.y + 1;
            if (belowY >= this.height) break;
            if (this.isSolid(this.playerPos.x, belowY)) break;
            if (this.blocks.has(`${this.playerPos.x},${belowY}`)) break;
            this.playerPos.y = belowY;
        }
    }

    applyBlockGravity(x, y) {
        this.blocks.delete(`${x},${y}`);
        while (true) {
            const belowY = y + 1;
            if (belowY >= this.height) break;
            if (this.isSolid(x, belowY)) break;
            if (this.blocks.has(`${x},${belowY}`)) break;
            y = belowY;
        }
        this.blocks.add(`${x},${y}`);
    }

    checkWin() {
        for (let y = 0; y < this.height; y++) {
            for (let x = 0; x < this.width; x++) {
                if (this.grid[y][x] === 'door') {
                    if (this.playerPos.x === x && this.playerPos.y === y) {
                        this.showMessage('Level Complete! Moves: ' + this.moves, 'success');
                        setTimeout(() => this.exitPlayMode(), 2000);
                    }
                }
            }
        }
    }

    renderPlayState() {
        // Clear all cells
        document.querySelectorAll('.cell').forEach(cell => {
            cell.className = 'cell';
        });

        // Render terrain, walls and door from grid
        for (let y = 0; y < this.height; y++) {
            for (let x = 0; x < this.width; x++) {
                const cellType = this.grid[y][x];
                if (cellType === 'wall' || cellType === 'door' || cellType === 'grass' || cellType === 'grass2') {
                    const cell = document.querySelector(`.cell[data-x="${x}"][data-y="${y}"]`);
                    cell.classList.add(cellType);
                }
            }
        }

        // Render blocks
        this.blocks.forEach(key => {
            const [x, y] = key.split(',').map(Number);
            const cell = document.querySelector(`.cell[data-x="${x}"][data-y="${y}"]`);
            if (cell) cell.classList.add('block');
        });

        // Render enemies
        for (const enemy of this.enemies) {
            const enemyCell = document.querySelector(`.cell[data-x="${enemy.x}"][data-y="${enemy.y}"]`);
            if (enemyCell) {
                enemyCell.classList.add('enemy');
            }
        }

        // Render player
        const playerCell = document.querySelector(`.cell[data-x="${this.playerPos.x}"][data-y="${this.playerPos.y}"]`);
        if (playerCell) {
            playerCell.classList.add('player');
            playerCell.classList.add('facing-' + this.playerFacing);
            if (this.holdingBlock) {
                playerCell.classList.add('holding');
            }
        }
    }

    updatePlayStatus() {
        document.getElementById('playStatus').textContent = 'Moves: ' + this.moves;
    }

    // Save/Load
    async save() {
        if (!this.validate()) return;

        const levelNum = parseInt(document.getElementById('levelNumber').value) || 1;
        const levelName = document.getElementById('levelName').value || 'Unnamed Level';

        // Check if level already exists
        try {
            const checkResponse = await fetch(`/api/levels/${levelNum}`);
            if (checkResponse.ok) {
                // Level exists, ask for confirmation
                const confirmed = confirm(`Level ${levelNum} already exists. Do you want to overwrite it?`);
                if (!confirmed) {
                    this.showMessage('Save cancelled', 'info');
                    return;
                }
            }
        } catch (e) {
            // Level doesn't exist, continue with save
        }

        const gridStrings = [];
        const saveCharMap = {
            'grass': 'G',
            'grass2': 'g',
            'wall': '#',
            'block': 'B',
            'player': 'P',
            'door': 'D',
            'enemy': 'E',
            'empty': ' '
        };
        for (let y = 0; y < this.height; y++) {
            let row = '';
            for (let x = 0; x < this.width; x++) {
                const cell = this.grid[y][x];
                row += saveCharMap[cell] || ' ';
            }
            gridStrings.push(row);
        }

        const levelData = {
            id: levelNum,
            name: levelName,
            width: this.width,
            height: this.height,
            grid: gridStrings
        };

        try {
            const response = await fetch('/api/levels', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(levelData)
            });

            if (response.ok) {
                this.showMessage('Level saved as level_' + String(levelNum).padStart(2, '0') + '.json', 'success');
                this.loadLevelsList();
            } else {
                throw new Error('Save failed');
            }
        } catch (e) {
            this.showMessage('Failed to save: ' + e.message, 'error');
        }
    }

    async loadLevelsList() {
        try {
            const response = await fetch('/api/levels');
            const levels = await response.json();

            const listEl = document.getElementById('levelsListItems');
            listEl.innerHTML = '';

            levels.forEach(level => {
                const li = document.createElement('li');
                li.textContent = `#${level.id}: ${level.name} (${level.width}x${level.height})`;
                li.addEventListener('click', () => this.loadLevel(level.id));
                listEl.appendChild(li);
            });
        } catch (e) {
            console.error('Failed to load levels list:', e);
        }
    }

    async loadLevel(levelId) {
        try {
            const response = await fetch(`/api/levels/${levelId}`);
            if (!response.ok) throw new Error('Level not found');

            const levelData = await response.json();
            this.applyLevelData(levelData);
            this.hideModal();
            this.showMessage('Loaded level ' + levelId, 'success');
        } catch (e) {
            this.showMessage('Failed to load level: ' + e.message, 'error');
        }
    }

    applyLevelData(data) {
        this.width = data.width || data.grid[0].length;
        this.height = data.height || data.grid.length;

        document.getElementById('widthInput').value = this.width;
        document.getElementById('heightInput').value = this.height;
        document.getElementById('levelNumber').value = data.id || 1;
        document.getElementById('levelName').value = data.name || '';

        // Recreate grid
        const gridEl = document.getElementById('grid');
        gridEl.innerHTML = '';
        gridEl.style.gridTemplateColumns = `repeat(${this.width}, 24px)`;
        gridEl.style.width = `${this.width * 24}px`;

        this.grid = [];
        const loadCharMap = {
            'G': 'grass',
            'g': 'grass2',
            '#': 'wall',
            'B': 'block',
            'P': 'player',
            'D': 'door',
            'E': 'enemy',
            ' ': 'empty'
        };

        for (let y = 0; y < this.height; y++) {
            const row = [];
            const rowStr = data.grid[y] || '';
            for (let x = 0; x < this.width; x++) {
                const char = rowStr[x] || ' ';
                const cell = document.createElement('div');
                cell.className = 'cell';
                cell.dataset.x = x;
                cell.dataset.y = y;

                const type = loadCharMap[char] || 'empty';
                row.push(type);
                if (type !== 'empty') {
                    cell.classList.add(type);
                }

                gridEl.appendChild(cell);
            }
            this.grid.push(row);
        }

        this.updateScrollInfo();
    }

    showLoadModal() {
        this.loadLevelsList();
        document.getElementById('loadModal').style.display = 'flex';

        // Also populate modal list
        fetch('/api/levels')
            .then(r => r.json())
            .then(levels => {
                const listEl = document.getElementById('modalLevelsList');
                listEl.innerHTML = '';
                levels.forEach(level => {
                    const li = document.createElement('li');
                    li.textContent = `Level ${level.id}: ${level.name} (${level.width}x${level.height})`;
                    li.addEventListener('click', () => this.loadLevel(level.id));
                    listEl.appendChild(li);
                });
            });
    }

    hideModal() {
        document.getElementById('loadModal').style.display = 'none';
    }

    exportLevel() {
        const levelNum = parseInt(document.getElementById('levelNumber').value) || 1;
        const levelName = document.getElementById('levelName').value || 'Unnamed Level';

        const gridStrings = [];
        const exportCharMap = {
            'grass': 'G',
            'grass2': 'g',
            'wall': '#',
            'block': 'B',
            'player': 'P',
            'door': 'D',
            'enemy': 'E',
            'empty': ' '
        };
        for (let y = 0; y < this.height; y++) {
            let row = '';
            for (let x = 0; x < this.width; x++) {
                const cell = this.grid[y][x];
                row += exportCharMap[cell] || ' ';
            }
            gridStrings.push(row);
        }

        const levelData = {
            id: levelNum,
            name: levelName,
            width: this.width,
            height: this.height,
            grid: gridStrings
        };

        const blob = new Blob([JSON.stringify(levelData, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `level_${String(levelNum).padStart(2, '0')}.json`;
        a.click();
        URL.revokeObjectURL(url);

        this.showMessage('Level exported', 'success');
    }

    showMessage(text, type) {
        const msgEl = document.getElementById('messages');
        msgEl.textContent = text;
        msgEl.className = 'messages ' + type;

        if (type !== 'info') {
            setTimeout(() => {
                msgEl.className = 'messages';
                msgEl.textContent = '';
            }, 5000);
        }
    }
}

// Initialize editor
const editor = new LevelEditor();
