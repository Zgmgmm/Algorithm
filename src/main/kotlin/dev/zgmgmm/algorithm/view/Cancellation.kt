package dev.zgmgmm.algorithm.view

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.paint.Color
import tornadofx.*
import java.io.File
import java.lang.Math.abs
import java.lang.Thread.sleep
import java.util.*

val BLOCK_SIZE = 50.0
val ROW = 6
val COL = 6
val TYPES = 4
val MIN_CANCELLATION = 3
val COLORS = arrayOf(Color.BLUE, Color.GREEN, Color.RED, Color.BLACK, Color.YELLOW, Color.AZURE, Color.PURPLE, Color.BISQUE)
val HIGHLIGHT_COLOR = Color.YELLOW
val SELECTED_COLOR = Color.RED
val SELECTED_STYLE = BorderStrokeStyle.DASHED
val EMPTY = -1
val DEBUG = false
val IMGS = File("src/res/png").listFiles().map {
    Image(it.toURI().toURL().toString())
}
val MARGIN = 5
var DOREPAINT = true
val stack = Stack<Cancellation.Exchange>()
var bestOps = listOf<Cancellation.Exchange>()
var score = 0
var highest = 0

class Cancellation : View("My View") {

    lateinit var canvas: Canvas
    lateinit var ctx: GraphicsContext
    lateinit var BLOCKS: Array<Array<Block>>
    var hightLightedBlock: Block? = null
    var selectedBlock: Block? = null

    data class Block(
            var row: Int,
            var col: Int,
            var type: Int = 0
    ) {
        val x: Double
            get() {
                return col.toDouble() * BLOCK_SIZE
            }
        val y: Double
            get() {
                return row.toDouble() * BLOCK_SIZE
            }

        fun copy(): Block {
            return Block(row, col, type)
        }
    }

    override val root = vbox {
        canvas = canvas {
            height = BLOCK_SIZE * ROW
            width = BLOCK_SIZE * COL
            ctx = graphicsContext2D

        }

        runAsync {
            initBlocks()

            canvas.run {

                setOnMouseMoved {
                    //                println("${it.sceneX} ${it.sceneY}")
                    val x = it.sceneX
                    val y = it.sceneY
                    val row = (y / BLOCK_SIZE).toInt()
                    val col = (x / BLOCK_SIZE).toInt()
                    hightLightedBlock = BLOCKS[row][col]
//                println("$row $col")
                    repaint()
                }
                setOnMouseClicked {
                    if (selectedBlock == hightLightedBlock)
                        selectedBlock = null
                    else if (selectedBlock == null || !isNeighbor(selectedBlock!!, hightLightedBlock!!)) {
                        if (hightLightedBlock!!.type != -1)
                            selectedBlock = hightLightedBlock
                    } else {
                        val eliminated = trySwitch(selectedBlock!!, hightLightedBlock!!, false)
                        if (eliminated.isNotEmpty()) {
                            switchType(selectedBlock!!, hightLightedBlock!!)
                            eliminate(eliminated)
                        }
                        selectedBlock = null
                        hightLightedBlock=null
                    }
                    repaint()
                }
            }
            print(BLOCKS)
            repaint()
            AI()
//            println()

        }

    }

    fun AI() {
        DOREPAINT = false
        algorithm(BLOCKS, 1)
        DOREPAINT = true
        println("highest score is $highest  times $times")
        bestOps.forEach {
            with(it) {
                val eliminated = trySwitch(BLOCKS[r1][c1],BLOCKS[r2][c2])
                switchType(BLOCKS[r1][c1],BLOCKS[r2][c2])
                sleep(500)
                eliminate(eliminated)
            }
        }
    }

    private fun print(blocks: Array<Array<Block>>) {
        if (!DEBUG)
            return
        for (i in 0 until ROW) {
            for (j in 0 until COL) {
                print(" " + typeAt(i, j))
                //drawBlock(blocks[i][j])
            }
            println()
        }
    }

