package com.petsonscreen.app.engine

import com.petsonscreen.app.model.PetCommandAction
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.random.Random

const val TICK_MS: Long = 30L
const val FLING_THRESHOLD_PX_PER_SEC: Float = 600f

enum class MascotState {
    STAND,
    WALK,
    FALLING,
    BOUNCE,
    CLIMB,
    CLIMB_CEILING,
    DESCEND,
    JUMP,
    CREEP,
    SIT,
    SIT_WITH_LEGS_DOWN,
    SIT_AND_LOOK_UP,
    WINK,
    SIT_AND_DANGLE_LEGS,
    SPECIAL,
    SPECIAL2,
    TRIPPING,
    DRAGGING,
    FLINGING
}

data class SpriteStep(
    val frameIndex: Int,
    val dx: Int,
    val dy: Int,
    val durationTicks: Int
)

class MascotInstance(
    val instanceKey: String,
    val petId: Int,
    initialX: Float = 100f,
    initialY: Float = 0f,
    private val random: Random = Random.Default
) {
    var x: Float = initialX
    var y: Float = initialY
    var facingRight: Boolean = random.nextBoolean()
    var state: MascotState = MascotState.FALLING
        private set

    var currentFrameIndex: Int = 3
        private set

    private var stepIndex: Int = 0
    private var stepTick: Int = 0
    private var stateTicks: Int = 0
    private var stateMaxTicks: Int = 0

    // Velocity in px/s used during FLINGING
    private var vx: Float = 0f
    private var vy: Float = 0f

    private var activeSteps: List<SpriteStep> = STEPS_FALLING

    init {
        enterState(MascotState.FALLING)
    }

    fun startDrag() {
        vx = 0f
        vy = 0f
        enterState(MascotState.DRAGGING)
    }

    fun dragBy(dx: Float, dy: Float, playgroundWidth: Float, playgroundHeight: Float, spriteSize: Float) {
        if (state != MascotState.DRAGGING) {
            startDrag()
        }
        if (abs(dx) > 1.5f) {
            facingRight = dx > 0f
        }
        val bounds = computeBounds(playgroundWidth, playgroundHeight, spriteSize)
        x = (x + dx).coerceIn(bounds.minX, bounds.maxX)
        y = (y + dy).coerceIn(bounds.minY, bounds.maxY)
    }

    fun endDrag(velocityX: Float, velocityY: Float) {
        val speedMag = hypot(velocityX, velocityY)
        if (speedMag > FLING_THRESHOLD_PX_PER_SEC) {
            vx = velocityX.coerceIn(-4500f, 4500f)
            vy = velocityY.coerceIn(-4500f, 4500f)
            if (abs(vx) > 40f) {
                facingRight = vx > 0f
            }
            enterState(MascotState.FLINGING)
        } else {
            vx = 0f
            vy = 0f
            enterState(MascotState.FALLING)
        }
    }

    fun applyCommand(action: PetCommandAction, playgroundWidth: Float, playgroundHeight: Float, spriteSize: Float) {
        if (state == MascotState.DRAGGING) return
        val bounds = computeBounds(playgroundWidth, playgroundHeight, spriteSize)
        when (action) {
            PetCommandAction.WALK -> {
                if (y < bounds.maxY - 4f) {
                    y = bounds.maxY
                }
                facingRight = random.nextBoolean()
                enterState(MascotState.WALK)
            }
            PetCommandAction.JUMP -> {
                facingRight = if (x <= bounds.minX + 8f) true else if (x >= bounds.maxX - 8f) false else random.nextBoolean()
                enterState(MascotState.JUMP)
            }
            PetCommandAction.SIT -> {
                if (y < bounds.maxY - 4f) {
                    y = bounds.maxY
                }
                enterState(MascotState.SIT)
            }
        }
    }

    fun tick(playgroundWidth: Float, playgroundHeight: Float, spriteSize: Float, speed: Float) {
        if (playgroundWidth <= 0f || playgroundHeight <= 0f || spriteSize <= 0f) return
        val bounds = computeBounds(playgroundWidth, playgroundHeight, spriteSize)
        val speedMultiplier = if (speed <= 0.1f) 1f else speed + 0.9f

        if (state == MascotState.DRAGGING) {
            advanceStepOnly()
            return
        }

        if (state == MascotState.FLINGING) {
            val dt = TICK_MS / 1000f
            val decay = 0.08f.pow(dt)
            vx *= decay
            vy *= decay
            vy += 900f * dt // gentle gravity pull during fling arc
            x += vx * dt
            y += vy * dt

            if (x <= bounds.minX) {
                x = bounds.minX
                facingRight = false
                enterState(MascotState.CLIMB)
                return
            }
            if (x >= bounds.maxX) {
                x = bounds.maxX
                facingRight = true
                enterState(MascotState.CLIMB)
                return
            }
            if (y <= bounds.minY) {
                y = bounds.minY
                enterState(MascotState.CLIMB_CEILING)
                return
            }
            if (y >= bounds.maxY) {
                y = bounds.maxY
                enterState(MascotState.BOUNCE)
                return
            }
            if (hypot(vx, vy) < 120f) {
                enterState(MascotState.FALLING)
                return
            }
            advanceStepOnly()
            return
        }

        val currentStep = activeSteps[stepIndex]
        val dirSign = if (facingRight) 1f else -1f
        val stepDx = currentStep.dx * dirSign * speedMultiplier
        val stepDy = currentStep.dy * speedMultiplier

        x += stepDx
        y += stepDy

        // Clamp and handle edge collisions per state
        when (state) {
            MascotState.STAND -> {
                y = bounds.maxY
                x = x.coerceIn(bounds.minX, bounds.maxX)
            }

            MascotState.WALK -> {
                y = bounds.maxY
                if (x <= bounds.minX) {
                    x = bounds.minX
                    if (random.nextBoolean()) {
                        facingRight = false
                        enterState(MascotState.CLIMB)
                    } else {
                        facingRight = true
                    }
                    return
                } else if (x >= bounds.maxX) {
                    x = bounds.maxX
                    if (random.nextBoolean()) {
                        facingRight = true
                        enterState(MascotState.CLIMB)
                    } else {
                        facingRight = false
                    }
                    return
                }
            }

            MascotState.FALLING -> {
                x = x.coerceIn(bounds.minX, bounds.maxX)
                if (y >= bounds.maxY) {
                    y = bounds.maxY
                    enterState(MascotState.BOUNCE)
                    return
                }
            }

            MascotState.BOUNCE -> {
                y = bounds.maxY
                x = x.coerceIn(bounds.minX, bounds.maxX)
            }

            MascotState.CLIMB -> {
                x = if (x <= (bounds.minX + bounds.maxX) * 0.5f) {
                    facingRight = false
                    bounds.minX
                } else {
                    facingRight = true
                    bounds.maxX
                }
                if (y <= bounds.minY) {
                    y = bounds.minY
                    enterState(MascotState.CLIMB_CEILING)
                    return
                }
            }

            MascotState.CLIMB_CEILING -> {
                y = bounds.minY
                if (x <= bounds.minX) {
                    x = bounds.minX
                    if (random.nextBoolean()) {
                        facingRight = false
                        enterState(MascotState.DESCEND)
                    } else {
                        facingRight = true
                    }
                    return
                } else if (x >= bounds.maxX) {
                    x = bounds.maxX
                    if (random.nextBoolean()) {
                        facingRight = true
                        enterState(MascotState.DESCEND)
                    } else {
                        facingRight = false
                    }
                    return
                }
            }

            MascotState.DESCEND -> {
                x = if (x <= (bounds.minX + bounds.maxX) * 0.5f) {
                    facingRight = false
                    bounds.minX
                } else {
                    facingRight = true
                    bounds.maxX
                }
                if (y >= bounds.maxY) {
                    y = bounds.maxY
                    if (random.nextFloat() < 0.35f) {
                        enterState(MascotState.CLIMB)
                    } else {
                        facingRight = x <= (bounds.minX + bounds.maxX) * 0.5f
                        enterState(MascotState.WALK)
                    }
                    return
                }
            }

            MascotState.JUMP -> {
                if (x <= bounds.minX) {
                    x = bounds.minX
                    facingRight = false
                    enterState(MascotState.CLIMB)
                    return
                } else if (x >= bounds.maxX) {
                    x = bounds.maxX
                    facingRight = true
                    enterState(MascotState.CLIMB)
                    return
                }
                if (y <= bounds.minY) {
                    y = bounds.minY
                    enterState(MascotState.CLIMB_CEILING)
                    return
                }
            }

            MascotState.CREEP -> {
                y = bounds.maxY
                if (x <= bounds.minX) {
                    x = bounds.minX
                    facingRight = false
                    enterState(MascotState.CLIMB)
                    return
                } else if (x >= bounds.maxX) {
                    x = bounds.maxX
                    facingRight = true
                    enterState(MascotState.CLIMB)
                    return
                }
            }

            MascotState.TRIPPING -> {
                y = bounds.maxY
                if (x <= bounds.minX) {
                    x = bounds.minX
                    facingRight = true
                } else if (x >= bounds.maxX) {
                    x = bounds.maxX
                    facingRight = false
                }
            }

            MascotState.SIT,
            MascotState.SIT_WITH_LEGS_DOWN,
            MascotState.SIT_AND_LOOK_UP,
            MascotState.WINK,
            MascotState.SIT_AND_DANGLE_LEGS,
            MascotState.SPECIAL,
            MascotState.SPECIAL2 -> {
                y = bounds.maxY
                x = x.coerceIn(bounds.minX, bounds.maxX)
            }

            MascotState.DRAGGING, MascotState.FLINGING -> Unit
        }

        // Advance step & state timers
        stepTick++
        stateTicks++

        // Check random timeouts for looping locomotion states
        if (state == MascotState.WALK && stateTicks >= stateMaxTicks) {
            enterState(pickRandomIdle())
            return
        }
        if (state == MascotState.CLIMB && stateTicks >= stateMaxTicks) {
            if (random.nextFloat() < 0.70f) {
                // Jump off the wall toward interior
                facingRight = x <= (bounds.minX + bounds.maxX) * 0.5f
                enterState(MascotState.JUMP)
            } else {
                enterState(MascotState.FALLING)
            }
            return
        }
        if (state == MascotState.CLIMB_CEILING && stateTicks >= stateMaxTicks) {
            enterState(MascotState.FALLING)
            return
        }
        if (state == MascotState.JUMP && stateTicks >= stateMaxTicks) {
            enterState(MascotState.FALLING)
            return
        }
        if (state == MascotState.CREEP && stateTicks >= stateMaxTicks) {
            enterState(if (random.nextBoolean()) MascotState.WALK else MascotState.STAND)
            return
        }

        if (stepTick >= currentStep.durationTicks) {
            stepTick = 0
            stepIndex++
            if (stepIndex >= activeSteps.size) {
                onSequenceCompleted()
                return
            }
            currentFrameIndex = activeSteps[stepIndex].frameIndex
        }
    }

    private fun advanceStepOnly() {
        val currentStep = activeSteps[stepIndex]
        stepTick++
        stateTicks++
        if (stepTick >= currentStep.durationTicks) {
            stepTick = 0
            stepIndex = (stepIndex + 1) % activeSteps.size
            currentFrameIndex = activeSteps[stepIndex].frameIndex
        }
    }

    private fun onSequenceCompleted() {
        when (state) {
            MascotState.STAND -> enterState(MascotState.WALK)
            MascotState.BOUNCE -> enterState(MascotState.WALK)
            MascotState.WALK,
            MascotState.FALLING,
            MascotState.CLIMB,
            MascotState.CLIMB_CEILING,
            MascotState.DESCEND,
            MascotState.JUMP,
            MascotState.CREEP,
            MascotState.DRAGGING,
            MascotState.FLINGING -> {
                stepIndex = 0
                currentFrameIndex = activeSteps[0].frameIndex
            }
            MascotState.SIT,
            MascotState.SIT_WITH_LEGS_DOWN,
            MascotState.SIT_AND_LOOK_UP,
            MascotState.WINK,
            MascotState.SIT_AND_DANGLE_LEGS,
            MascotState.SPECIAL,
            MascotState.SPECIAL2,
            MascotState.TRIPPING -> {
                enterState(if (random.nextFloat() < 0.7f) MascotState.WALK else MascotState.STAND)
            }
        }
    }

    private fun enterState(newState: MascotState) {
        state = newState
        stepIndex = 0
        stepTick = 0
        stateTicks = 0
        activeSteps = when (newState) {
            MascotState.STAND -> STEPS_STAND
            MascotState.WALK -> {
                stateMaxTicks = random.nextInt(10, 70)
                STEPS_WALK
            }
            MascotState.FALLING -> STEPS_FALLING
            MascotState.BOUNCE -> STEPS_BOUNCE
            MascotState.CLIMB -> {
                stateMaxTicks = random.nextInt(50, 350)
                STEPS_CLIMB
            }
            MascotState.CLIMB_CEILING -> {
                stateMaxTicks = random.nextInt(30, 200)
                STEPS_CLIMB_CEILING
            }
            MascotState.DESCEND -> STEPS_DESCEND
            MascotState.JUMP -> {
                stateMaxTicks = random.nextInt(24, 48)
                STEPS_JUMP
            }
            MascotState.CREEP -> {
                stateMaxTicks = random.nextInt(60, 140)
                STEPS_CREEP
            }
            MascotState.SIT -> STEPS_SIT
            MascotState.SIT_WITH_LEGS_DOWN -> STEPS_SIT_LEGS_DOWN
            MascotState.SIT_AND_LOOK_UP -> STEPS_SIT_LOOK_UP
            MascotState.WINK -> STEPS_WINK
            MascotState.SIT_AND_DANGLE_LEGS -> STEPS_SIT_DANGLE_LEGS
            MascotState.SPECIAL -> STEPS_SPECIAL
            MascotState.SPECIAL2 -> STEPS_SPECIAL2
            MascotState.TRIPPING -> STEPS_TRIPPING
            MascotState.DRAGGING -> STEPS_DRAGGING
            MascotState.FLINGING -> STEPS_FLINGING
        }
        currentFrameIndex = activeSteps[0].frameIndex
    }

    private fun pickRandomIdle(): MascotState {
        return IDLE_POOL[random.nextInt(IDLE_POOL.size)]
    }

    data class Bounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float)

    companion object {
        fun computeBounds(playgroundWidth: Float, playgroundHeight: Float, spriteSize: Float): Bounds {
            val overhang = spriteSize / 3f
            val minX = -overhang
            val maxX = (playgroundWidth - spriteSize + overhang).coerceAtLeast(minX)
            val minY = -overhang
            val maxY = (playgroundHeight - spriteSize).coerceAtLeast(minY)
            return Bounds(minX, maxX, minY, maxY)
        }

        private val IDLE_POOL = listOf(
            MascotState.SIT,
            MascotState.STAND,
            MascotState.WINK,
            MascotState.SIT_AND_DANGLE_LEGS,
            MascotState.SIT_WITH_LEGS_DOWN,
            MascotState.SIT_AND_LOOK_UP,
            MascotState.TRIPPING,
            MascotState.CREEP,
            MascotState.SPECIAL,
            MascotState.SPECIAL2
        )

        private val STEPS_STAND = listOf(SpriteStep(0, 0, 0, 150))

        private val STEPS_WALK = listOf(
            SpriteStep(0, 2, 0, 6),
            SpriteStep(1, 2, 0, 6),
            SpriteStep(0, 2, 0, 6),
            SpriteStep(2, 2, 0, 6)
        )

        private val STEPS_FALLING = listOf(SpriteStep(3, 0, 15, 250))

        private val STEPS_BOUNCE = listOf(
            SpriteStep(17, 0, 0, 8),
            SpriteStep(18, 0, 0, 8)
        )

        private val STEPS_CLIMB = listOf(
            SpriteStep(13, 0, 0, 16),
            SpriteStep(13, 0, -1, 4),
            SpriteStep(11, 0, -1, 4),
            SpriteStep(12, 0, -1, 4),
            SpriteStep(12, 0, 0, 16),
            SpriteStep(12, 0, -2, 4),
            SpriteStep(11, 0, -2, 4),
            SpriteStep(13, 0, -2, 4)
        )

        private val STEPS_CLIMB_CEILING = listOf(
            SpriteStep(24, 0, 0, 16),
            SpriteStep(24, 1, 0, 4),
            SpriteStep(22, 1, 0, 4),
            SpriteStep(23, 1, 0, 4),
            SpriteStep(23, 0, 0, 16),
            SpriteStep(23, 2, 0, 4),
            SpriteStep(22, 2, 0, 4),
            SpriteStep(24, 2, 0, 4)
        )

        private val STEPS_DESCEND = listOf(
            SpriteStep(13, 0, 0, 16),
            SpriteStep(13, 0, 2, 4),
            SpriteStep(11, 0, 2, 4),
            SpriteStep(12, 0, 2, 4),
            SpriteStep(12, 0, 0, 16),
            SpriteStep(12, 0, 1, 4),
            SpriteStep(11, 0, 1, 4),
            SpriteStep(13, 0, 1, 4)
        )

        private val STEPS_JUMP = listOf(SpriteStep(21, 10, -1, 2))

        private val STEPS_CREEP = listOf(
            SpriteStep(19, 0, 0, 28),
            SpriteStep(19, 2, 0, 4),
            SpriteStep(20, 2, 0, 4),
            SpriteStep(20, 1, 0, 4),
            SpriteStep(20, 0, 0, 24)
        )

        private val STEPS_SIT = listOf(SpriteStep(10, 0, 0, 250))
        private val STEPS_SIT_LEGS_DOWN = listOf(SpriteStep(30, 0, 0, 250))
        private val STEPS_SIT_LOOK_UP = listOf(SpriteStep(25, 0, 0, 250))

        private val STEPS_WINK = listOf(
            SpriteStep(14, 0, 0, 30),
            SpriteStep(16, 0, 0, 5),
            SpriteStep(14, 0, 0, 30)
        )

        private val STEPS_SIT_DANGLE_LEGS = buildList {
            repeat(5) {
                add(SpriteStep(30, 0, 0, 5))
                add(SpriteStep(31, 0, 0, 15))
            }
        }

        private val STEPS_SPECIAL = listOf(
            SpriteStep(0, 0, 0, 16),
            SpriteStep(37, 0, 0, 20),
            SpriteStep(38, 0, 0, 20),
            SpriteStep(39, 0, 0, 20),
            SpriteStep(40, 0, 0, 20)
        )

        private val STEPS_SPECIAL2 = listOf(41, 42, 43, 44, 45, 44, 43, 42, 41).map {
            SpriteStep(it, 0, 0, 14)
        }

        private val STEPS_TRIPPING = listOf(
            SpriteStep(18, 8, 0, 8),
            SpriteStep(17, 4, 0, 4),
            SpriteStep(19, 2, 0, 4),
            SpriteStep(19, 0, 0, 10),
            SpriteStep(18, 4, 0, 4)
        )

        private val STEPS_DRAGGING = listOf(
            SpriteStep(6, 0, 0, 8),
            SpriteStep(4, 0, 0, 8),
            SpriteStep(5, 0, 0, 8),
            SpriteStep(7, 0, 0, 8),
            SpriteStep(5, 0, 0, 8),
            SpriteStep(4, 0, 0, 8)
        )

        private val STEPS_FLINGING = listOf(SpriteStep(7, 0, 0, 250))
    }
}
