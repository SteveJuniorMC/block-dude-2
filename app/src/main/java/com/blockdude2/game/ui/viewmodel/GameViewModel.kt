package com.blockdude2.game.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockdude2.game.audio.SoundManager
import com.blockdude2.game.data.GameState
import com.blockdude2.game.data.Level
import com.blockdude2.game.data.LevelRepository
import com.blockdude2.game.data.ProgressDataStore
import com.blockdude2.game.game.GameEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val progressDataStore = ProgressDataStore(application)
    private val soundManager = SoundManager(application)

    private var _levels: List<Level> = emptyList()
    val levels: List<Level> get() = _levels

    private val _completedLevels = MutableStateFlow<Set<Int>>(emptySet())
    val completedLevels: StateFlow<Set<Int>> = _completedLevels.asStateFlow()

    private val _currentLevel = MutableStateFlow<Level?>(null)
    val currentLevel: StateFlow<Level?> = _currentLevel.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private var gameEngine: GameEngine? = null
    private var enemyTickJob: Job? = null

    init {
        _levels = LevelRepository.getAllLevels(application)

        viewModelScope.launch {
            progressDataStore.completedLevels.collect { completed ->
                _completedLevels.value = completed
            }
        }
    }

    fun refreshLevels() {
        _levels = LevelRepository.getAllLevels(getApplication())
    }

    fun getGameEngine(): GameEngine? = gameEngine

    fun startLevel(levelId: Int) {
        val level = levels.find { it.id == levelId } ?: return
        _currentLevel.value = level
        gameEngine = GameEngine(level)
        _gameState.value = gameEngine?.createInitialState()
        startEnemyTick()
    }

    private fun startEnemyTick() {
        enemyTickJob?.cancel()
        enemyTickJob = viewModelScope.launch {
            while (true) {
                delay(600) // Enemies move every 600ms
                val state = _gameState.value ?: continue
                val engine = gameEngine ?: continue
                if (state.levelCompleted || state.gameOver) break
                val newState = engine.tickEnemies(state)
                if (newState != state) {
                    _gameState.value = newState
                    if (newState.gameOver) {
                        break
                    }
                }
            }
        }
    }

    private fun stopEnemyTick() {
        enemyTickJob?.cancel()
        enemyTickJob = null
    }

    fun startNextAvailableLevel() {
        val nextLevelId = (_completedLevels.value.maxOrNull() ?: 0) + 1
        val levelId = if (nextLevelId <= levels.size) nextLevelId else 1
        startLevel(levelId)
    }

    fun moveLeft() {
        val state = _gameState.value ?: return
        val engine = gameEngine ?: return
        val newState = engine.moveLeft(state)
        if (newState != state) {
            soundManager.playMoveHorizontal()
            _gameState.value = newState
            checkLevelComplete(newState)
        }
    }

    fun moveRight() {
        val state = _gameState.value ?: return
        val engine = gameEngine ?: return
        val newState = engine.moveRight(state)
        if (newState != state) {
            soundManager.playMoveHorizontal()
            _gameState.value = newState
            checkLevelComplete(newState)
        }
    }

    fun moveUp() {
        val state = _gameState.value ?: return
        val engine = gameEngine ?: return
        val newState = engine.moveUp(state)
        if (newState != state) {
            soundManager.playMoveUp()
            _gameState.value = newState
            checkLevelComplete(newState)
        }
    }

    fun pickUpOrPlace() {
        val state = _gameState.value ?: return
        val engine = gameEngine ?: return
        val wasHolding = state.holdingBlock
        val newState = engine.pickUpOrPlace(state)
        if (newState != state) {
            if (wasHolding) {
                soundManager.playPlace()
            } else {
                soundManager.playPickUp()
            }
            _gameState.value = newState
        }
    }

    fun restartLevel() {
        _gameState.value = gameEngine?.createInitialState()
        startEnemyTick()
    }

    fun goToNextLevel() {
        val currentLevelId = _currentLevel.value?.id ?: return
        val nextLevelId = currentLevelId + 1
        if (nextLevelId <= levels.size) {
            startLevel(nextLevelId)
        }
    }

    fun hasNextLevel(): Boolean {
        val currentLevelId = _currentLevel.value?.id ?: return false
        return currentLevelId < levels.size
    }

    private fun checkLevelComplete(state: GameState) {
        if (state.levelCompleted) {
            val levelId = _currentLevel.value?.id ?: return
            soundManager.playLevelComplete()
            viewModelScope.launch {
                progressDataStore.markLevelCompleted(levelId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopEnemyTick()
        soundManager.release()
    }
}