    private fun typeAt(row: Int, col: Int): Int {
        if (row < 0 || row >= ROW)
            return -2 //out of bounds
        if (col < 0 || col >= COL)
            return -2 //out of bounds
        val block = BLOCKS[row][col] ?: return -1
        return block.type
    }


    private fun eliminate(eliminated: List<Block>) {
        val ordered = eliminated.sortedBy {
            it.row
        }
        ordered.forEach {
            it.type = EMPTY
            val col = it.col
            for (row in it.row downTo 1) {
                BLOCKS[row][col].type = BLOCKS[row - 1][col].type
            }
            BLOCKS[0][col].type = EMPTY
        }
        repaint()
    }


    private fun switchType(a: Block, b: Block) {
        val t = a.type
        a.type = b.type
        b.type = t
        repaint()
    }


    private fun trySwitch(a: Block, b: Block, doRepaint: Boolean = false): List<Block> {
        val t = DOREPAINT
        DOREPAINT = doRepaint
        val eliminated = ArrayList<Block>()
        if (a.type == b.type)
            return eliminated
        if (a.type == EMPTY || b.type == EMPTY)
            return eliminated
        switchType(a, b)
        var success = false
        eliminated.run {
            addAll(test(b.row, b.col))
            addAll(test(a.row, a.col))
        }
        switchType(a, b)
        DOREPAINT = t
        return eliminated
    }

    private fun copyOf(blocks: Array<Array<Block>>): Array<Array<Block>> {
        val clone = Array(ROW) { i -> Array<Block>(COL) { j -> Block(i, j, -1) } }
        for (i in 0 until ROW)
            for (j in 0 until COL)
                clone[i][j] = blocks[i][j].copy()
        return clone
    }

    private fun initBlocks() {
        BLOCKS = Array(ROW) { i -> Array<Block>(COL) { j -> Block(i, j, -1) } }
        for (i in 0 until ROW)
            for (j in 0 until COL) {
                var ok = false
                while (!ok) {
                    BLOCKS[i][j].type = (Math.random() * TYPES).toInt()
                    ok = true
                    val res = test(i, j)
                    ok = (res.size == 0)
                }
            }
    }

    private fun isNeighbor(a: Block, b: Block): Boolean {
        return (abs(a.row - b.row) + abs(a.col - b.col) == 1)
    }

    private fun test(row: Int, col: Int): ArrayList<Block> {
        val same = arrayListOf<Block>()
        var vSame = arrayListOf<Block>()
        var hSame = arrayListOf<Block>()
        val type = BLOCKS[row][col].type ?: -1
        if (typeAt(row - 1, col) == type) {
            vSame.add(BLOCKS[row - 1][col])
            if (typeAt(row - 2, col) == type)
                vSame.add(BLOCKS[row - 2][col])
        }
        if (typeAt(row + 1, col) == type) {
            vSame.add(BLOCKS[row + 1][col])
            if (typeAt(row + 2, col) == type)
                vSame.add(BLOCKS[row + 2][col])
        }
        if (typeAt(row, col - 1) == type) {
            hSame.add(BLOCKS[row][col - 1])
            if (typeAt(row, col - 2) == type)
                hSame.add(BLOCKS[row][col - 2])
        }
        if (typeAt(row, col + 1) == type) {
            hSame.add(BLOCKS[row][col + 1])
            if (typeAt(row, col + 2) == type)
                hSame.add(BLOCKS[row][col + 2])
        }
        if (vSame.size >= MIN_CANCELLATION - 1 || hSame.size >= MIN_CANCELLATION - 1) {
            same.add(BLOCKS[row][col])
            same.addAll(hSame)
            same.addAll(vSame)
        }
        return same
    }

    private fun shadow() {
        ctx.save()
        ctx.fill = Color.rgb(0, 0, 0, 0.1)
        ctx.fillRect(0.0, 0.0, canvas.width, canvas.height)
        ctx.restore()
    }

