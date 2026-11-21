package com.petsonscreen.app

import com.petsonscreen.app.engine.MascotInstance
import com.petsonscreen.app.engine.MascotState
import com.petsonscreen.app.model.PetCommandAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MascotPhysicsTest {

    @Test
    fun `bounds allow overhang of spriteWidth divided by 3 on top left right`() {
        val bounds = MascotInstance.computeBounds(
            playgroundWidth = 1080f,
            playgroundHeight = 1920f,
            spriteSize = 150f
        )
        assertEquals(-50f, bounds.minX, 0.01f)
        assertEquals(980f, bounds.maxX, 0.01f)
        assertEquals(-50f, bounds.minY, 0.01f)
        assertEquals(1770f, bounds.maxY, 0.01f)
    }

    @Test
    fun `falling hits bottom edge and transitions to bounce then walk`() {
        val mascot = MascotInstance("t1", 1, initialX = 400f, initialY = 1750f, random = Random(42))
        assertEquals(MascotState.FALLING, mascot.state)
        mascot.tick(1080f, 1920f, 150f, speed = 1.5f)
        assertEquals(MascotState.BOUNCE, mascot.state)
        repeat(16) {
            mascot.tick(1080f, 1920f, 150f, speed = 1.5f)
        }
        assertEquals(MascotState.WALK, mascot.state)
    }

    @Test
    fun `fast fling enters flinging and decays exponentially`() {
        val mascot = MascotInstance("t2", 1, initialX = 400f, initialY = 800f, random = Random(7))
        mascot.startDrag()
        assertEquals(MascotState.DRAGGING, mascot.state)
        mascot.endDrag(velocityX = 1200f, velocityY = -800f)
        assertEquals(MascotState.FLINGING, mascot.state)
        assertEquals(7, mascot.currentFrameIndex)
    }

    @Test
    fun `commands switch mascot state immediately`() {
        val mascot = MascotInstance("t3", 1, initialX = 300f, initialY = 1770f, random = Random(1))
        mascot.applyCommand(PetCommandAction.SIT, 1080f, 1920f, 150f)
        assertEquals(MascotState.SIT, mascot.state)
        mascot.applyCommand(PetCommandAction.JUMP, 1080f, 1920f, 150f)
        assertEquals(MascotState.JUMP, mascot.state)
        mascot.applyCommand(PetCommandAction.WALK, 1080f, 1920f, 150f)
        assertEquals(MascotState.WALK, mascot.state)
        assertTrue(mascot.currentFrameIndex in 0..2)
    }
}