    private fun repaint() {
        if (!DOREPAINT)
            return
        ctx.fill = Color.WHITE
        ctx.fillRect(0.0, 0.0, canvas.width, canvas.height)
        for (i in 0 until ROW)
            for (j in 0 until COL)
                drawBlock(BLOCKS[i][j])

        selectBlock(selectedBlock)
        highlightBlock(hightLightedBlock)
    }

    private fun selectBlock(block: Block?) {
        if (block == null || block.type == EMPTY) return
        val type = block.type
        val x = block.x
        val y = block.y
        ctx.save()
        ctx.drawImage(IMGS[type], x - MARGIN, y - MARGIN, BLOCK_SIZE + MARGIN * 2, BLOCK_SIZE + MARGIN * 2)
        ctx.restore()
    }

    private fun highlightBlock(block: Block?) {
        if (block == null || block.type == EMPTY) return
        val type = block.type
        val x = block.x
        val y = block.y
        ctx.save()
        ctx.drawImage(IMGS[type], x - MARGIN, y - MARGIN, BLOCK_SIZE + MARGIN * 2, BLOCK_SIZE + MARGIN * 2)
        ctx.restore()
//        if (block == null) return
//        val type = block.type
//        val x = block.x
//        val y = block.y
//
//        val color= Color.rgb(0,0,0,0.5)
//        drawBlock(block)
//        ctx.lineWidth = 10.0
//        ctx.save()
//        ctx.stroke = color
//        ctx.strokeRect(x, y, BLOCK_SIZE, BLOCK_SIZE)
//        ctx.restore()
    }

    private fun drawBlock(block: Block?) {
        if (block == null || block.type == EMPTY) return
        val type = block.type
        val x = block.x
        val y = block.y
        ctx.save()
        ctx.drawImage(IMGS[type], x + MARGIN, y + MARGIN, BLOCK_SIZE - MARGIN * 2, BLOCK_SIZE - MARGIN * 2)
        ctx.restore()
    }


    private fun algorithm(state: Array<Array<Block>>, step: Int) {
        val savedState = copyOf(state)
        BLOCKS = state
//        println("step $step")
        print(state)
        repaint()

        val candidate = arrayListOf<Exchange>()
        var eliminated: List<Block>? = null

        for (i in 0 until ROW)
            for (j in 0 until COL - 1) {
                eliminated = trySwitch(BLOCKS[i][j], BLOCKS[i][j + 1])
                if (eliminated.isNotEmpty())
                    candidate.add(Exchange(BLOCKS[i][j].row, BLOCKS[i][j].col, BLOCKS[i][j + 1].row, BLOCKS[i][j + 1].col))
            }
        for (i in 0 until ROW - 1)
            for (j in 0 until COL) {
                eliminated = trySwitch(BLOCKS[i][j], BLOCKS[i + 1][j])
                if (eliminated.isNotEmpty())
                    candidate.add(Exchange(BLOCKS[i][j].row, BLOCKS[i][j].col, BLOCKS[i + 1][j].row, BLOCKS[i + 1][j].col))
            }

//        println(candidate.size)
        var ord = 1
        candidate.forEach { (r1, c1, r2, c2) ->
            times += 1

//            println("$r1,$c1 <-> $r2,$c2")
            eliminated = trySwitch(BLOCKS[r1][c1], BLOCKS[r2][c2])
            switchType(BLOCKS[r1][c1], BLOCKS[r2][c2])
            eliminate(eliminated!!)
//            println("step $step try ${ord++}")
            print(BLOCKS)
            stack.push(Exchange(r1, c1, r2, c2))
            score += eliminated!!.size
            algorithm(BLOCKS, step + 1)
            if (highest < score) {
                highest = score
                bestOps = stack.toList()
            }
            stack.pop()
            score -= eliminated!!.size
            BLOCKS = copyOf(savedState)
        }
//        println("roll back")
    }

    var times = 0

    data class Exchange(
            val r1: Int,
            val c1: Int,
            val r2: Int,
            val c2: Int
    )
}
